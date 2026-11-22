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
        System.setProperty("ares.agent.search.types", "BLUE_INPUT");
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
