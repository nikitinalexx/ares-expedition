package com.terraforming.ares.selfplay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.agent.Decision;
import com.terraforming.ares.services.agent.DecisionProbe;
import com.terraforming.ares.services.agent.DecisionType;
import com.terraforming.ares.services.agent.MlpPhaseModel;
import com.terraforming.ares.services.agent.NeuralPolicy;
import com.terraforming.ares.services.agent.SyntheticModelFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Measurement harness for search, before any search is built.
 * <p>
 * Answers with numbers, not estimates:
 * <ul>
 *   <li>what a decision point costs: copying the game, the policy forward pass, engine work between two decisions;</li>
 *   <li>whether a mid-game copy can be resumed: {@code processSimulation(copy)} must ask the same decision first
 *       (same type and same candidate ids), otherwise counterfactual rollouts from a snapshot are meaningless;</li>
 *   <li>how long a full rollout from a decision point takes, and a rollout truncated after N decisions;</li>
 *   <li>how the rollouts scale with 1/2/4/8 threads.</li>
 * </ul>
 * The live games are played by {@link PlayerDifficulty#AGENT} without recording, on one thread, so the timings are not
 * polluted by contention. Rollouts are then replayed from the captured snapshots at every requested scale.
 * <p>
 * Nothing here is used at deploy time: it is a measuring instrument, not a player.
 */
@Component
@RequiredArgsConstructor
public class RolloutBench {
    public record Options(Path out, int games, Set<DecisionType> types, int capturesPerTypePerGame, int maxCaptures,
                          int truncateDecisions, int rolloutsPerCapture, List<Integer> scales, long maxSeconds,
                          boolean hashPolicy, boolean syntheticModel) {
    }

    /** One decision of the current action attempt, replayed in order before the target decision. */
    private record TrailStep(DecisionType type, int id) {
    }

    public record Capture(DecisionType type, int round, int candidates, int chosenIndex, int chosenId, int[] ids,
                          float[] observation, String playerUuid, MarsGame turnStart, List<TrailStep> trail,
                          long copyNanos) {
    }

    private static final class Timings {
        long count;
        long gapSum;
        long policySum;
        final List<Long> gaps = new ArrayList<>();
        final List<Long> policies = new ArrayList<>();

        void add(long gap, long policy) {
            count++;
            gapSum += gap;
            policySum += policy;
            if (gaps.size() < 100_000) { // memory guard: percentiles on the first 100k decisions are enough
                gaps.add(gap);
                policies.add(policy);
            }
        }
    }

    /** Thrown from the hook to stop a truncated rollout; never leaves the rollout thread. */
    private static final class StopRollout extends RuntimeException {
        StopRollout() {
            super(null, null, false, false);
        }
    }

    private record RolloutOutcome(long nanos, int decisions, boolean forced, int decisionsBeforeForce,
                                  DecisionType firstType, boolean firstSameType, boolean firstSameIds,
                                  boolean trailBroken, boolean forceSameIds, boolean forceSameObservation,
                                  boolean completed, int score, String error) {
    }

    private final GameService gameService;
    private final SimulationProcessorService simulationProcessorService;
    private final WinPointsService winPointsService;
    private final com.terraforming.ares.services.ai.AiService aiService;
    private final NeuralPolicy neuralPolicy;

    public void run(Options options) throws Exception {
        Path parent = options.out().toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        List<Capture> captures = new ArrayList<>();
        Map<DecisionType, Timings> timings = new EnumMap<>(DecisionType.class);
        Map<DecisionType, Integer> liveCounts = new EnumMap<>(DecisionType.class);
        long[] obsStats = new long[2];
        Map<String, Object> modelShapes = new TreeMap<>();
        if (options.syntheticModel()) {
            // Real inference path, real shapes, meaningless weights: only the cost of the net is being measured.
            Path modelRoot = Path.of(options.out() + ".models");
            Map<DecisionType, Path> dirs = SyntheticModelFactory.writeAll(modelRoot, 256, 128, 12345L);
            for (DecisionType type : DecisionType.values()) {
                MlpPhaseModel model = MlpPhaseModel.load(dirs.get(type));
                neuralPolicy.use(model, 0.0);
                Map<String, Object> shape = new LinkedHashMap<>();
                shape.put("obsSize", model.obsSize());
                shape.put("candFeatures", model.candFeatures());
                modelShapes.put(type.name(), shape);
            }
            System.out.println("[BENCH] synthetic models (hidden 256,128) loaded from " + modelRoot);
        }
        long started = System.nanoTime();
        double gameSeconds = 0;

        for (int g = 0; g < options.games(); g++) {
            if (options.maxSeconds() > 0 && (System.nanoTime() - started) / 1e9 > options.maxSeconds()) {
                break;
            }
            MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                    .playerNames(new ArrayList<>(List.of("Seat1", "Seat2")))
                    .computers(List.of(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT))
                    .mulligan(true)
                    .expansion(Expansion.BASE).expansion(Expansion.BUFFED_CORPORATION).expansion(Expansion.DISCOVERY)
                    .dummyHand(true)
                    .build());
            LiveHook hook = new LiveHook(options, captures, timings, liveCounts, obsStats);
            DecisionProbe.Hook previous = DecisionProbe.set(hook);
            long t0 = System.nanoTime();
            try {
                simulationProcessorService.processSimulation(game);
            } finally {
                DecisionProbe.set(previous);
            }
            gameSeconds += (System.nanoTime() - t0) / 1e9;
            System.out.printf(Locale.ROOT, "[BENCH] game %d/%d: %.0f ms, %d decisions, %d captures kept%n",
                    g + 1, options.games(), (System.nanoTime() - t0) / 1e6, hook.decisions, captures.size());
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("games", options.games());
        report.put("gcSeconds", round(gcSeconds() / 1000.0, 2));
        report.put("gameSeconds", round(gameSeconds, 2));
        report.put("gameMsMean", round(1000 * gameSeconds / Math.max(1, options.games()), 1));
        report.put("captures", captures.size());
        Map<String, Object> countByType = new TreeMap<>();
        liveCounts.forEach((type, count) -> countByType.put(type.name(), count));
        report.put("decisionsPerType", countByType);
        report.put("modelShapes", modelShapes);
        Map<String, Object> cost = new TreeMap<>();
        timings.forEach((type, t) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("decisions", t.count);
            m.put("policyUsMean", round(t.policySum / 1000.0 / Math.max(1, t.count), 2));
            m.put("policyUsP50", round(percentile(t.policies, 0.5) / 1000.0, 2));
            m.put("policyUsP95", round(percentile(t.policies, 0.95) / 1000.0, 2));
            m.put("gapMsMean", round(t.gapSum / 1e6 / Math.max(1, t.count), 3));
            m.put("gapMsP50", round(percentile(t.gaps, 0.5) / 1e6, 3));
            m.put("gapMsP95", round(percentile(t.gaps, 0.95) / 1e6, 3));
            cost.put(type.name(), m);
        });
        report.put("costPerDecision", cost);
        long totalDecisions = timings.values().stream().mapToLong(t -> t.count).sum();
        long totalPolicy = timings.values().stream().mapToLong(t -> t.policySum).sum();
        report.put("policyUsOverallMean", round(totalPolicy / 1000.0 / Math.max(1, totalDecisions), 2));
        report.put("obsNonZeroFraction", round(obsStats[1] / (double) Math.max(1, obsStats[0]), 4));

        long[] copies = captures.stream().mapToLong(Capture::copyNanos).sorted().toArray();
        report.put("copyUsMean", round(mean(copies) / 1000.0, 2));
        report.put("copyUsP50", round(percentile(copies, 0.5) / 1000.0, 2));
        report.put("copyUsP95", round(percentile(copies, 0.95) / 1000.0, 2));

        List<Map<String, Object>> rolloutReports = new ArrayList<>();
        for (int scale : options.scales()) {
            rolloutReports.add(rolloutScale(options, captures, scale));
        }
        report.put("rollouts", rolloutReports);

        Map<String, Object> byType = new TreeMap<>();
        Map<DecisionType, List<Capture>> grouped = new EnumMap<>(DecisionType.class);
        captures.forEach(c -> grouped.computeIfAbsent(c.type(), t -> new ArrayList<>()).add(c));
        grouped.forEach((type, list) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("captures", list.size());
            m.put("candidatesMean", round(list.stream().mapToInt(Capture::candidates).average().orElse(0), 2));
            byType.put(type.name(), m);
        });
        report.put("capturesPerType", byType);

        ObjectMapper mapper = new ObjectMapper();
        Path tmp = Path.of(options.out() + ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), report);
        Files.move(tmp, options.out(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[BENCH] report: " + options.out());
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));
    }

    /**
     * Replays every capture {@code rolloutsPerCapture} times with the requested pool size and reports wall time,
     * per-rollout time, and — the important part — whether the resumption actually reached the same decision.
     */
    private Map<String, Object> rolloutScale(Options options, List<Capture> captures, int scale) throws Exception {
        int limit = options.truncateDecisions() > 0 ? options.truncateDecisions() : Integer.MAX_VALUE;
        List<Sample> samples = Collections.synchronizedList(new ArrayList<>());
        long wall;
        try (ExecutorService pool = Executors.newFixedThreadPool(scale)) {
            List<Future<?>> futures = new ArrayList<>();
            long t0 = System.nanoTime();
            for (Capture capture : captures) {
                for (int r = 0; r < options.rolloutsPerCapture(); r++) {
                    futures.add(pool.submit(() -> samples.add(new Sample(capture,
                            rollout(capture, limit, options.hashPolicy())))));
                }
            }
            for (Future<?> f : futures) {
                f.get();
            }
            wall = System.nanoTime() - t0;
        }
        List<RolloutOutcome> outcomes = samples.stream().map(Sample::outcome).toList();
        long[] nanos = outcomes.stream().filter(o -> o.error() == null).mapToLong(RolloutOutcome::nanos).sorted().toArray();
        long forced = outcomes.stream().filter(RolloutOutcome::forced).count();
        long sameType = outcomes.stream().filter(RolloutOutcome::firstSameType).count();
        long sameIds = outcomes.stream().filter(RolloutOutcome::firstSameIds).count();
        long forceSameIds = outcomes.stream().filter(RolloutOutcome::forceSameIds).count();
        long forceSameObservation = outcomes.stream().filter(RolloutOutcome::forceSameObservation).count();
        long trailBroken = outcomes.stream().filter(RolloutOutcome::trailBroken).count();
        long errors = outcomes.stream().filter(o -> o.error() != null).count();
        long decisions = outcomes.stream().mapToLong(RolloutOutcome::decisions).sum();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("threads", scale);
        m.put("rollouts", outcomes.size());
        m.put("wallSeconds", round(wall / 1e9, 3));
        m.put("rolloutsPerSecond", round(outcomes.size() / Math.max(1e-9, wall / 1e9), 1));
        m.put("rolloutMsMean", round(mean(nanos) / 1e6, 2));
        m.put("rolloutMsP50", round(percentile(nanos, 0.5) / 1e6, 2));
        m.put("rolloutMsP95", round(percentile(nanos, 0.95) / 1e6, 2));
        m.put("decisionsMean", round(decisions / (double) Math.max(1, outcomes.size()), 1));
        m.put("forcedRate", round(forced / (double) Math.max(1, outcomes.size()), 4));
        m.put("firstSameTypeRate", round(sameType / (double) Math.max(1, outcomes.size()), 4));
        m.put("firstSameIdsRate", round(sameIds / (double) Math.max(1, outcomes.size()), 4));
        m.put("forceSameIdsRate", round(forceSameIds / (double) Math.max(1, outcomes.size()), 4));
        m.put("forceSameObservationRate", round(forceSameObservation / (double) Math.max(1, outcomes.size()), 4));
        m.put("trailBrokenRate", round(trailBroken / (double) Math.max(1, outcomes.size()), 4));
        m.put("errors", errors);
        m.put("completedRate", round(outcomes.stream().filter(RolloutOutcome::completed).count()
                / (double) Math.max(1, outcomes.size()), 4));
        m.put("decisionsBeforeForceMean", round(outcomes.stream().filter(RolloutOutcome::forced)
                .mapToInt(RolloutOutcome::decisionsBeforeForce).average().orElse(0), 2));
        if (!outcomes.isEmpty()) {
            RolloutOutcome first = outcomes.getFirst();
            m.put("firstErrorExample", first.error());
            m.put("exampleFirstType", first.firstType() == null ? null : first.firstType().name());
        }
        m.put("scoreMean", round(outcomes.stream().filter(o -> o.error() == null && o.decisions() > 0)
                .mapToInt(RolloutOutcome::score).average().orElse(0), 2));
        Map<String, Object> byType = new TreeMap<>();
        Map<DecisionType, List<Sample>> grouped = new EnumMap<>(DecisionType.class);
        samples.forEach(s -> grouped.computeIfAbsent(s.capture().type(), t -> new ArrayList<>()).add(s));
        grouped.forEach((type, list) -> {
            Map<String, Object> t = new LinkedHashMap<>();
            int n = list.size();
            t.put("rollouts", n);
            t.put("forcedRate", round(list.stream().filter(s -> s.outcome().forced()).count() / (double) n, 4));
            t.put("forceSameObservationRate",
                    round(list.stream().filter(s -> s.outcome().forceSameObservation()).count() / (double) n, 4));
            t.put("errors", list.stream().filter(s -> s.outcome().error() != null).count());
            long[] ms = list.stream().filter(s -> s.outcome().error() == null)
                    .mapToLong(s -> s.outcome().nanos()).sorted().toArray();
            t.put("rolloutMsMean", round(mean(ms) / 1e6, 2));
            t.put("rolloutMsP50", round(percentile(ms, 0.5) / 1e6, 2));
            t.put("decisionsMean", round(list.stream().mapToInt(s -> s.outcome().decisions()).average().orElse(0), 1));
            Map<String, Integer> firstTypes = new TreeMap<>();
            for (Sample s : list) {
                if (s.outcome().firstType() != null) {
                    firstTypes.merge(s.outcome().firstType().name(), 1, Integer::sum);
                }
            }
            t.put("firstTypes", firstTypes);
            t.put("completedRate",
                    round(list.stream().filter(s -> s.outcome().completed()).count() / (double) n, 4));
            byType.put(type.name(), t);
        });
        m.put("byType", byType);
        String example = outcomes.stream().filter(o -> o.error() != null).findFirst().map(RolloutOutcome::error).orElse("-");
        System.out.printf(Locale.ROOT,
                "[BENCH] threads=%d: %d rollouts in %.2fs (%.1f/s), mean %.2f ms, forced %.1f%%, same-state %.1f%%, errors %d%s%n",
                scale, outcomes.size(), wall / 1e9, outcomes.size() / Math.max(1e-9, wall / 1e9), mean(nanos) / 1e6,
                100.0 * forced / Math.max(1, outcomes.size()), 100.0 * forceSameObservation / Math.max(1, outcomes.size()),
                errors, errors == 0 ? "" : " e.g. " + example);
        return m;
    }

    private record Sample(Capture capture, RolloutOutcome outcome) {
    }

    private RolloutOutcome rollout(Capture capture, int decisionLimit, boolean hashPolicy) {
        MarsGame copy = new MarsGame(capture.turnStart());
        Player player = copy.getPlayerByUuid(capture.playerUuid());
        ReplayHook hook = new ReplayHook(capture.trail(), capture.type(), capture.chosenId(), capture.ids(),
                capture.observation(), decisionLimit, hashPolicy);
        DecisionProbe.Hook previous = DecisionProbe.set(hook);
        long t0 = System.nanoTime();
        String error = null;
        try {
            // The same single action attempt the live game made, then the rest of the game as usual.
            aiService.processAiTurn(copy, player);
            simulationProcessorService.processSimulation(copy);
        } catch (StopRollout truncated) {
            // expected for a truncated rollout
        } catch (Throwable t) {
            error = t.getClass().getSimpleName() + ": " + t.getMessage();
        } finally {
            DecisionProbe.set(previous);
        }
        long nanos = System.nanoTime() - t0;
        int score = 0;
        boolean completed = copy.getStateType() == StateType.GAME_END;
        if (error == null && completed) {
            List<Player> players = copy.getPlayerUuidToPlayer().values().stream()
                    .sorted(Comparator.comparing(Player::getName)).toList();
            if (players.size() == 2) {
                score = winPointsService.countWinPoints(players.get(0), copy)
                        - winPointsService.countWinPoints(players.get(1), copy);
            }
        }
        return new RolloutOutcome(nanos, hook.decisions, hook.forced, hook.decisionsBeforeForce, hook.firstType,
                hook.firstSameType, hook.firstSameIds, hook.trailBroken, hook.forceSameIds,
                hook.forceSameObservation, completed, score, error);
    }

    /** Captures the live game's decisions and times; never forces anything. */
    private static final class LiveHook implements DecisionProbe.Hook {
        private final Options options;
        private final List<Capture> captures;
        private final Map<DecisionType, Timings> timings;
        private final Map<DecisionType, Integer> liveCounts;
        private final long[] obsStats;
        private final Map<DecisionType, Integer> perType = new EnumMap<>(DecisionType.class);
        private final List<TrailStep> trail = new ArrayList<>();
        private long decisionAt;
        private long previousChoiceAt;
        private int decisions;
        private int kept;
        private boolean atTurnStart = true;
        private MarsGame turnStart;
        private String turnPlayer;
        private long turnCopyNanos;

        LiveHook(Options options, List<Capture> captures, Map<DecisionType, Timings> timings,
                 Map<DecisionType, Integer> liveCounts, long[] obsStats) {
            this.options = options;
            this.captures = captures;
            this.timings = timings;
            this.liveCounts = liveCounts;
            this.obsStats = obsStats;
        }

        @Override
        public void turnStarted(MarsGame game, Player player) {
            // The copy is taken at the first decision of the attempt: nothing has been applied since this point.
            atTurnStart = true;
            turnPlayer = player.getUuid();
        }

        @Override
        public int beforeChoice(Decision decision, MarsGame game, Player player) {
            decisionAt = System.nanoTime();
            decisions++;
            // How much of the observation is actually zero: decides whether a sparse forward pass is worth it.
            float[] observation = decision.observation();
            int nonZero = 0;
            for (float v : observation) {
                if (v != 0f) {
                    nonZero++;
                }
            }
            obsStats[0] += observation.length;
            obsStats[1] += nonZero;
            if (atTurnStart) {
                atTurnStart = false;
                trail.clear();
                turnPlayer = player.getUuid();
                long copyStart = System.nanoTime();
                turnStart = new MarsGame(game);
                turnCopyNanos = System.nanoTime() - copyStart;
            }
            return options.hashPolicy() ? hashChoice(decision) : -1;
        }

        @Override
        public void afterChoice(Decision decision, MarsGame game, Player player, int index) {
            long now = System.nanoTime();
            long gap = previousChoiceAt == 0 ? 0 : decisionAt - previousChoiceAt;
            previousChoiceAt = now;
            timings.computeIfAbsent(decision.type(), type -> new Timings()).add(gap, now - decisionAt);
            liveCounts.merge(decision.type(), 1, Integer::sum);
            int count = perType.merge(decision.type(), 1, Integer::sum);
            if (options.types().contains(decision.type())
                    && count <= options.capturesPerTypePerGame()
                    && kept < options.maxCaptures()
                    && turnStart != null
                    && player.getUuid().equals(turnPlayer)) {
                int[] ids = new int[decision.candidates().size()];
                for (int i = 0; i < ids.length; i++) {
                    ids[i] = decision.candidates().get(i).id();
                }
                captures.add(new Capture(decision.type(), game.getTurns(), ids.length, index, ids[index], ids,
                        decision.observation(), player.getUuid(), turnStart, new ArrayList<>(trail), turnCopyNanos));
                kept++;
            }
            trail.add(new TrailStep(decision.type(), decision.candidates().get(index).id()));
        }
    }

    /**
     * Forces the recorded prefix of the action attempt, then the captured candidate, and counts decisions so the
     * rollout can be truncated after N of them.
     */
    private static final class ReplayHook implements DecisionProbe.Hook {
        private final List<TrailStep> trail;
        private final DecisionType targetType;
        private final int targetId;
        private final int[] targetIds;
        private final float[] targetObservation;
        private final int decisionLimit;
        private final boolean hashPolicy;
        private int trailIndex;
        private boolean trailBroken;
        private boolean forced;
        private boolean forceSameIds;
        private boolean forceSameObservation;
        private int decisions;
        private int decisionsBeforeForce = -1;
        private DecisionType firstType;
        private boolean firstSameType;
        private boolean firstSameIds;

        ReplayHook(List<TrailStep> trail, DecisionType targetType, int targetId, int[] targetIds,
                   float[] targetObservation, int decisionLimit, boolean hashPolicy) {
            this.trail = trail;
            this.targetType = targetType;
            this.targetId = targetId;
            this.targetIds = targetIds;
            this.targetObservation = targetObservation;
            this.decisionLimit = decisionLimit;
            this.hashPolicy = hashPolicy;
        }

        @Override
        public int beforeChoice(Decision decision, MarsGame game, Player player) {
            if (firstType == null) {
                firstType = decision.type();
                firstSameType = trail.isEmpty()
                        ? decision.type() == targetType
                        : decision.type() == trail.getFirst().type();
                firstSameIds = trail.isEmpty() && firstSameType && sameIds(decision);
            }
            if (++decisions > decisionLimit) {
                throw new StopRollout();
            }
            if (trailIndex < trail.size()) {
                TrailStep expected = trail.get(trailIndex++);
                if (decision.type() != expected.type()) {
                    trailBroken = true;
                    return -1;
                }
                int index = indexOf(decision, expected.id());
                if (index < 0) {
                    trailBroken = true;
                    return -1;
                }
                return index;
            }
            if (!forced && decision.type() == targetType) {
                int index = indexOf(decision, targetId);
                if (index >= 0) {
                    forced = true;
                    decisionsBeforeForce = decisions;
                    forceSameIds = sameIds(decision);
                    forceSameObservation = java.util.Arrays.equals(targetObservation, decision.observation());
                    return index;
                }
            }
            return hashPolicy ? hashChoice(decision) : -1;
        }

        private static int indexOf(Decision decision, int id) {
            for (int i = 0; i < decision.candidates().size(); i++) {
                if (decision.candidates().get(i).id() == id) {
                    return i;
                }
            }
            return -1;
        }

        private boolean sameIds(Decision decision) {
            if (decision.candidates().size() != targetIds.length) {
                return false;
            }
            for (int i = 0; i < targetIds.length; i++) {
                if (decision.candidates().get(i).id() != targetIds[i]) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void afterChoice(Decision decision, MarsGame game, Player player, int index) {
        }
    }

    /**
     * Deterministic stand-in for a trained policy: the same decision state always yields the same candidate, but the
     * choices are spread over the candidates like random play. Lets the bench test the resumption mechanism itself,
     * without trained models (uniform random would re-decide the prefix differently and mask the state comparison).
     */
    private static int hashChoice(Decision decision) {
        long h = 0xcbf29ce484222325L;
        for (float v : decision.observation()) {
            h ^= Float.floatToIntBits(v) & 0xffffffffL;
            h *= 0x100000001b3L;
        }
        h ^= decision.type().ordinal();
        h *= 0x100000001b3L;
        for (com.terraforming.ares.services.agent.Candidate candidate : decision.candidates()) {
            h ^= candidate.id();
            h *= 0x100000001b3L;
        }
        return (int) Math.floorMod(h, decision.candidates().size());
    }

    private static double mean(long[] values) {        if (values.length == 0) {
            return 0;
        }
        long sum = 0;
        for (long v : values) {
            sum += v;
        }
        return sum / (double) values.length;
    }

    private static double percentile(long[] sorted, double p) {
        if (sorted.length == 0) {
            return 0;
        }
        int index = (int) Math.round(p * (sorted.length - 1));
        return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
    }

    private static double percentile(List<Long> values, double p) {
        long[] sorted = values.stream().mapToLong(Long::longValue).sorted().toArray();
        return percentile(sorted, p);
    }

    private static long gcSeconds() {
        long millis = 0;
        for (java.lang.management.GarbageCollectorMXBean gc
                : java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gc.getCollectionTime() > 0) {
                millis += gc.getCollectionTime();
            }
        }
        return millis;
    }

    private static double round(double value, int digits) {        double factor = Math.pow(10, digits);
        return Math.round(value * factor) / factor;
    }

    public static Set<DecisionType> parseTypes(String spec) {
        if (spec == null || spec.isBlank() || spec.equalsIgnoreCase("all")) {
            return EnumSet.allOf(DecisionType.class);
        }
        Set<DecisionType> result = EnumSet.noneOf(DecisionType.class);
        for (String part : spec.split(",")) {
            result.add(DecisionType.valueOf(part.trim().toUpperCase(Locale.ROOT)));
        }
        return result;
    }

    public static List<Integer> parseScales(String spec) {
        List<Integer> result = new ArrayList<>();
        for (String part : spec.split(",")) {
            int scale = Integer.parseInt(part.trim());
            if (scale < 1) {
                throw new IllegalArgumentException("scale must be positive: " + part);
            }
            result.add(scale);
        }
        return result;
    }
}
