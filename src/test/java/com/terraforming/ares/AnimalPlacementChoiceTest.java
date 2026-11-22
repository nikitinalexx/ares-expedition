package com.terraforming.ares;

import com.terraforming.ares.cards.blue.ConservedBiome;
import com.terraforming.ares.cards.blue.EcologicalZone;
import com.terraforming.ares.cards.blue.FilterFeeders;
import com.terraforming.ares.cards.blue.Fish;
import com.terraforming.ares.cards.blue.Herbivores;
import com.terraforming.ares.cards.blue.SmallAnimals;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.agent.BlueInputDecisions;
import com.terraforming.ares.services.agent.OnePlySearch;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The owner's sanity check (22/11): with ONE animal to place and every holder at zero animals, Fish is the only card that
 * pays now (1 point per animal; EcologicalZone, FilterFeeders, Herbivores and SmallAnimals pay 0.5, so they need two).
 * The question is the DECISION, not the win rate: given the five holders on the table, where does the player put it?
 * <p>
 * This asks the search directly ({@code rankBlueSlot} ranks the candidates by V(afterstate) with the deployed critics), so
 * it is deterministic and stays as a regression test. It needs the value models, which is why the test pins them.
 */
@org.springframework.boot.test.context.SpringBootTest(
        classes = TerraformingAresApplication.class,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration",
                "spring.main.web-application-type=none"
        })
class AnimalPlacementChoiceTest extends EngineTestBase {

    static {
        // OnePlySearch reads these through System.getProperty in its constructor, which runs when the context creates the
        // bean - a static initialiser is the only place that is early enough.
        System.setProperty("ares.agent.search", "oneply");
        System.setProperty("ares.agent.search.types", "BLUE_INPUT,STANDARD_ACTION");
        System.setProperty("ares.agent.valueModel",
                "D:/ares/runs/value/v_1118-2332_17_s1,D:/ares/runs/value/v_1118-2332_17_s2,"
                        + "D:/ares/runs/value/v_1118-2332_17_s3");
    }

    @Autowired
    GameService gameService;
    @Autowired
    CardService cardService;
    @Autowired
    MarsContextProvider marsContextProvider;
    @Autowired
    BlueInputDecisions blueInputDecisions;
    @Autowired
    OnePlySearch search;
    @Autowired
    com.terraforming.ares.services.TerraformingService terraformingService;
    @Autowired
    com.terraforming.ares.services.agent.StandardActionDecisions standardActionDecisions;
    @Autowired
    com.terraforming.ares.services.ai.turnProcessors.AiTurnService aiTurnService;
    @Autowired
    com.terraforming.ares.services.agent.ObservationBuilder observationBuilder;

    private Card byClass(Class<? extends Card> type) {
        for (int id = 0; id < 2000; id++) {
            Card card = cardService.getCard(id);
            if (card != null && card.getClass() == type) {
                return card;
            }
        }
        throw new IllegalStateException("no card of " + type.getSimpleName());
    }

    @Test
    void one_animal_and_every_counter_at_zero_goes_to_fish() {
        MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT))
                .mulligan(true)
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                .dummyHand(true).build());
        game.setStateType(StateType.PERFORM_BLUE_ACTION, marsContextProvider.provide(game));
        Player me = game.getPlayerUuidToPlayer().values().stream()
                .filter(p -> p.getName().equals("A")).findFirst().orElseThrow();

        Card biome = byClass(ConservedBiome.class);
        me.getPlayed().addCard(biome.getId());
        me.getCardResourcesCount().put(ConservedBiome.class, 0);
        List<Card> holders = List.of(byClass(Fish.class), byClass(EcologicalZone.class), byClass(FilterFeeders.class),
                byClass(Herbivores.class), byClass(SmallAnimals.class));
        for (Card holder : holders) {
            me.getPlayed().addCard(holder.getId());
            me.getCardResourcesCount().put(holder.getClass(), 0);
        }

        var slot = blueInputDecisions.nextSlot(game, me, biome, new HashMap<>());
        assertTrue(slot.isPresent(), "ConservedBiome asks where the animal goes");
        var choices = slot.get().choices();
        assertTrue(choices.size() >= 2, "more than one holder is offered: " + choices);
        List<String> offered = choices.stream().map(c -> cardService.getCard(c.id()).getClass().getSimpleName()).toList();
        System.out.println("[SANITY] offered: " + offered);

        int index = search.rankBlueSlot(game, me, biome, choices, new HashMap<>());
        org.junit.jupiter.api.Assumptions.assumeTrue(index >= 0,
                "the deployed value models are not available here; run with -DargLine=\"-Dares.agent.search=oneply "
                        + "-Dares.agent.search.types=BLUE_INPUT -Dares.agent.valueModel=<critics>\"");
        Card taken = cardService.getCard(choices.get(index).id());
        System.out.println("[SANITY] took: " + taken.getClass().getSimpleName());
        assertEquals(Fish.class, taken.getClass(),
                "with every counter at zero only Fish pays: offered " + offered + ", took " + taken.getClass().getSimpleName());
    }

    @Test
    void one_ocean_from_the_end_and_ahead_it_finishes_the_game() throws Exception {
        // The owner's scenario (22/11): the parameters are one ocean from the maximum and the player is ahead, so buying
        // that ocean ends the game NOW and wins it. Playing on for a bigger margin risks the opponent's last round. The
        // search must pick the ocean, not stop and not a cheaper action.
        MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT))
                .mulligan(true)
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                .dummyHand(true).build());
        Player me = game.getPlayerUuidToPlayer().values().stream()
                .filter(p -> p.getName().equals("A")).findFirst().orElseThrow();
        Player other = game.getPlayerUuidToPlayer().values().stream()
                .filter(p -> p.getName().equals("B")).findFirst().orElseThrow();
        me.setMc(300);
        other.setMc(0);
        me.setTerraformingRating(40);
        other.setTerraformingRating(0);
        game.setStateType(StateType.BUILD_GREEN_PROJECTS, marsContextProvider.provide(game));

        var context = marsContextProvider.provide(game, me);
        while (game.getPlanet().temperatureLeft() > 0) {
            terraformingService.increaseTemperature(context);
        }
        while (game.getPlanet().oxygenLeft() > 0) {
            terraformingService.raiseOxygen(context);
        }
        while (game.getPlanet().oceansLeft() > 1) {
            terraformingService.revealOcean(context);
        }
        // keep the phase snapshot in sync with the constructed planet, or the engine's own canX checks read the old one
        java.lang.reflect.Field snapshot = MarsGame.class.getDeclaredField("planetAtTheStartOfThePhase");
        snapshot.setAccessible(true);
        snapshot.set(game, new com.terraforming.ares.model.Planet(game.getPlanet()));
        System.out.println("[SANITY] oceansLeft " + game.getPlanet().oceansLeft()
                + ", temperatureLeft " + game.getPlanet().temperatureLeft()
                + ", oxygenLeft " + game.getPlanet().oxygenLeft());

        var choices = standardActionDecisions.options(game, me,
                List.of(com.terraforming.ares.model.turn.TurnType.STANDARD_PROJECT));
        List<Integer> kinds = choices.stream().map(com.terraforming.ares.services.agent.StandardActionChoice::kind).toList();
        System.out.println("[SANITY] standard actions offered: " + kinds);
        // why the search may skip every candidate: apply the ending action to a copy and print what the engine says
        try {
            var copy = new MarsGame(game);
            var meCopy = copy.getPlayerByUuid(me.getUuid());
            aiTurnService.standardProjectTurn(copy, meCopy, com.terraforming.ares.model.StandardProjectType.OCEAN);
            System.out.println("[SANITY] SP_OCEAN applied on a copy: ended=" + copy.gameEndCondition());
            try {
                observationBuilder.build(copy, meCopy);
                System.out.println("[SANITY] observation on the ended game: ok");
            } catch (RuntimeException e) {
                System.out.println("[SANITY] observation on the ended game FAILED: " + e);
            }
        } catch (RuntimeException e) {
            System.out.println("[SANITY] SP_OCEAN failed on a copy: " + e);
        }
        var decision = standardActionDecisions.decision(game, me, choices);
        int index = search.choose(decision, game, me);
        try {
            java.lang.reflect.Field models = OnePlySearch.class.getDeclaredField("models");
            models.setAccessible(true);
            java.lang.reflect.Field types = OnePlySearch.class.getDeclaredField("types");
            types.setAccessible(true);
            System.out.println("[SANITY] enabled=" + search.enabled() + " models=" + ((List<?>) models.get(search)).size() + " types=" + types.get(search) + " index=" + index);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        org.junit.jupiter.api.Assumptions.assumeTrue(index >= 0,
                "the deployed value models are not available here; see the class javadoc for the argLine");
        int chosen = choices.get(index).kind();
        System.out.println("[SANITY] took kind " + chosen + " (SP_OCEAN=" + com.terraforming.ares.services.agent.StandardActionDecisions.SP_OCEAN + ")");
        assertEquals(com.terraforming.ares.services.agent.StandardActionDecisions.SP_OCEAN, chosen,
                "one ocean ends the game and the player is ahead: offered " + kinds);
    }

    @Test
    void without_fish_the_animal_completes_an_odd_counter() {
        // Herbivores at 1 animal pays its next one; SmallAnimals at 0 pays nothing for the first. A player that counts
        // marginal points takes Herbivores; one that takes the "first" or the "biggest" card does not.
        MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT))
                .mulligan(true)
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                .dummyHand(true).build());
        game.setStateType(StateType.PERFORM_BLUE_ACTION, marsContextProvider.provide(game));
        Player me = game.getPlayerUuidToPlayer().values().stream()
                .filter(p -> p.getName().equals("A")).findFirst().orElseThrow();

        Card biome = byClass(ConservedBiome.class);
        me.getPlayed().addCard(biome.getId());
        me.getCardResourcesCount().put(ConservedBiome.class, 0);
        Card herbivores = byClass(Herbivores.class);
        Card smallAnimals = byClass(SmallAnimals.class);
        for (Card holder : List.of(herbivores, smallAnimals)) {
            me.getPlayed().addCard(holder.getId());
        }
        me.getCardResourcesCount().put(Herbivores.class, 1);
        me.getCardResourcesCount().put(SmallAnimals.class, 0);

        var slot = blueInputDecisions.nextSlot(game, me, biome, new HashMap<>());
        assertTrue(slot.isPresent(), "ConservedBiome asks where the animal goes");
        var choices = slot.get().choices();
        List<String> offered = choices.stream().map(c -> cardService.getCard(c.id()).getClass().getSimpleName()).toList();
        int index = search.rankBlueSlot(game, me, biome, choices, new HashMap<>());
        org.junit.jupiter.api.Assumptions.assumeTrue(index >= 0,
                "the deployed value models are not available here; see the class javadoc for the argLine");
        Card taken = cardService.getCard(choices.get(index).id());
        System.out.println("[SANITY] offered " + offered + ", took " + taken.getClass().getSimpleName());
        assertEquals(Herbivores.class, taken.getClass(),
                "Herbivores' next animal pays a point, SmallAnimals' first pays nothing: offered " + offered);
    }
}







