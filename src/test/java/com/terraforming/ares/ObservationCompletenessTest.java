package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildDto;
import com.terraforming.ares.model.BuildType;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.GlobalParameter;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.model.parameters.MeasurableGlobalParameter;
import com.terraforming.ares.model.parameters.Ocean;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.agent.PickPhaseDecisions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "The network can only learn what it is told": every PUBLIC fact about the game must change the observation, and no hidden fact
 * may. Each fact is changed on a copy of a real mid-game state, from the point of view of both players.
 * <p>
 * Facts the observation does not show yet are listed in {@link #KNOWN_BLIND_SPOTS} - this list is the to-do list of the encoder.
 * The test fails when a NEW blind spot appears (or a known one is fixed and must be removed from the list).
 */
class ObservationCompletenessTest extends EngineTestBase {
    /**
     * Facts that are NOT visible in the observation yet. Fixing the encoder removes lines from here.
     */
    static final List<String> KNOWN_BLIND_SPOTS = List.of();

    /**
     * While the phase is being picked the choice of THIS round is hidden by the rules (simultaneous pick); nothing else may be hidden.
     */
    static final List<String> HIDDEN_BY_RULE_WHILE_PICKING = List.of("my phase chosen THIS round", "opponent's phase chosen THIS round");

    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;
    @Autowired
    PickPhaseDecisions decisions;
    @Autowired
    CardService cardService;

    /**
     * A copy of a real game at the first moment (round 12 or later) when `wanted` is true for its state.
     */
    private MarsGame snapshot(java.util.function.Predicate<MarsGame> wanted) {
        for (int attempt = 0; attempt < 30; attempt++) {
            MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                    .playerNames(new ArrayList<>(List.of("A", "B")))
                    .computers(List.of(PlayerDifficulty.SMART, PlayerDifficulty.SMART))
                    .mulligan(true)
                    .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                    .dummyHand(true).build());
            MarsGame[] snapshot = new MarsGame[1];
            simulation.processSimulation(game, current -> {
                if (snapshot[0] == null && current.getTurns() >= 12 && wanted.test(current)) {
                    snapshot[0] = new MarsGame(current);
                }
            });
            if (snapshot[0] != null) {
                return snapshot[0];
            }
        }
        throw new IllegalStateException("no such state found in 30 games");
    }

    private MarsGame whilePicking() {
        return snapshot(g -> g.getStateType() == com.terraforming.ares.model.StateType.PICK_PHASE);
    }

    private MarsGame insideARound() {
        return snapshot(g -> g.getStateType() == com.terraforming.ares.model.StateType.BUILD_GREEN_PROJECTS
                || g.getStateType() == com.terraforming.ares.model.StateType.PERFORM_BLUE_ACTION
                || g.getStateType() == com.terraforming.ares.model.StateType.DRAFT_CARDS);
    }

    private float[] observe(MarsGame game, String uuid) {
        return decisions.build(game, game.getPlayerByUuid(uuid)).observation();
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static MeasurableGlobalParameter parameter(MarsGame game, GlobalParameter which) {
        try {
            Field f = game.getPlanet().getClass().getDeclaredField("measurableGlobalParameters");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<GlobalParameter, MeasurableGlobalParameter> map = (Map<GlobalParameter, MeasurableGlobalParameter>) f.get(game.getPlanet());
            return map.get(which);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * (game, player) -> changes one fact of that player (or of the game when the player is irrelevant).
     */
    private Map<String, BiConsumer<MarsGame, Player>> playerFacts() {
        Map<String, BiConsumer<MarsGame, Player>> facts = new LinkedHashMap<>();
        facts.put("mc", (g, p) -> p.setMc(p.getMc() + 7));
        facts.put("heat", (g, p) -> p.setHeat(p.getHeat() + 5));
        facts.put("plants", (g, p) -> p.setPlants(p.getPlants() + 4));
        facts.put("mcIncome", (g, p) -> p.setMcIncome(p.getMcIncome() + 2));
        facts.put("heatIncome", (g, p) -> p.setHeatIncome(p.getHeatIncome() + 2));
        facts.put("plantsIncome", (g, p) -> p.setPlantsIncome(p.getPlantsIncome() + 2));
        facts.put("steelIncome", (g, p) -> p.setSteelIncome(p.getSteelIncome() + 2));
        facts.put("titaniumIncome", (g, p) -> p.setTitaniumIncome(p.getTitaniumIncome() + 2));
        facts.put("cardIncome", (g, p) -> p.setCardIncome(p.getCardIncome() + 1));
        facts.put("terraformingRating", (g, p) -> p.setTerraformingRating(p.getTerraformingRating() + 3));
        facts.put("forests", (g, p) -> p.setForests(p.getForests() + 2));
        facts.put("extraPoints", (g, p) -> p.setExtraPoints(p.getExtraPoints() + 5));
        for (int phase = 1; phase <= 5; phase++) {
            int index = phase - 1;
            facts.put("phase upgrade of phase " + phase, (g, p) -> p.getPhaseCards().set(index, p.getPhaseCards().get(index) == 1 ? 2 : 1));
        }
        facts.put("phase chosen THIS round", (g, p) -> p.setChosenPhase(p.getChosenPhase() == 3 ? 4 : 3));
        facts.put("phase chosen LAST round", (g, p) -> p.setPreviousChosenPhase(p.getPreviousChosenPhase() == null || p.getPreviousChosenPhase() != 2 ? 2 : 4));
        facts.put("a build available (e.g. 2nd green build)", (g, p) -> p.getBuilds().add(new BuildDto(BuildType.GREEN)));
        facts.put("extra activations of blue cards left", (g, p) -> p.setBlueActionExtraActivationsLeft(p.getBlueActionExtraActivationsLeft() + 1));
        facts.put("a card is in the hand", (g, p) -> p.getHand().addCard(unusedCard(g, CardColor.GREEN)));
        facts.put("a green card is on the table", (g, p) -> putOnTable(p, unusedCard(g, CardColor.GREEN)));
        facts.put("a blue card is on the table", (g, p) -> putOnTable(p, unusedCard(g, CardColor.BLUE)));
        facts.put("a red card is on the table", (g, p) -> putOnTable(p, unusedCard(g, CardColor.RED)));
        facts.put("a blue card was activated this round", (g, p) -> {
            int blue = unusedCard(g, CardColor.BLUE);
            putOnTable(p, blue);
            p.getActivatedBlueCards().addCard(blue);
        });
        facts.put("resources (animals/microbes) on a played card", (g, p) -> {
            // a card that really keeps animals or microbes: put it on the table and add resources to it
            int id = unusedCardWithTag(g, com.terraforming.ares.model.Tag.ANIMAL);
            putOnTable(p, id);
            p.getCardResourcesCount().merge(cardService.getCard(id).getClass(), 3, Integer::sum);
        });
        facts.put("lunaProjectOffice", (g, p) -> p.setLunaProjectOffice(p.getLunaProjectOffice() + 1));
        facts.put("projectInspection", (g, p) -> p.setProjectInspection(p.getProjectInspection() + 1));
        return facts;
    }

    /**
     * A card of the colour that nobody holds or has played (the first one that is a good example: it has tags and a price).
     */
    /**
     * Mimic post-build bookkeeping the V2 encoder assumes (Research Grant tags, resource maps, …).
     */
    private void putOnTable(Player player, int cardId) {
        Card card = cardService.getCard(cardId);
        player.getPlayed().addCard(cardId);
        if (card.getCardMetadata() != null
                && card.getCardMetadata().getCardAction() == com.terraforming.ares.model.CardAction.RESEARCH_GRANT) {
            player.getCardToTag().putIfAbsent(card.getClass(),
                    new java.util.ArrayList<>(java.util.List.of(
                            com.terraforming.ares.model.Tag.DYNAMIC,
                            com.terraforming.ares.model.Tag.DYNAMIC,
                            com.terraforming.ares.model.Tag.DYNAMIC)));
        }
        if (card.getCollectableResource() != null) {
            player.getCardResourcesCount().putIfAbsent(card.getClass(), 0);
        }
    }

    private int unusedCard(MarsGame game, CardColor color) {
        java.util.Set<Integer> taken = new java.util.HashSet<>();
        game.getPlayerUuidToPlayer().values().forEach(p -> {
            taken.addAll(p.getHand().getCards());
            taken.addAll(p.getPlayed().getCards());
        });
        for (int id : cardService.createProjectsDeck(game.getExpansions()).getCards()) {
            Card card = cardService.getCard(id);
            if (card.getColor() == color && !taken.contains(id) && card.getTags() != null && !card.getTags().isEmpty()) {
                return id;
            }
        }
        throw new IllegalStateException("no free " + color + " card");
    }

    private int unusedCardWithTag(MarsGame game, com.terraforming.ares.model.Tag tag) {
        java.util.Set<Integer> taken = new java.util.HashSet<>();
        game.getPlayerUuidToPlayer().values().forEach(p -> {
            taken.addAll(p.getHand().getCards());
            taken.addAll(p.getPlayed().getCards());
        });
        for (int id : cardService.createProjectsDeck(game.getExpansions()).getCards()) {
            Card card = cardService.getCard(id);
            if (!taken.contains(id) && card.getTags() != null && card.getTags().contains(tag)) {
                return id;
            }
        }
        throw new IllegalStateException("no free card with the tag " + tag);
    }

    private Map<String, java.util.function.Consumer<MarsGame>> worldFacts() {
        Map<String, java.util.function.Consumer<MarsGame>> facts = new LinkedHashMap<>();
        facts.put("temperature", g -> parameter(g, GlobalParameter.TEMPERATURE).increase());
        facts.put("oxygen", g -> parameter(g, GlobalParameter.OXYGEN).increase());
        facts.put("an ocean is revealed", g -> g.getPlanet().getOceans().stream().filter(o -> !o.isRevealed()).findFirst().ifPresent(Ocean::reveal));
        facts.put("a milestone is claimed by a player", g -> {
            Player first = g.getPlayerUuidToPlayer().values().iterator().next();
            g.getMilestones().stream().filter(m -> m.getAchievedByPlayers().isEmpty()).findFirst()
                    .ifPresent(m -> m.getAchievedByPlayers().add(first.getUuid()));
        });
        facts.put("round number", g -> setField(g, "turns", g.getTurns() + 3));
        return facts;
    }

    @Test
    void whilePickingOnlyThePicksOfTheRoundAreHidden() {
        List<String> invisible = invisibleFacts(whilePicking());
        List<String> unexpected = new ArrayList<>(invisible);
        unexpected.removeAll(HIDDEN_BY_RULE_WHILE_PICKING);
        unexpected.removeAll(KNOWN_BLIND_SPOTS);
        assertTrue(unexpected.isEmpty(), "The observation does not show (while picking a phase): " + unexpected);
        assertTrue(invisible.containsAll(HIDDEN_BY_RULE_WHILE_PICKING),
                "The picks of the current round must be hidden while the phase is being picked, but they changed the observation");
    }

    @Test
    void insideARoundEverythingIsVisibleIncludingTheirPicks() {
        List<String> invisible = invisibleFacts(insideARound());
        invisible.removeAll(KNOWN_BLIND_SPOTS);
        assertTrue(invisible.isEmpty(), "The observation does not show (inside a round): " + invisible);
    }

    /**
     * Facts that change nothing in the observation of the given state, from the point of view of both players.
     */
    private List<String> invisibleFacts(MarsGame base) {
        List<String> invisible = new ArrayList<>();
        for (Map.Entry<String, BiConsumer<MarsGame, Player>> fact : playerFacts().entrySet()) {
            for (int observerIndex = 0; observerIndex < 2; observerIndex++) {
                for (int ownerIndex = 0; ownerIndex < 2; ownerIndex++) {
                    MarsGame game = new MarsGame(base);
                    List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
                    players.sort((a, b) -> a.getName().compareTo(b.getName()));
                    Player observer = players.get(observerIndex);
                    Player owner = players.get(ownerIndex);
                    float[] before = observe(game, observer.getUuid());
                    fact.getValue().accept(game, owner);
                    float[] after = observe(game, observer.getUuid());
                    if (Arrays.equals(before, after)) {
                        invisible.add((owner == observer ? "my " : "opponent's ") + fact.getKey());
                    }
                }
            }
        }
        for (Map.Entry<String, java.util.function.Consumer<MarsGame>> fact : worldFacts().entrySet()) {
            MarsGame game = new MarsGame(base);
            Player observer = game.getPlayerUuidToPlayer().values().iterator().next();
            float[] before = observe(game, observer.getUuid());
            fact.getValue().accept(game);
            if (Arrays.equals(before, observe(game, observer.getUuid()))) {
                invisible.add("world: " + fact.getKey());
            }
        }
        invisible.sort(String::compareTo);
        return new ArrayList<>(new java.util.LinkedHashSet<>(invisible));
    }
}
