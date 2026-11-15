package com.terraforming.ares.selfplay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraforming.ares.TerraformingAresApplication;
import com.terraforming.ares.controllers.SimulationCompareController;
import com.terraforming.ares.model.PlayerDifficulty;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command line entry point of the night pipeline (no web server, no database).
 * <pre>
 * play:    --mode play --out DIR --games N [--threads T] [--matchups AGENT:AGENT:0.5,AGENT_R:AGENT:0.5]
 *                      [--games-per-chunk C] [--max-seconds S] [--stop-file FILE] [--max-failure-ratio 0.05] [--min-free-gb 30]
 *                      [--max-rows-per-game-type K]   (write at most K random decisions of a game per type; 0 = all)
 *                      [--count-chunks-from DIR]      (also count finished .ares there for resume numbering; SSD stage + HDD)
 *                      [--model MODEL_DIR | --models MODELS_DIR] [--temperature 0.5]
 * compare: --mode compare --out FILE.json --a AGENT --b SMART --games N [--concurrency C]
 *                      [--model MODEL_DIR | --models MODELS_DIR] [--temperature 0]
 * score:   --mode scorecard --out FILE.json --games N [--a AGENT] [--b SMART] [--concurrency C]
 *                      (where the points go: Tr, forests, cards, resources, milestones, awards per side)
 * bench:   --mode bench --out FILE.json [--games N] [--types A,B|all] [--captures-per-type 2] [--max-captures 400]
 *                      [--truncate-decisions N] [--rollouts-per-capture 1] [--scales 1,2,4,8] [--max-seconds S]
 *                      [--hash-policy false|true] [--synthetic-model false|true]
 * </pre>
 * {@code --model} is one exported folder (legacy / single decision). {@code --models} is a directory of such folders
 * (one per decision type), as written by {@code ml/train_all.py}.
 * Exit codes: 0 completed, 3 stopped on request (stop file / time limit), 4 too many failed games, 5 disk almost full (stopped by itself),
 * 6 out of memory (restart me), 2 bad arguments, 1 crash.
 * Everything unexpected is written to stderr with the stack trace (the pipeline keeps it in the stage log).
 */
public final class SelfPlayMain {
    private SelfPlayMain() {
    }

    public static void main(String[] args) {
        System.setProperty(FatalErrors.PROPERTY, "true");
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            FatalErrors.haltIfOutOfMemory(throwable);
            System.err.println("UNCAUGHT in thread " + thread.getName());
            throwable.printStackTrace();
        });
        int code;
        try {
            code = run(args);
        } catch (IllegalArgumentException e) {
            FatalErrors.haltIfOutOfMemory(e);
            System.err.println("BAD ARGUMENTS: " + e.getMessage());
            code = 2;
        } catch (Throwable t) {
            FatalErrors.haltIfOutOfMemory(t);
            System.err.println("SELF-PLAY CRASHED: " + t);
            t.printStackTrace();
            code = 1;
        }
        System.out.flush();
        System.err.flush();
        System.exit(code);
    }

    static int run(String[] args) throws Exception {
        Map<String, String> arguments = parse(args);
        String mode = arguments.getOrDefault("mode", "play");
        if (mode.equals("names")) {  // no game and no Spring context needed
            List<String> names = com.terraforming.ares.services.agent.PickPhaseDecisions.featureNames();
            Path file = Path.of(required(arguments, "out"));
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            new ObjectMapper().writeValue(file.toFile(), names);
            System.out.println("[NAMES] " + names.size() + " feature names written to " + file);
            return 0;
        }
        if (mode.equals("scales")) {  // name + divisor per observation value: tooling can report real units
            Path file = Path.of(required(arguments, "out"));
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("names", com.terraforming.ares.services.agent.ObservationBuilder.names());
            float[] scales = com.terraforming.ares.services.agent.ObservationBuilder.scales();
            java.util.List<Float> boxes = new java.util.ArrayList<>(scales.length);
            for (float scale : scales) {
                boxes.add(scale);
            }
            payload.put("scales", boxes);
            new ObjectMapper().writeValue(file.toFile(), payload);
            System.out.println("[SCALES] " + scales.length + " values -> " + file);
            return 0;
        }
        if (mode.equals("schemas")) {  // the encoder's schema hash per decision type: what a converted model must carry
            Path file = Path.of(required(arguments, "out"));
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            new ObjectMapper().writeValue(file.toFile(),
                    com.terraforming.ares.services.agent.NeuralPolicy.schemaDetails());
            System.out.println("[SCHEMAS] written to " + file);
            return 0;
        }
        // the agent model is read by NeuralPolicy when the context starts
        if (arguments.containsKey("models")) {
            System.setProperty("ares.agent.models", arguments.get("models"));
            System.setProperty("ares.agent.temperature", arguments.getOrDefault("temperature", "0"));
        }
        if (arguments.containsKey("model")) {
            System.setProperty("ares.agent.model", arguments.get("model"));
            System.setProperty("ares.agent.temperature", arguments.getOrDefault("temperature", "0"));
        }

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TerraformingAresApplication.class)
                .profiles("selfplay")
                .web(WebApplicationType.NONE)
                .properties("spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration",
                        "spring.main.banner-mode=off", "logging.level.root=WARN")
                .run()) {
            switch (mode) {
                case "play":
                    return play(context, arguments);
                case "compare":
                    return compare(context, arguments);
                case "bench":
                    return bench(context, arguments);
                case "audit":
                    return audit(context, arguments);
                case "scorecard":
                    return scorecard(context, arguments);
                case "cards":
                    return cards(context, arguments);
                case "sanity":
                    return sanity(context, arguments);
                case "engineaudit":
                    return engineAudit(context, arguments);
                case "macroaudit":
                    return macroAudit(context, arguments);
                case "rollouttest":
                    return rolloutTest(context, arguments);
                case "phasedist":
                    return phaseDist(context, arguments);
                default:
                    throw new IllegalArgumentException("unknown --mode " + mode);
            }
        }
    }

    private static int play(ConfigurableApplicationContext context, Map<String, String> a) throws Exception {
        Path out = Path.of(required(a, "out"));
        int games = Integer.parseInt(required(a, "games"));
        // In-flight games. Every game keeps all its decisions in memory until it ends, so this bounds the heap.
        int threads = Integer.parseInt(a.getOrDefault("threads",
                String.valueOf(Runtime.getRuntime().availableProcessors() * 4)));
        if (games < 1 || threads < 1) {
            throw new IllegalArgumentException("games and threads must be positive");
        }
        SelfPlayRunner.Options options = new SelfPlayRunner.Options(
                out, games, threads,
                matchups(a.getOrDefault("matchups", "AGENT:AGENT:1")),
                Integer.parseInt(a.getOrDefault("games-per-chunk", "500")),
                Long.parseLong(a.getOrDefault("max-seconds", "0")),
                a.containsKey("stop-file") ? Path.of(a.get("stop-file")) : null,
                Double.parseDouble(a.getOrDefault("max-failure-ratio", "0.05")),
                Double.parseDouble(a.getOrDefault("min-free-gb", "0")),
                a.containsKey("count-chunks-from") ? Path.of(a.get("count-chunks-from")) : null);

        context.getBean(com.terraforming.ares.services.agent.DecisionLogs.class)
                .setMaxRowsPerGameType(Integer.parseInt(a.getOrDefault("max-rows-per-game-type", "0")));
        SelfPlayRunner.Summary summary = context.getBean(SelfPlayRunner.class).run(options);
        System.out.println("[SELFPLAY] done: " + new ObjectMapper().writeValueAsString(summary));
        return switch (summary.stoppedReason()) {
            case "COMPLETED" -> 0;
            case "TOO_MANY_FAILURES" -> 4;
            case "DISK_LOW" -> 5;
            default -> 3;
        };
    }

    /**
     * Measures the cost and the feasibility of search (see {@link RolloutBench}). A diagnostic mode: it does not write
     * dataset rows and does not change anything the night pipeline reads.
     */
    private static int bench(ConfigurableApplicationContext context, Map<String, String> a) throws Exception {
        Path out = Path.of(required(a, "out"));
        RolloutBench.Options options = new RolloutBench.Options(
                out,
                Integer.parseInt(a.getOrDefault("games", "50")),
                RolloutBench.parseTypes(a.get("types")),
                Integer.parseInt(a.getOrDefault("captures-per-type", "2")),
                Integer.parseInt(a.getOrDefault("max-captures", "400")),
                Integer.parseInt(a.getOrDefault("truncate-decisions", "0")),
                Integer.parseInt(a.getOrDefault("rollouts-per-capture", "1")),
                RolloutBench.parseScales(a.getOrDefault("scales", "1")),
                Long.parseLong(a.getOrDefault("max-seconds", "0")),
                Boolean.parseBoolean(a.getOrDefault("hash-policy", "false")),
                Boolean.parseBoolean(a.getOrDefault("synthetic-model", "false")));
        context.getBean(RolloutBench.class).run(options);
        return 0;
    }

    /**
     * Sanity check of a value network (see {@link ValueAudit}): perturb one thing in real positions and report whether
     * V moves the way the rules of the game imply. Diagnostic only.
     */
    private static int audit(ConfigurableApplicationContext context, Map<String, String> a) throws Exception {
        java.util.List<Integer> cards = new java.util.ArrayList<>();
        for (String part : a.getOrDefault("cards", "").split(",")) {
            if (!part.isBlank()) {
                cards.add(Integer.parseInt(part.trim()));
            }
        }
        java.util.List<String> interactionCards = new java.util.ArrayList<>();
        for (String part : a.getOrDefault("interaction-cards", "").split(",")) {
            if (!part.isBlank()) {
                interactionCards.add(part.trim());
            }
        }
        ValueAudit.Options options = new ValueAudit.Options(
                Path.of(required(a, "out")),
                Integer.parseInt(a.getOrDefault("games", "100")),
                Integer.parseInt(a.getOrDefault("captures-per-game", "6")),
                a.getOrDefault("opponent", "SMART"),
                cards,
                interactionCards);
        context.getBean(ValueAudit.class).run(options);
        return 0;
    }

    /**
     * The obvious-move bench: positions where the rules alone say what is better, run through the live decision path.
     * Exit 1 when a scenario fails, so a night can refuse a model that does not even take a free card.
     */
    private static int sanity(ConfigurableApplicationContext context, Map<String, String> a) throws Exception {
        SanityBench.Report report = context.getBean(SanityBench.class).run(Path.of(required(a, "out")));
        return report.failed() == 0 ? 0 : 1;
    }

    /**
     * Is a rollout a better chooser than the policy? For each captured decision the position is replayed once per
     * candidate and once with the policy's own choice; the paired difference says whether planning can improve the player.
     */
    private static int rolloutTest(ConfigurableApplicationContext context, Map<String, String> a) throws Exception {
        RolloutImprovement.Report report = context.getBean(RolloutImprovement.class).run(
                Path.of(required(a, "out")),
                Integer.parseInt(a.getOrDefault("games", "24")),
                com.terraforming.ares.services.agent.DecisionType.valueOf(a.getOrDefault("type", "BUILD_CARD")));
        return report.positions() == 0 ? 1 : 0;
    }

    /**
     * Causal test of the engine plan: the same position played twice (same deck, same opponent), once plainly and once with
     * a macro that presses every blue action for a few turns. The paired difference is the effect of committing to the
     * engine - the evidence a critic or a policy can be taught from, without a teacher.
     */
    private static int macroAudit(ConfigurableApplicationContext context, Map<String, String> a) throws Exception {
        MacroAudit.Report report = context.getBean(MacroAudit.class).run(
                Path.of(required(a, "out")),
                Integer.parseInt(a.getOrDefault("games", "24")),
                Integer.parseInt(a.getOrDefault("turns", "8")),
                a.getOrDefault("macro", "all"));
        return report.positions() == 0 ? 1 : 0;
    }

    /**
     * Does the critic price a blue engine? V of a real position, then with several activatable blue action cards on the
     * table, then after pressing them. Diagnostic: it says whether a search could ever prefer an engine.
     */
    private static int engineAudit(ConfigurableApplicationContext context, Map<String, String> a) throws Exception {
        EngineAudit.Report report = context.getBean(EngineAudit.class).run(
                Path.of(required(a, "out")),
                Integer.parseInt(a.getOrDefault("games", "12")),
                a.getOrDefault("value-model", "D:/ares/runs/value/v_r15_outcome"),
                Integer.parseInt(a.getOrDefault("cards", "4")));
        return report.positions() == 0 ? 1 : 0;
    }

    /** Which project card class belongs to which expansion: the audit uses it to classify dead observation bits. */
    private static int cards(ConfigurableApplicationContext context, Map<String, String> a) throws Exception {
        Path file = Path.of(required(a, "out"));
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        java.util.Map<String, java.util.List<String>> byExpansion = new java.util.LinkedHashMap<>();
        var projects = context.getBean(com.terraforming.ares.services.CardFactory.class).createAllProjects();
        for (var expansion : com.terraforming.ares.model.Expansion.values()) {
            var cards = projects.get(expansion);
            if (cards == null) {
                continue;
            }
            java.util.List<String> names = new java.util.ArrayList<>();
            for (var card : cards.values()) {
                names.add(card.getClass().getSimpleName());
            }
            java.util.Collections.sort(names);
            byExpansion.put(expansion.name(), names);
        }
        new ObjectMapper().writeValue(file.toFile(), byExpansion);
        System.out.println("[CARDS] " + byExpansion.size() + " expansions -> " + file);
        return 0;
    }

    /** Where the points go, per term of the score, for {@code a} and {@code b}. Diagnostic only. */
    private static int phaseDist(ConfigurableApplicationContext context, Map<String, String> a) throws Exception {
        context.getBean(com.terraforming.ares.selfplay.PhaseDistribution.class).run(
                Path.of(required(a, "out")),
                Integer.parseInt(required(a, "games")),
                PlayerDifficulty.valueOf(a.getOrDefault("a", "NETWORK")),
                PlayerDifficulty.valueOf(a.getOrDefault("b", "RANDOM")),
                Integer.parseInt(a.getOrDefault("concurrency", "16")));
        return 0;
    }

    private static int scorecard(ConfigurableApplicationContext context, Map<String, String> a) throws Exception {
        context.getBean(com.terraforming.ares.selfplay.Scorecard.class).run(
                Path.of(required(a, "out")),
                Integer.parseInt(required(a, "games")),
                PlayerDifficulty.valueOf(a.getOrDefault("a", "AGENT")),
                PlayerDifficulty.valueOf(a.getOrDefault("b", "SMART")),
                Integer.parseInt(a.getOrDefault("concurrency", "16")));
        return 0;
    }

    private static int compare(ConfigurableApplicationContext context, Map<String, String> a) throws Exception {        Path out = Path.of(required(a, "out"));
        SimulationCompareController.CompareResult result = context.getBean(SimulationCompareController.class).compare(
                Integer.parseInt(required(a, "games")),
                PlayerDifficulty.valueOf(required(a, "a")),
                PlayerDifficulty.valueOf(required(a, "b")),
                Integer.parseInt(a.getOrDefault("concurrency", "64")));
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        Path tmp = Path.of(out + ".tmp");
        new ObjectMapper().writeValue(tmp.toFile(), result);
        Files.move(tmp, out, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[COMPARE] " + result);
        return result.failedGames() > Math.max(5, result.games() / 20) ? 4 : 0;
    }

    public static List<SelfPlayRunner.Matchup> matchups(String spec) {
        List<SelfPlayRunner.Matchup> result = new ArrayList<>();
        for (String item : spec.split(",")) {
            String[] parts = item.trim().split(":");
            if (parts.length != 3) {
                throw new IllegalArgumentException("matchup must look like A:B:weight, got '" + item + "'");
            }
            double weight = Double.parseDouble(parts[2]);
            if (!(weight > 0)) {
                throw new IllegalArgumentException("matchup weight must be positive: " + item);
            }
            result.add(new SelfPlayRunner.Matchup(PlayerDifficulty.valueOf(parts[0]), PlayerDifficulty.valueOf(parts[1]), weight));
        }
        return result;
    }

    public static Map<String, String> parse(String[] args) {
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("--") || i + 1 >= args.length) {
                throw new IllegalArgumentException("expected --name value pairs, got '" + args[i] + "'");
            }
            result.put(args[i].substring(2), args[++i]);
        }
        return result;
    }

    private static String required(Map<String, String> arguments, String name) {
        String value = arguments.get(name);
        if (value == null) {
            throw new IllegalArgumentException("missing --" + name);
        }
        return value;
    }
}
