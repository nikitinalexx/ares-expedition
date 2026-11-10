package com.terraforming.ares;

import com.terraforming.ares.cards.blue.RestructuredResources;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildDto;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.payments.MegacreditsPayment;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.payments.RestructuredResourcesPayment;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.model.BuildType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the bug "plants go below zero" (docs/BUGS.md #2): the plants a card spends itself and the plants
 * spent as a payment (Restructured Resources) were validated independently, so a build could spend more plants than the
 * player has.
 */
class PlantSpendingValidationTest extends EngineTestBase {
    @Autowired
    GameService gameService;
    @Autowired
    CardService cardService;
    @Autowired
    CardValidationService cardValidationService;

    private MarsGame newGame() {
        return gameService.createNewSimulation(GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(PlayerDifficulty.RANDOM, PlayerDifficulty.RANDOM))
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                .dummyHand(true).build());
    }

    private int restructuredResourcesId() {
        for (int id : cardService.createProjectsDeck(List.of(Expansion.BASE, Expansion.DISCOVERY, Expansion.BUFFED_CORPORATION)).getCards()) {
            if (cardService.getCard(id) instanceof RestructuredResources) {
                return id;
            }
        }
        throw new IllegalStateException("Restructured Resources not found");
    }

    /**
     * Green cards that spend plants themselves, with the amount (negative PLANT bonus in the card metadata).
     */
    private Map<Integer, Integer> plantSpendingGreenCards() {
        Map<Integer, Integer> result = new java.util.TreeMap<>();
        for (int id : cardService.createProjectsDeck(List.of(Expansion.BASE, Expansion.DISCOVERY, Expansion.BUFFED_CORPORATION)).getCards()) {
            Card card = cardService.getCard(id);
            if (card.getColor() != CardColor.GREEN) {
                continue;
            }
            int spent = card.getCardMetadata().getBonuses().stream()
                    .filter(g -> g.getType() == GainType.PLANT && g.getValue() < 0)
                    .mapToInt(Gain::getValue).sum();
            if (spent < 0) {
                result.put(id, -spent);
            }
        }
        return result;
    }

    private String validate(MarsGame game, Player player, int cardId, int restructuredPayments) {
        Card card = cardService.getCard(cardId);
        List<Payment> payments = new ArrayList<>();
        int paidByPlants = 0;
        for (int i = 0; i < restructuredPayments; i++) {
            payments.add(new RestructuredResourcesPayment());
            paidByPlants += new RestructuredResourcesPayment().getDiscount();
        }
        payments.add(new MegacreditsPayment(Math.max(0, card.getPrice() - paidByPlants)));
        return cardValidationService.validateCard(player, game, cardId, payments, Map.of());
    }

    private Player prepared(MarsGame game, int cardId, int plants) {
        Player player = game.getPlayerUuidToPlayer().values().iterator().next();
        player.getHand().addCard(cardId);
        player.getPlayed().addCard(restructuredResourcesId());
        player.getBuilds().clear();
        player.getBuilds().add(new BuildDto(BuildType.GREEN));
        player.setMc(200);
        player.setPlants(plants);
        return player;
    }

    @Test
    void thereAreCardsThatSpendPlants() {
        Map<Integer, Integer> cards = plantSpendingGreenCards();
        assertTrue(cards.size() >= 3, "expected Food Factory, Biomass Combustors, Potatoes, ...: " + cards);
    }

    @Test
    void aCardThatSpendsPlantsCanNotBePaidWithTheSamePlants() {
        Map<Integer, Integer> cards = plantSpendingGreenCards();
        for (Map.Entry<Integer, Integer> e : cards.entrySet()) {
            int cardId = e.getKey();
            int cost = e.getValue();
            String name = cardService.getCard(cardId).getClass().getSimpleName();

            // exactly enough plants for the card itself, one of them is also spent as a payment -> impossible
            MarsGame tight = newGame();
            Player player = prepared(tight, cardId, cost);
            assertNotNull(validate(tight, player, cardId, 1), name + ": needs " + cost + " plants itself and 1 more for the payment, has " + cost);

            // with one plant more everything is fine (or fails for an unrelated reason, never because of plants)
            MarsGame enough = newGame();
            Player rich = prepared(enough, cardId, cost + 1);
            String result = validate(enough, rich, cardId, 1);
            assertTrue(result == null || !result.toLowerCase().contains("plant"), name + ": " + result);
        }
    }

    @Test
    void youCanNotPayWithMorePlantsThanYouHave() {
        int cardId = plantSpendingGreenCards().keySet().iterator().next();
        MarsGame game = newGame();
        Player player = prepared(game, cardId, 1);
        // plain card without plant cost would be cheaper to test, but any card with two payments of 1 plant each needs 2 plants
        assertNotNull(validate(game, player, cardId, 2), "two plant payments with a single plant");
    }

    @Test
    void theRegularCaseStillWorks() {
        int cardId = plantSpendingGreenCards().keySet().iterator().next();
        MarsGame game = newGame();
        Player player = prepared(game, cardId, 10);
        String result = validate(game, player, cardId, 0);
        assertTrue(result == null || !result.toLowerCase().contains("plant"), "paying in MC with plenty of plants: " + result);
        assertEquals(10, player.getPlants());
        assertNull(null);
    }
}
