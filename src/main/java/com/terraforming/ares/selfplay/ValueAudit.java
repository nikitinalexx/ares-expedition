package com.terraforming.ares.selfplay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraforming.ares.TerraformingAresApplication;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.agent.Decision;
import com.terraforming.ares.services.agent.DecisionProbe;
import com.terraforming.ares.services.agent.MlpValueModel;
import com.terraforming.ares.services.agent.ObservationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Sanity check of a trained value network: <b>perturb one thing in a real position and see whether V moves the way the
 * game says it should</b>. This is deliberately not a win-rate measurement - it answers "does the network see what the
 * encoder claims to encode", which is the failure mode that no amount of training fixes.
 * <p>
 * For every captured position the audit computes the same score the search uses ({@code V(me) - V(opponent)}, halved,
 * averaged over the ensemble), then applies one controlled change to a copy and reports the difference:
 * <ul>
 *   <li>resources: +10 MC, +5 plants, +5 heat, +1 TR, +1 forest;</li>
 *   <li>incomes: +1 MC / cards / plants / heat / steel / titanium per generation;</li>
 *   <li>cards: one specific card added to the played table, per card (this is where a silent encoding bug shows up:
 *       a card that changes nothing in V is either worthless or missing from the state);</li>
 *   <li>cards in hand: +1 card (information, not income).</li>
 * </ul>
 * The expected sign is printed next to every perturbation, so an anomaly is visible without interpretation; the per-card
 * table can then be compared with the empirical strength of the same cards (built-card correlation, `ml/card_values.py`).
 * <p>
 * Diagnostic only: nothing here runs in a game.
 */
@Component
@RequiredArgsConstructor
public class ValueAudit {
    public record Options(Path out, int games, int capturesPerGame, String opponent, List<Integer> cards,
                          List<String> interactionCards) {
    }

    private record Capture(String playerUuid, MarsGame game, int round) {
    }

    /** One perturbation of a position: what changes, the expected sign of dV, and how to apply it. */
    private record Perturbation(String name, String expected, Perturb action) {
        interface Perturb {
            void run(MarsGame game, Player player);
        }
    }

    private final GameService gameService;
    private final SimulationProcessorService simulationProcessorService;
    private final ObservationBuilder observationBuilder;
    private final com.terraforming.ares.services.CardService cardService;

    public void run(Options options) throws Exception {
        List<MlpValueModel> models = loadModels();
        List<Capture> captures = new ArrayList<>();
        for (int g = 0; g < options.games(); g++) {
            MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                    .playerNames(new ArrayList<>(List.of("Seat1", "Seat2")))
                    .computers(List.of(PlayerDifficulty.AGENT, PlayerDifficulty.valueOf(options.opponent())))
                    .mulligan(true)
                    .expansion(Expansion.BASE).expansion(Expansion.BUFFED_CORPORATION).expansion(Expansion.DISCOVERY)
                    .dummyHand(true)
                    .build());
            DecisionProbe.Hook previous = DecisionProbe.set(new CaptureHook(captures, options.capturesPerGame()));
            try {
                simulationProcessorService.processSimulation(game);
            } finally {
                DecisionProbe.set(previous);
            }
            if ((g + 1) % 20 == 0) {
                System.out.printf(Locale.ROOT, "[AUDIT] %d games, %d positions%n", g + 1, captures.size());
            }
        }
        System.out.printf(Locale.ROOT, "[AUDIT] %d positions captured%n", captures.size());

        Map<String, List<Double>> deltas = new TreeMap<>();
        Map<String, List<Double>> deltasByRound = new TreeMap<>();
        Map<String, List<Integer>> roundsByValue = new TreeMap<>();
        Map<String, List<String>> changedFeatures = new TreeMap<>();
        // Interaction test (owner, 2026-11-14): a blue card's value depends on the state - AquiferPumping pumps cheaper
        // oceans the more steel you make. The flag and the steel income are both in the observation, so a value net CAN
        // learn that. This splits every perturbation by whether the card is on my table, which is what shows whether it did.
        Map<String, Map<String, List<Double>>> withCard = new TreeMap<>();
        Map<String, Map<String, List<Double>>> withoutCard = new TreeMap<>();
        List<String> names = ObservationBuilder.names();
        List<Double> calibration = new ArrayList<>();
        for (Capture capture : captures) {
            float base = score(capture.game(), capture.playerUuid(), models);
            Player me = capture.game().getPlayerByUuid(capture.playerUuid());
            Player other = other(capture.game(), me);
            float[] baseObservation = observationBuilder.build(capture.game(), me);
            calibration.add((double) (models.get(0).value(baseObservation)
                    + models.get(0).value(observationBuilder.build(capture.game(), other))));
            for (Perturbation perturbation : standardPerturbations(options.cards())) {
                MarsGame copy = new MarsGame(capture.game());
                Player player = copy.getPlayerByUuid(capture.playerUuid());
                try {
                    perturbation.action().run(copy, player);
                } catch (RuntimeException e) {
                    continue; // a perturbation that does not apply to this position is simply skipped
                }
                double delta = score(copy, capture.playerUuid(), models) - base;
                deltas.computeIfAbsent(perturbation.name(), key -> new ArrayList<>()).add(delta);
                deltasByRound.computeIfAbsent(perturbation.name(), key -> new ArrayList<>()).add(delta);
                roundsByValue.computeIfAbsent(perturbation.name(), key -> new ArrayList<>()).add(capture.round());
                for (String card : options.interactionCards()) {
                    Set<String> played = playedCardNames(capture.game(), capture.playerUuid());
                    Map<String, Map<String, List<Double>>> target = played.contains(card) ? withCard : withoutCard;
                    target.computeIfAbsent(card, key -> new TreeMap<>())
                            .computeIfAbsent(perturbation.name(), key -> new ArrayList<>()).add(delta);
                }
                // Which features actually moved: distinguishes "V ignores this" from "the encoder does not carry it".
                float[] perturbed = observationBuilder.build(copy, copy.getPlayerByUuid(capture.playerUuid()));
                List<String> moved = new ArrayList<>();
                for (int i = 0; i < Math.min(baseObservation.length, perturbed.length); i++) {
                    if (Math.abs(baseObservation[i] - perturbed[i]) > 1e-6f) {
                        moved.add(i < names.size() ? names.get(i) : ("#" + i));
                        if (moved.size() >= 20) {
                            break;
                        }
                    }
                }
                if (!moved.isEmpty()) {
                    List<String> shared = changedFeatures.computeIfAbsent(perturbation.name(), key -> new ArrayList<>());
                    if (shared.size() < 20) {
                        shared.addAll(moved.subList(0, Math.min(moved.size(), 20 - shared.size())));
                    }
                }
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("games", options.games());
        report.put("opponent", options.opponent());
        report.put("positions", captures.size());
        report.put("models", models.size());
        report.put("positionsSummary", summary(captures));
        report.put("scale", "score = mean over nets of (logit(me) - logit(opponent)) / 2; dV is printed in these units");
        report.put("calibration", Map.of(
                "mean_V_me_plus_V_opp", round(mean(calibration), 4),
                "median_V_me_plus_V_opp", round(median(calibration), 4),
                "note", "V(me) + V(opp) = 1 means the two readings are consistent; the deviation is what the symmetric "
                        + "score cancels"));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Perturbation perturbation : standardPerturbations(options.cards())) {
            List<Double> values = deltas.get(perturbation.name());
            if (values == null || values.isEmpty()) {
                continue;
            }
            long positive = values.stream().filter(v -> v > 0).count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("perturbation", perturbation.name());
            row.put("expected", perturbation.expected());
            row.put("n", values.size());
            row.put("meanDelta", round(mean(values), 5));
            row.put("medianDelta", round(median(values), 5));
            row.put("sharePositive", round(positive / (double) values.size(), 3));
            row.put("shareNegative", round((values.size() - positive) / (double) values.size(), 3));
            row.put("changedFeatures", changedFeatures.getOrDefault(perturbation.name(), List.of()));
            // The owner's test: the value of a production card must FALL as the game runs out. If the deltas do not move
            // with the round, V is round-blind exactly where it matters most.
            row.put("byRound", byRound(deltasByRound.get(perturbation.name()), roundsByValue.get(perturbation.name())));
            rows.add(row);
        }
        report.put("perturbations", rows);
        // The per-card rows are the interesting part for "which cards does the network not see at all": sorted by dV.
        List<Map<String, Object>> cards = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (!row.get("perturbation").toString().startsWith("played +")) {
                continue;
            }
            cards.add(row);
        }
        cards.sort(Comparator.comparingDouble((Map<String, Object> row) -> (double) row.get("meanDelta")).reversed());
        report.put("cardsByDelta", cards);

        Map<String, Object> interactions = new LinkedHashMap<>();
        for (String card : options.interactionCards()) {
            Map<String, List<Double>> with = withCard.getOrDefault(card, Map.of());
            Map<String, List<Double>> without = withoutCard.getOrDefault(card, Map.of());
            Map<String, Object> perCard = new LinkedHashMap<>();
            TreeSet<String> keys = new TreeSet<>(with.keySet());
            keys.addAll(without.keySet());
            for (String perturbation : keys) {
                List<Double> a = with.get(perturbation);
                List<Double> b = without.get(perturbation);
                if (a == null || b == null || a.size() < 30 || b.size() < 30) {
                    continue;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("withCard", round(mean(a), 4));
                row.put("nWith", a.size());
                row.put("withoutCard", round(mean(b), 4));
                row.put("nWithout", b.size());
                row.put("difference", round(mean(a) - mean(b), 4));
                perCard.put(perturbation, row);
            }
            if (!perCard.isEmpty()) {
                interactions.put(card, perCard);
            }
        }
        report.put("interactions", interactions);

        Path tmp = Path.of(options.out() + ".tmp");
        Files.createDirectories(tmp.toAbsolutePath().getParent());
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), report);
        Files.move(tmp, options.out(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[AUDIT] report: " + options.out());
        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(report));
    }

    private List<MlpValueModel> loadModels() throws Exception {
        String spec = System.getProperty("ares.agent.valueModel", "").trim();
        if (spec.isEmpty()) {
            throw new IllegalStateException("set -Dares.agent.valueModel=DIR[,DIR] to the model(s) to audit");
        }
        List<MlpValueModel> models = new ArrayList<>();
        for (String item : spec.split(",")) {
            if (!item.isBlank()) {
                models.add(MlpValueModel.load(Path.of(item.trim())));
            }
        }
        System.out.println("[AUDIT] " + models.size() + " value net(s) from " + spec);
        return models;
    }

    /** Every perturbation the audit knows about, with the sign the rules of the game imply. */
    private List<Perturbation> standardPerturbations(List<Integer> cards) {
        List<Perturbation> result = new ArrayList<>();
        result.add(new Perturbation("mc +10", "> 0", (game, player) -> player.setMc(player.getMc() + 10)));
        result.add(new Perturbation("mc -10", "< 0", (game, player) -> player.setMc(Math.max(0, player.getMc() - 10))));
        result.add(new Perturbation("plants +5", "> 0", (game, player) -> player.setPlants(player.getPlants() + 5)));
        result.add(new Perturbation("heat +5", "> 0", (game, player) -> player.setHeat(player.getHeat() + 5)));
        result.add(new Perturbation("tr +1", "> 0", (game, player) ->
                player.setTerraformingRating(player.getTerraformingRating() + 1)));
        result.add(new Perturbation("forests +1", "> 0", (game, player) -> player.setForests(player.getForests() + 1)));
        for (String field : List.of("mcIncome", "cardIncome", "plantsIncome", "heatIncome", "steelIncome", "titaniumIncome")) {
            result.add(new Perturbation("income " + field + " +1", "> 0", (game, player) -> income(player, field)));
        }
        result.add(new Perturbation("extraPoint +1", "> 0", (game, player) -> player.setExtraPoints(player.getExtraPoints() + 1)));
        // Negative controls: the same change on the OTHER side must move my score the other way. If these are positive,
        // the score is not reading the table correctly and every comparison built on it is meaningless.
        result.add(new Perturbation("opponent mc +10", "< 0", (game, player) -> {
            Player other = other(game, player);
            if (other != null) {
                other.setMc(other.getMc() + 10);
            }
        }));
        result.add(new Perturbation("opponent plants +5", "< 0", (game, player) -> {
            Player other = other(game, player);
            if (other != null) {
                other.setPlants(other.getPlants() + 5);
            }
        }));
        result.add(new Perturbation("opponent income plantsIncome +1", "< 0", (game, player) -> {
            Player other = other(game, player);
            if (other != null) {
                income(other, "plantsIncome");
            }
        }));
        result.add(new Perturbation("hand +1 card", ">= 0", (game, player) -> {
            if (!game.getProjectsDeck().getCards().isEmpty()) {
                player.getHand().getCards().add(game.getProjectsDeck().getCards().removeFirst());
            }
        }));
        for (int cardId : cards) {
            result.add(new Perturbation("played +" + cardId, "> 0", (game, player) -> {
                cardService.getCard(cardId); // throws if the id is not a card
                player.getPlayed().getCards().add(cardId);
            }));
            // The owner's test: ONE resource on a card can decide a game (one microbe enables a whole chain). The card is
            // added together with one unit of the resource it stores, which is what TableResourcesFeature encodes.
            result.add(new Perturbation("played +" + cardId + " +1 resource", "> 0", (game, player) -> {
                Card card = cardService.getCard(cardId);
                player.getPlayed().getCards().add(cardId);
                player.getCardResourcesCount().merge(card.getClass(), 1, Integer::sum);
            }));
        }
        return result;
    }

    /** The score the search compares candidates with, so the audit measures the same quantity the games use. */
    private float score(MarsGame game, String uuid, List<MlpValueModel> models) {
        Player me = game.getPlayerByUuid(uuid);
        Player other = other(game, me);
        float[] mine = observationBuilder.build(game, me);
        if (other == null) {
            float sum = 0f;
            for (MlpValueModel model : models) {
                sum += model.logit(mine);
            }
            return sum / models.size();
        }
        float[] theirs = observationBuilder.build(game, other);
        float sum = 0f;
        for (MlpValueModel model : models) {
            sum += (model.logit(mine) - model.logit(theirs)) * 0.5f;
        }
        return sum / models.size();
    }

    /** Simple names of the cards on this player's table. */
    private Set<String> playedCardNames(MarsGame game, String uuid) {
        Set<String> result = new java.util.HashSet<>();
        Player player = game.getPlayerByUuid(uuid);
        if (player == null) {
            return result;
        }
        for (Integer id : player.getPlayed().getCards()) {
            result.add(cardService.getCard(id).getClass().getSimpleName());
        }
        return result;
    }

    private static Player other(MarsGame game, Player me) {        for (Player candidate : game.getPlayerUuidToPlayer().values()) {
            if (!candidate.getUuid().equals(me.getUuid())) {
                return candidate;
            }
        }
        return null;
    }

    /** Player is a Lombok {@code @Data} bean: every income has a setter, no reflection needed. */
    private static void income(Player player, String field) {
        switch (field) {
            case "mcIncome" -> player.setMcIncome(player.getMcIncome() + 1);
            case "cardIncome" -> player.setCardIncome(player.getCardIncome() + 1);
            case "plantsIncome" -> player.setPlantsIncome(player.getPlantsIncome() + 1);
            case "heatIncome" -> player.setHeatIncome(player.getHeatIncome() + 1);
            case "steelIncome" -> player.setSteelIncome(player.getSteelIncome() + 1);
            case "titaniumIncome" -> player.setTitaniumIncome(player.getTitaniumIncome() + 1);
            default -> throw new IllegalArgumentException("unknown income " + field);
        }
    }

    /** Mean delta per round band: the timing curve of one perturbation. */
    private static Map<String, Object> byRound(List<Double> values, List<Integer> rounds) {
        if (values == null || values.isEmpty() || rounds == null || rounds.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        int[][] bands = {{0, 4}, {5, 8}, {9, 12}, {13, 16}, {17, 99}};
        for (int[] band : bands) {
            List<Double> selected = new ArrayList<>();
            for (int i = 0; i < Math.min(values.size(), rounds.size()); i++) {
                int round = rounds.get(i);
                if (round >= band[0] && round <= band[1]) {
                    selected.add(values.get(i));
                }
            }
            if (selected.size() >= 20) {
                result.put(band[0] + "-" + band[1], round(mean(selected), 4));
            }
        }
        return result;
    }

    /** What the captured positions look like: without this the deltas have no context (all-zero MC? round 2?). */
    private static Map<String, Object> summary(List<Capture> captures) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, java.util.function.ToIntFunction<Player>> readers = new LinkedHashMap<>();
        readers.put("mc", Player::getMc);
        readers.put("plants", Player::getPlants);
        readers.put("heat", Player::getHeat);
        readers.put("terraformingRating", Player::getTerraformingRating);
        readers.put("forests", Player::getForests);
        readers.put("mcIncome", Player::getMcIncome);
        readers.put("cardIncome", Player::getCardIncome);
        readers.put("plantsIncome", Player::getPlantsIncome);
        readers.put("steelIncome", Player::getSteelIncome);
        readers.put("titaniumIncome", Player::getTitaniumIncome);
        readers.put("cardsOnTable", capture -> capture.getPlayed().size());
        for (Map.Entry<String, java.util.function.ToIntFunction<Player>> entry : readers.entrySet()) {
            List<Double> values = new ArrayList<>();
            for (Capture capture : captures) {
                values.add((double) entry.getValue().applyAsInt(capture.game().getPlayerByUuid(capture.playerUuid())));
            }
            result.put(entry.getKey(), Map.of("mean", round(mean(values), 2), "median", round(median(values), 2)));
        }
        List<Double> rounds = new ArrayList<>();
        for (Capture capture : captures) {
            rounds.add((double) capture.round());
        }
        result.put("round", Map.of("mean", round(mean(rounds), 2), "median", round(median(rounds), 2)));
        return result;
    }

    private static double mean(List<Double> values) {        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private static double median(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compare);
        return sorted.isEmpty() ? 0 : sorted.get(sorted.size() / 2);
    }

    private static double round(double value, int digits) {
        double factor = Math.pow(10, digits);
        return Math.round(value * factor) / factor;
    }

    /** Captures the position at the start of a turn, for both seats, a few times per game. */
    private static final class CaptureHook implements DecisionProbe.Hook {
        private final List<Capture> captures;
        private final int perGame;
        private final java.util.Set<String> taken = new java.util.HashSet<>();
        private int kept;

        CaptureHook(List<Capture> captures, int perGame) {
            this.captures = captures;
            this.perGame = perGame;
        }

        @Override
        public void turnStarted(MarsGame game, Player player) {
            // A round has several turns (one per chosen phase), so "every N-th turn" would still stay inside one round.
            // One capture per selected round, every 4th round, spreads the sample over the whole game.
            int round = game.getTurns();
            if (kept >= perGame || round < 4 || round % 4 != 0 || !taken.add(round + "/" + player.getUuid())) {
                return;
            }
            kept++;
            captures.add(new Capture(player.getUuid(), new MarsGame(game), round));
        }

        @Override
        public int beforeChoice(Decision decision, MarsGame game, Player player) {
            return -1;
        }

        @Override
        public void afterChoice(Decision decision, MarsGame game, Player player, int index) {
        }
    }
}
