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
 *                      [--model MODEL_DIR] [--temperature 0.5]
 * compare: --mode compare --out FILE.json --a AGENT --b SMART --games N [--concurrency C] [--model MODEL_DIR] [--temperature 0]
 * </pre>
 * Exit codes: 0 completed, 3 stopped on request (stop file / time limit), 4 too many failed games, 5 disk almost full (stopped by itself), 2 bad arguments, 1 crash.
 * Everything unexpected is written to stderr with the stack trace (the pipeline keeps it in the stage log).
 */
public final class SelfPlayMain {
    private SelfPlayMain() {
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("UNCAUGHT in thread " + thread.getName());
            throwable.printStackTrace();
        });
        int code;
        try {
            code = run(args);
        } catch (IllegalArgumentException e) {
            System.err.println("BAD ARGUMENTS: " + e.getMessage());
            code = 2;
        } catch (Throwable t) {
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
        // the agent model is read by NeuralPolicy when the context starts
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
                default:
                    throw new IllegalArgumentException("unknown --mode " + mode);
            }
        }
    }

    private static int play(ConfigurableApplicationContext context, Map<String, String> a) throws Exception {
        Path out = Path.of(required(a, "out"));
        int games = Integer.parseInt(required(a, "games"));
        int threads = Integer.parseInt(a.getOrDefault("threads", String.valueOf(Runtime.getRuntime().availableProcessors())));
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
                Double.parseDouble(a.getOrDefault("min-free-gb", "0")));

        SelfPlayRunner.Summary summary = context.getBean(SelfPlayRunner.class).run(options);
        System.out.println("[SELFPLAY] done: " + new ObjectMapper().writeValueAsString(summary));
        return switch (summary.stoppedReason()) {
            case "COMPLETED" -> 0;
            case "TOO_MANY_FAILURES" -> 4;
            case "DISK_LOW" -> 5;
            default -> 3;
        };
    }

    private static int compare(ConfigurableApplicationContext context, Map<String, String> a) throws Exception {
        Path out = Path.of(required(a, "out"));
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
