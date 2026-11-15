package com.terraforming.ares.selfplay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.agent.Decision;
import com.terraforming.ares.services.agent.DecisionProbe;
import com.terraforming.ares.services.agent.DecisionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Is a rollout a better chooser than the policy? The decisive measurement for "improve the player by planning".
 * <p>
 * For a real decision point the position is replayed once per candidate with a probe that forces that candidate at the
 * decision and lets the policy play the rest of the game; every replay starts from the same copy (same deck, same dummy,
 * same opponent), so the differences are paired. The baseline is one replay in which the policy chooses by itself.
 * <p>
 * If the best-by-rollout candidate beats the policy's choice on the same positions, then planning is an improvement
 * operator for this player and its choices can be distilled into the fast policy. If it does not, no amount of search
 * will help, and the ceiling is the policy itself.
 * <p>
 * {@code --mode rollouttest --out FILE.json --games N --type BUILD_CARD|PERFORM_BLUE_ACTION|PICK_PHASE}
 */
@Component
@RequiredArgsConstructor
public class RolloutImprovement {
    public record Sample(DecisionType type, int candidates, int policyIndex, int bestIndex, double policyMargin,
                         double bestMargin, double difference, boolean sameChoice) {
    }

    public record Report(String type, int positions, double policyMargin, double bestMargin, double meanDifference,
                         double sd, int betterWithRollout, int betterWithPolicy, int agree,
                         List<Sample> samples) {
    }

    private final GameService gameService;
    private final SimulationProcessorService simulation;
    private final WinPointsService winPointsService;

    /** Captures the position and the decision the policy takes there, then stops the game. */
    private static final class Capture implements DecisionProbe.Hook {
        final DecisionType type;
        MarsGame snapshot;
        Decision decision;
        int policyIndex = -1;
        String playerUuid;

        Capture(DecisionType type) {
            this.type = type;
        }

        @Override
        public int beforeChoice(Decision decision, MarsGame game, Player player) {
            if (snapshot == null && decision.type() == type && decision.candidates().size() > 1) {
                snapshot = new MarsGame(game);
                this.decision = decision;
                this.playerUuid = player.getUuid();
            }
            return -1;
        }

        @Override
        public void afterChoice(Decision decision, MarsGame game, Player player, int index) {
            if (snapshot != null && player.getUuid().equals(playerUuid) && policyIndex < 0
                    && decision.type() == type) {
                policyIndex = index;
            }
        }
    }

    /** Forces one candidate at the first decision of the target type BELONGING TO THE CAPTURED PLAYER. */
    private static final class Force implements DecisionProbe.Hook {
        final DecisionType type;
        final int index;
        final String playerUuid;
        boolean used;

        Force(DecisionType type, int index, String playerUuid) {
            this.type = type;
            this.index = index;
            this.playerUuid = playerUuid;
        }

        @Override
        public int beforeChoice(Decision decision, MarsGame game, Player player) {
            if (!used && decision.type() == type && player.getUuid().equals(playerUuid)
                    && index < decision.candidates().size()) {
                used = true;
                return index;
            }
            return -1;
        }

        @Override
        public void afterChoice(Decision decision, MarsGame game, Player player, int chosen) {
        }
    }

    public Report run(Path out, int positions, DecisionType type) throws Exception {
        List<Sample> samples = new ArrayList<>();
        int guard = 0;
        while (samples.size() < positions && guard++ < positions * 10) {
            Capture capture = new Capture(type);
            MarsGame game = newGame();
            DecisionProbe.Hook previous = DecisionProbe.set(capture);
            try {
                simulation.processSimulation(game, current -> {
                    if (capture.snapshot != null) {
                        throw new StopPlayout();
                    }
                });
            } catch (StopPlayout stop) {
                // the position is captured; the game is abandoned here
            } finally {
                DecisionProbe.set(previous);
            }
            if (capture.snapshot == null || capture.policyIndex < 0) {
                continue;
            }
            MarsGame base = capture.snapshot;
            String uuid = capture.playerUuid;
            int candidates = capture.decision.candidates().size();
            long selectionSeed = 11L * (samples.size() + 1);
            long evaluationSeed = 7919L * (samples.size() + 1);
            // SELECT on one determinization of the hidden cards, EVALUATE on a different one: picking the best of several
            // noisy rollouts and reading that same number would be the winner's curse (the maximum of k noisy estimates is
            // biased upward by ~1 sigma of their noise).
            double bestMargin = Double.NEGATIVE_INFINITY;
            int bestIndex = capture.policyIndex;
            for (int index = 0; index < candidates; index++) {
                double margin = playOut(new MarsGame(base), uuid, new Force(type, index, uuid), selectionSeed);
                if (margin > bestMargin) {
                    bestMargin = margin;
                    bestIndex = index;
                }
            }
            double policyMargin = playOut(new MarsGame(base), uuid, null, evaluationSeed);
            double chosenMargin = playOut(new MarsGame(base), uuid, new Force(type, bestIndex, uuid), evaluationSeed);
            samples.add(new Sample(type, candidates, capture.policyIndex, bestIndex, policyMargin, chosenMargin,
                    chosenMargin - policyMargin, capture.policyIndex == bestIndex));
        }
        double meanPolicy = samples.stream().mapToDouble(Sample::policyMargin).average().orElse(0);
        double meanBest = samples.stream().mapToDouble(Sample::bestMargin).average().orElse(0);
        double mean = samples.stream().mapToDouble(Sample::difference).average().orElse(0);
        double sd = 0;
        for (Sample sample : samples) {
            sd += (sample.difference() - mean) * (sample.difference() - mean);
        }
        sd = samples.size() > 1 ? Math.sqrt(sd / (samples.size() - 1)) : 0;
        Report report = new Report(type.name(), samples.size(), meanPolicy, meanBest, mean, sd,
                (int) samples.stream().filter(s -> s.difference() > 0.5).count(),
                (int) samples.stream().filter(s -> s.difference() < -0.5).count(),
                (int) samples.stream().filter(Sample::sameChoice).count(),
                samples);
        if (out != null && out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        if (out != null) {
            new ObjectMapper().writeValue(out.toFile(), report);
        }
        System.out.printf("[ROLLOUT] %s: %d positions, policy %+.1f, best-by-rollout %+.1f, difference %+.1f +/- %.1f "
                        + "(better with rollout %d, with policy %d, same choice %d)%n",
                report.type(), report.positions(), report.policyMargin(), report.bestMargin(), report.meanDifference(),
                1.96 * report.sd() / Math.sqrt(Math.max(1, report.positions())), report.betterWithRollout(),
                report.betterWithPolicy(), report.agree());
        return report;
    }

    private double playOut(MarsGame sim, String uuid, DecisionProbe.Hook hook, long seed) {
        com.terraforming.ares.services.agent.Determinizer.shuffle(sim, uuid, seed);
        DecisionProbe.Hook previous = DecisionProbe.set(hook);
        try {
            simulation.processSimulation(sim);
        } finally {
            DecisionProbe.set(previous);
        }
        Player me = sim.getPlayerByUuid(uuid);
        Player other = sim.getPlayerUuidToPlayer().values().stream()
                .filter(p -> !p.getUuid().equals(uuid)).findFirst().orElseThrow();
        return winPointsService.countWinPoints(me, sim) - winPointsService.countWinPoints(other, sim);
    }

    private static final class StopPlayout extends RuntimeException {
        StopPlayout() {
            super(null, null, false, false);
        }
    }

    private MarsGame newGame() {
        return gameService.createNewSimulation(GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT))
                .mulligan(true)
                .expansion(com.terraforming.ares.model.Expansion.BASE)
                .expansion(com.terraforming.ares.model.Expansion.DISCOVERY)
                .expansion(com.terraforming.ares.model.Expansion.BUFFED_CORPORATION)
                .dummyHand(true).build());
    }
}
