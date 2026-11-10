package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.GlobalParameter;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rule-level invariants that must hold at every step of every game. A violation means the engine (card effect, payment,
 * production, ...) produced an impossible state - these are "silent" bugs that never throw an exception.
 */
public class GameInvariants {
    private static final int DUMMY_CARD_ID_START = 20000; // AiConstants dummy cards

    private final CardService cardService;
    private Set<Integer> cardUniverse;
    private final Map<String, Integer> lastTerraformingRating = new HashMap<>();
    // some cards deliberately cost TR (Lunar Beam, Strip Mine, Investment Loan, ...): count those losses
    private int terraformingLost;
    private final Map<String, Set<Integer>> previousPlayed = new HashMap<>();
    private final Map<String, String> previousSummary = new HashMap<>();

    public GameInvariants(CardService cardService) {
        this.cardService = cardService;
    }

    public List<String> check(MarsGame game) {
        List<String> violations = new ArrayList<>();

        // 1. every real card exists at most once among deck / hands / played, and no card appears out of nowhere
        Map<Integer, String> location = new HashMap<>();
        collect(location, violations, game.getProjectsDeck().getCards(), "deck");
        for (Player p : game.getPlayerUuidToPlayer().values()) {
            collect(location, violations, p.getHand().getCards(), "hand:" + p.getName());
            collect(location, violations, p.getPlayed().getCards(), "played:" + p.getName());
        }
        if (cardUniverse == null) {
            cardUniverse = new HashSet<>(cardService.createProjectsDeck(game.getExpansions()).getCards());
            cardUniverse.addAll(cardService.createCorporationsDeck(new HashSet<>(game.getExpansions())).getCards());
        }
        for (Integer id : location.keySet()) {
            if (!cardUniverse.contains(id)) {
                violations.add("CARD_FROM_NOWHERE card " + name(id) + " is in " + location.get(id) + " but is not part of the projects deck of this game");
            }
        }

        int terraformingProgress = game.getPlanet().getCurrentLevel(GlobalParameter.TEMPERATURE)
                + game.getPlanet().getCurrentLevel(GlobalParameter.OXYGEN)
                + game.getPlanet().oceansBuilt()
                + infrastructureProgress(game);
        int totalTrGained = 0;

        for (Player p : game.getPlayerUuidToPlayer().values()) {
            String who = p.getName();
            Set<Integer> playedNow = new HashSet<>(p.getPlayed().getCards());
            Set<Integer> newlyPlayed = new HashSet<>(playedNow);
            newlyPlayed.removeAll(previousPlayed.getOrDefault(p.getUuid(), Set.of()));
            previousPlayed.put(p.getUuid(), playedNow);
            String context = " {newly played: " + newlyPlayed.stream().map(this::name).toList() + ", phase " + p.getChosenPhase()
                    + ", before: " + previousSummary.get(p.getUuid()) + "}";
            previousSummary.put(p.getUuid(), "mc=" + p.getMc() + " heat=" + p.getHeat() + " plants=" + p.getPlants());
            int before = violations.size();
            // 2. no negative stockpiles / incomes that can not be negative
            neg(violations, who, "mc", p.getMc());
            neg(violations, who, "heat", p.getHeat());
            neg(violations, who, "plants", p.getPlants());
            neg(violations, who, "cardIncome", p.getCardIncome());
            neg(violations, who, "heatIncome", p.getHeatIncome());
            neg(violations, who, "plantsIncome", p.getPlantsIncome());
            neg(violations, who, "steelIncome", p.getSteelIncome());
            neg(violations, who, "titaniumIncome", p.getTitaniumIncome());
            neg(violations, who, "forests", p.getForests());
            if (p.getMcIncome() < -5) {
                violations.add("MC_INCOME_TOO_LOW " + who + " mcIncome=" + p.getMcIncome());
            }

            for (int i = before; i < violations.size(); i++) {
                violations.set(i, violations.get(i) + context);
            }
            // 3. resources live only on played cards, and are non-negative
            Set<Class<?>> playedClasses = new HashSet<>();
            for (Integer id : p.getPlayed().getCards()) {
                playedClasses.add(cardService.getCard(id).getClass());
            }
            for (Map.Entry<Class<?>, Integer> e : p.getCardResourcesCount().entrySet()) {
                if (e.getValue() < 0) {
                    violations.add("NEGATIVE_CARD_RESOURCES " + who + " " + e.getKey().getSimpleName() + "=" + e.getValue());
                }
                if (e.getValue() > 0 && !playedClasses.contains(e.getKey())) {
                    violations.add("RESOURCES_ON_UNPLAYED_CARD " + who + " " + e.getKey().getSimpleName() + "=" + e.getValue());
                }
            }

            // 4. activated blue cards are played cards
            for (Integer id : p.getActivatedBlueCards().getCards()) {
                if (!p.getPlayed().containsCard(id)) {
                    violations.add("ACTIVATED_NOT_PLAYED " + who + " " + name(id));
                }
            }

            // 5. remember TR changes to account for it below
            int tr = p.getTerraformingRating();
            totalTrGained += tr - Constants.STARTING_RT;
            Integer previous = lastTerraformingRating.put(p.getUuid(), tr);
            if (previous != null && tr < previous) {
                terraformingLost += previous - tr;
            }
        }

        // 6. every raised global parameter is credited to somebody's TR
        if (!game.isCrysis() && totalTrGained + terraformingLost < terraformingProgress) {
            violations.add("FREE_TERRAFORMING progress=" + terraformingProgress + " (temperature+oxygen+oceans+infrastructure) but players gained only " + totalTrGained + " net TR (+" + terraformingLost + " lost on purpose)");
        }

        return violations;
    }

    private static int infrastructureProgress(MarsGame game) {
        try {
            return game.getPlanet().getCurrentLevel(GlobalParameter.INFRASTRUCTURE);
        } catch (NullPointerException e) {
            return 0; // infrastructure is not part of this game
        }
    }

    private void collect(Map<Integer, String> location, List<String> violations, List<Integer> cards, String where) {
        for (Integer id : cards) {
            if (id >= DUMMY_CARD_ID_START) {
                continue;
            }
            String previous = location.put(id, where);
            if (previous != null) {
                violations.add("DUPLICATE_CARD card " + name(id) + " is in both " + previous + " and " + where);
            }
        }
    }

    private String name(int id) {
        try {
            Card card = cardService.getCard(id);
            return id + "/" + card.getClass().getSimpleName();
        } catch (RuntimeException e) {
            return String.valueOf(id);
        }
    }

    private static void neg(List<String> violations, String who, String field, int value) {
        if (value < 0) {
            violations.add("NEGATIVE_" + field.toUpperCase() + " " + who + "=" + value);
        }
    }
}
