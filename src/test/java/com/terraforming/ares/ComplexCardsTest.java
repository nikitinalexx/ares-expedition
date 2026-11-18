package com.terraforming.ares;

import com.terraforming.ares.cards.blue.FilterFeeders;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildDto;
import com.terraforming.ares.model.BuildType;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.payments.AnaerobicMicroorganismsPayment;
import com.terraforming.ares.model.payments.RestructuredResourcesPayment;
import com.terraforming.ares.services.CardResourceService;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.agent.BuildInputDecisions;
import com.terraforming.ares.services.agent.BuildPaymentChoice;
import com.terraforming.ares.services.agent.BuildPaymentDecisions;
import com.terraforming.ares.services.agent.BlueInputDecisions;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cards whose build/blue input is not independent per slot, and the two rules the owner changed or flagged. These are
 * engine-mechanics tests, not strategy tests: they say "the input menu is what the card says it is" and "the discount is
 * worth what the card says", which is what every ranking above them depends on.
 */
class ComplexCardsTest extends EngineTestBase {

    @Autowired
    GameService gameService;
    @Autowired
    MarsContextProvider marsContextProvider;
    @Autowired
    CardService cardService;
    @Autowired
    CardResourceService cardResourceService;
    @Autowired
    WinPointsService winPointsService;
    @Autowired
    BuildInputDecisions buildInputDecisions;
    @Autowired
    BuildPaymentDecisions buildPaymentDecisions;
    @Autowired
    BlueInputDecisions blueInputDecisions;
    @Autowired
    AiTurnService aiTurnService;

    /** Filter Feeders: one animal per addition of microbes to ANOTHER card, and 1 VP per 2 animals. */
    @Test
    void filterFeedersTakesOneAnimalPerMicrobeAdditionAndScoresOnePerTwo() {
        MarsGame game = phaseOne();
        Player player = named(game, "A");
        Card filterFeeders = cardService.getCard(217);
        assertEquals(FilterFeeders.class, filterFeeders.getClass(), "217 must be the single Filter Feeders");
        player.getPlayed().addCard(filterFeeders.getId());
        player.getCardResourcesCount().put(FilterFeeders.class, 0);

        Card microbeCard = firstWithResource(CardCollectableResource.MICROBE, filterFeeders.getId());
        player.getPlayed().addCard(microbeCard.getId());
        player.getCardResourcesCount().put(microbeCard.getClass(), 0);

        cardResourceService.addResources(player, microbeCard, 3);
        assertEquals(1, player.getCardResourcesCount().get(FilterFeeders.class).intValue(),
                "any number of microbes added at once is one animal");

        player.getCardResourcesCount().put(FilterFeeders.class, 4);
        int withFour = winPointsService.countWinPoints(player, game);
        player.getCardResourcesCount().put(FilterFeeders.class, 0);
        int withNone = winPointsService.countWinPoints(player, game);
        assertEquals(2, withFour - withNone, "4 animals are 2 victory points at 1 VP per 2");
    }

    /** Decomposers: the "remove a microbe to draw" option exists only while a microbe will be on the card. */
    @Test
    void decomposersOffersTheCardOnlyWhenAMicrobeWillBeOnIt() {
        MarsGame game = phaseOne();
        Player player = named(game, "A");
        Card decomposers = byAction(CardAction.DECOMPOSERS);
        player.getPlayed().addCard(decomposers.getId());
        player.getCardResourcesCount().put(decomposers.getClass(), 0);

        Card built = firstWithTag(Tag.PLANT);
        player.getCardResourcesCount().put(decomposers.getClass(), 0);

        var empty = buildInputDecisions.nextSlot(game, player, built, new HashMap<>());
        assertTrue(empty.isPresent(), "a plant card fires Decomposers");
        assertTrue(empty.get().choices().stream().anyMatch(c -> c.kind() == BuildInputDecisions.KIND_DECOMP_MICROBE));
        assertFalse(empty.get().choices().stream().anyMatch(c -> c.kind() == BuildInputDecisions.KIND_DECOMP_CARD),
                "with no microbe on the card, taking one to draw is illegal");

        player.getCardResourcesCount().put(decomposers.getClass(), 1);
        var withMicrobe = buildInputDecisions.nextSlot(game, player, built, new HashMap<>());
        assertTrue(withMicrobe.isPresent());
        assertTrue(withMicrobe.get().choices().stream().anyMatch(c -> c.kind() == BuildInputDecisions.KIND_DECOMP_CARD),
                "with a microbe on the card, the card may be taken");
    }

    /**
     * Viral Enhancers may put its resource on the card that is being built: the engine calls
     * {@code resourceCards(..., includeBuilding = true)}, and the card's own text does not say "another card" (Filter
     * Feeders' does, which is why that one says ANOTHER). This test pins the engine behaviour so a later change is noticed.
     */
    @Test
    void viralEnhancersMayTargetTheCardBeingBuilt() {
        MarsGame game = phaseOne();
        Player player = named(game, "A");
        Card viral = byAction(CardAction.VIRAL_ENHANCERS);
        player.getPlayed().addCard(viral.getId());

        Card built = firstWithTag(Tag.ANIMAL);
        Card target = firstWithResource(CardCollectableResource.MICROBE, built.getId());
        player.getPlayed().addCard(target.getId());
        player.getCardResourcesCount().put(target.getClass(), 0);

        var slot = buildInputDecisions.nextSlot(game, player, built, new HashMap<>());
        assertTrue(slot.isPresent());
        assertTrue(slot.get().choices().stream().anyMatch(c -> c.id() == target.getId()), "another card is a target");
        assertTrue(slot.get().choices().stream().anyMatch(c -> c.id() == built.getId()),
                "the card being built is a legal target for Viral Enhancers");
    }

    /** Mars University cannot discard the card that is being built, and always offers the stop. */
    @Test
    void marsUniversityCannotDiscardTheCardBeingBuilt() {
        MarsGame game = phaseOne();
        Player player = named(game, "A");
        Card university = byAction(CardAction.MARS_UNIVERSITY);
        player.getPlayed().addCard(university.getId());

        Card built = firstWithTag(Tag.SCIENCE);
        Card other = firstWithTag(Tag.PLANT);
        player.getHand().getCards().clear();
        player.getHand().addCards(List.of(built.getId(), other.getId()));

        var slot = buildInputDecisions.nextSlot(game, player, built, new HashMap<>());
        assertTrue(slot.isPresent());
        assertTrue(slot.get().choices().stream().noneMatch(c -> c.id() == built.getId()),
                "the card being played cannot be discarded");
        assertTrue(slot.get().choices().stream().anyMatch(c -> c.kind() == BuildInputDecisions.KIND_MU_STOP));
        assertTrue(slot.get().choices().stream().anyMatch(c -> c.id() == other.getId()));
    }

    /** Anaerobic Microorganisms pays -10 with two microbes, and stacks with Restructured Resources for -15. */
    @Test
    void anaerobicMicroorganismsPaysTenAndStacksWithRestructuredResources() {
        MarsGame game = phaseOne();
        Player player = named(game, "A");
        player.setMc(200);
        Card anaerobic = byAction(CardAction.ANAEROBIC_MICROORGANISMS);
        Card restructured = byAction(CardAction.RESTRUCTURED_RESOURCES);
        player.getPlayed().addCard(anaerobic.getId());
        player.getPlayed().addCard(restructured.getId());
        player.getCardResourcesCount().put(anaerobic.getClass(), 2);
        // Restructured Resources is paid with a plant ("one green"), not with a green card:
        // canUseRestructured requires the special effect and player.getPlants() >= 1.
        player.setPlants(5);
        Card green = firstWithColor(com.terraforming.ares.model.CardColor.GREEN);
        player.getHand().getCards().clear();
        player.getHand().addCards(List.of(green.getId()));

        Map<Integer, List<Integer>> input = buildInputDecisions.fill(game, player, green, decision -> 0);
        List<BuildPaymentChoice> plans = buildPaymentDecisions.options(game, player, green, input);

        boolean anaerobicPlan = plans.stream().anyMatch(p -> p.payments().stream()
                .anyMatch(pay -> pay instanceof AnaerobicMicroorganismsPayment));
        boolean bothPlan = plans.stream().anyMatch(p -> p.payments().stream()
                .anyMatch(pay -> pay instanceof AnaerobicMicroorganismsPayment)
                && p.payments().stream().anyMatch(pay -> pay instanceof RestructuredResourcesPayment));
        assertTrue(anaerobicPlan, "-10 must be offered with two microbes on Anaerobic Microorganisms");
        assertTrue(bothPlan, "-10 and -5 must be offered together, i.e. -15");

        player.setPlants(0);
        List<BuildPaymentChoice> withoutPlant = buildPaymentDecisions.options(game, player, green, input);
        assertTrue(withoutPlant.stream().noneMatch(p -> p.payments().stream()
                        .anyMatch(pay -> pay instanceof RestructuredResourcesPayment)),
                "without a plant the -5 is not legal");

        player.setPlants(5);
        player.getCardResourcesCount().put(anaerobic.getClass(), 1);
        List<BuildPaymentChoice> withoutMicrobes = buildPaymentDecisions.options(game, player, green, input);
        assertTrue(withoutMicrobes.stream().noneMatch(p -> p.payments().stream()
                        .anyMatch(pay -> pay instanceof AnaerobicMicroorganismsPayment)),
                "with one microbe the -10 is not legal");
    }

    /** Fibrous Composite Material asks a phase upgrade only when all three microbes are taken. */
    @Test
    void fibrousAsksForAPhaseUpgradeOnlyForThreeMicrobes() {
        MarsGame game = phaseOne();
        Player player = named(game, "A");
        Card fibrous = byAction(CardAction.FIBROUS_COMPOSITE_MATERIAL);
        player.getPlayed().addCard(fibrous.getId());

        var first = blueInputDecisions.nextSlot(game, player, fibrous, new HashMap<>());
        assertTrue(first.isPresent(), "the microbe amount is asked first");

        Map<Integer, List<Integer>> one = new HashMap<>();
        one.put(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(1));
        assertTrue(blueInputDecisions.nextSlot(game, player, fibrous, one).isEmpty(),
                "one microbe needs no phase upgrade");

        Map<Integer, List<Integer>> three = new HashMap<>();
        three.put(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(3));
        var afterThree = blueInputDecisions.nextSlot(game, player, fibrous, three);
        assertTrue(afterThree.isPresent(), "three microbes are paid for with a phase upgrade");
    }

    /** Research Grant asks the tag first, and the tag then fires the triggers it would fire on a build. */
    @Test
    void researchGrantAsksTheTagAndThenItsTriggers() {
        MarsGame game = phaseOne();
        Player player = named(game, "A");
        Card grant = byAction(CardAction.RESEARCH_GRANT);
        Card university = byAction(CardAction.MARS_UNIVERSITY);
        player.getPlayed().addCard(grant.getId());
        player.getPlayed().addCard(university.getId());

        var tagSlot = blueInputDecisions.nextSlot(game, player, grant, new HashMap<>());
        assertTrue(tagSlot.isPresent(), "the tag is asked first");

        Map<Integer, List<Integer>> science = new HashMap<>();
        science.put(InputFlag.TAG_INPUT.getId(), List.of(Tag.SCIENCE.ordinal()));
        var triggered = blueInputDecisions.nextSlot(game, player, grant, science);
        assertTrue(triggered.isPresent(), "a science tag fires Mars University");
        assertTrue(triggered.get().choices().stream().anyMatch(c -> c.kind() == BlueInputDecisions.KIND_TARGET
                        || c.kind() == BlueInputDecisions.KIND_DISCARD_STOP
                        || c.id() == BlueInputDecisions.ID_DECOMP_MICROBE),
                "Mars University asks what to discard after the tag");
    }

    /** Power Infrastructure: any amount of heat becomes that much money. */
    @Test
    void powerInfrastructureTurnsHeatIntoMoney() {
        MarsGame game = phaseOne();
        Player player = named(game, "A");
        Card power = byAction(CardAction.POWER_INFRASTRUCTURE);
        player.getPlayed().addCard(power.getId());
        player.setHeat(8);
        player.setMc(10);

        var slot = blueInputDecisions.nextSlot(game, player, power, new HashMap<>());
        assertTrue(slot.isPresent(), "the heat amount is the input");
        assertEquals(8, slot.get().choices().size(), "every amount from one to all the heat is offered");

        Map<Integer, List<Integer>> input = new HashMap<>();
        input.put(InputFlag.DISCARD_HEAT.getId(), List.of(8));
        aiTurnService.performBlueAction(game, player, power.getId(), input);
        assertEquals(18, player.getMc(), "spent heat is paid at par");
        assertEquals(0, player.getHeat());
    }

    /** Self Replicating Bacteria: five microbes buy a build slot with -25, and it is a build, not money. */
    @Test
    void selfReplicatingBacteriaGrantsTheTwentyFiveDiscountForFiveMicrobes() {
        MarsGame game = phaseOne();
        Player player = named(game, "A");
        Card bacteria = byAction(CardAction.SELF_REPLICATING_BACTERIA);
        player.getPlayed().addCard(bacteria.getId());
        player.getCardResourcesCount().put(bacteria.getClass(), 5);
        player.getBuilds().clear();

        Map<Integer, List<Integer>> input = new HashMap<>();
        input.put(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(5));
        aiTurnService.performBlueAction(game, player, bacteria.getId(), input);

        assertEquals(0, player.getCardResourcesCount().get(bacteria.getClass()).intValue(), "five microbes are spent");
        assertTrue(player.getBuilds().stream().anyMatch(build -> build.getExtraDiscount() >= 25),
                "a build slot worth -25 must appear, otherwise the microbes are wasted");
    }

    /**
     * Drone Assisted Construction: 2 MC, and 2 more when the phase I chose this round is upgraded - even when the phase
     * being played belongs to the opponent (owner's case: I chose the upgraded phase 2, the dummy plays phase 3).
     */
    @Test
    void droneAssistedConstructionPaysTheBonusForMyOwnUpgradedPhase() {
        MarsGame game = phaseOne();
        Player player = named(game, "A");
        Card drone = byAction(CardAction.DRONE_ASSISTED_CONSTRUCTION);
        player.getPlayed().addCard(drone.getId());
        player.setChosenPhase(3);
        player.setMc(10);

        aiTurnService.performBlueAction(game, player, drone.getId(), new HashMap<>());
        assertEquals(12, player.getMc(), "2 MC without an upgraded phase");

        // the engine refuses to activate the same blue card twice in one generation
        player.getActivatedBlueCards().getCards().clear();
        player.setMc(10);
        player.setChosenPhase(2);
        com.terraforming.ares.services.UpgradePhaseHelper.upgradePhase(player, 2); // phase 2, first step
        aiTurnService.performBlueAction(game, player, drone.getId(), new HashMap<>());
        assertEquals(14, player.getMc(), "2 + 2 when my chosen phase is upgraded");
    }

    /** Green Houses: every unit of heat becomes a plant, and the menu offers every amount up to the whole stock. */
    @Test
    void greenHousesTurnsAllHeatIntoPlants() {
        MarsGame game = phaseOne();
        Player player = named(game, "A");
        Card greenHouses = byAction(CardAction.GREEN_HOUSES);
        player.getPlayed().addCard(greenHouses.getId());
        player.setHeat(6);
        player.setPlants(1);

        var slot = blueInputDecisions.nextSlot(game, player, greenHouses, new HashMap<>());
        assertTrue(slot.isPresent(), "the heat amount is the input");
        // The card itself allows at most four heat per activation (ActionInputData.max = 4), so the menu is 1..4 even
        // with more heat in stock: dumping a big stock takes several activations, which is what point 9 of the owner's
        // list is about.
        assertEquals(4, slot.get().choices().size(), "the card's own maximum is four heat per activation");

        Map<Integer, List<Integer>> input = new HashMap<>();
        input.put(InputFlag.DISCARD_HEAT.getId(), List.of(4));
        aiTurnService.performBlueAction(game, player, greenHouses.getId(), input);
        assertEquals(5, player.getPlants(), "heat is paid at par in plants");
        assertEquals(2, player.getHeat(), "the rest of the stock stays");
    }

    // ------------------------------------------------------------------------------------------------------------------

    private MarsGame phaseOne() {
        MarsGame game = gameService.createNewSimulation(parameters());
        game.setStateType(StateType.BUILD_GREEN_PROJECTS, marsContextProvider.provide(game));
        for (Player player : game.getPlayerUuidToPlayer().values()) {
            while (player.getNextTurn() != null) {
                player.removeNextTurn();
            }
            player.getBuilds().clear();
            player.getBuilds().add(new BuildDto(BuildType.GREEN));
        }
        return game;
    }

    private static GameParameters parameters() {
        return GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT))
                .mulligan(true)
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                .dummyHand(true)
                .build();
    }

    private static Player named(MarsGame game, String name) {
        return game.getPlayerUuidToPlayer().values().stream()
                .filter(p -> p.getName().equals(name)).findFirst().orElseThrow();
    }

    private Card byAction(CardAction action) {
        for (int id = 1; id < 600; id++) {
            Card card = cardService.getCard(id);
            if (card != null && card.getCardMetadata() != null && card.getCardMetadata().getCardAction() == action) {
                return card;
            }
        }
        throw new IllegalStateException("no card with action " + action);
    }

    private Card firstWithResource(CardCollectableResource resource, int excludeId) {
        for (int id = 1; id < 600; id++) {
            Card card = cardService.getCard(id);
            if (card != null && card.getId() != excludeId && card.getCollectableResource() == resource) {
                return card;
            }
        }
        throw new IllegalStateException("no card with resource " + resource);
    }

    private Card firstWithTag(Tag tag) {
        for (int id = 1; id < 600; id++) {
            Card card = cardService.getCard(id);
            if (card != null && card.getTags() != null && card.getTags().contains(tag)) {
                return card;
            }
        }
        throw new IllegalStateException("no card with tag " + tag);
    }

    private Card firstWithColor(com.terraforming.ares.model.CardColor color) {
        for (int id = 1; id < 600; id++) {
            Card card = cardService.getCard(id);
            if (card != null && card.getColor() == color) {
                return card;
            }
        }
        throw new IllegalStateException("no card of colour " + color);
    }
}
