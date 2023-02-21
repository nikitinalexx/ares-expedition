package com.terraforming.ares.cards.blue;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.payments.AnaerobicMicroorganismsPayment;
import com.terraforming.ares.model.payments.MegacreditsPayment;
import com.terraforming.ares.services.PaymentValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static com.terraforming.ares.model.Constants.AI_CENTRAL_CARD_ID;
import static com.terraforming.ares.model.Constants.ANAEROBIC_MICROORGANISMS_CARD_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@SpringBootTest
class AnaerobicMicroorganismsFlowTest {

    @Autowired
    private PaymentValidationService paymentValidationService;

    private Player player;
    private Card card;

    @BeforeEach
    public void setUp() {
        player = Player.builder().build();
        player.getPlayed().addCard(ANAEROBIC_MICROORGANISMS_CARD_ID);
        card = new AnaerobicMicroorganisms(ANAEROBIC_MICROORGANISMS_CARD_ID);
        player.initResources(card);
    }

    @Test
    void testNotEnoughResources() {
        player.addResources(card, 1);
        String errorMessage = paymentValidationService.validate(
                new AiCentral(AI_CENTRAL_CARD_ID),
                player,
                Collections.singletonList(new AnaerobicMicroorganismsPayment()),
                Map.of()
        );
        assertEquals("Invalid payment: Anaerobic Microorganisms < 2", errorMessage);
    }

    @Test
    void testNotEnoughTotalMcToPay() {
        player.addResources(card, 3);

        String errorMessage = paymentValidationService.validate(
                new AiCentral(AI_CENTRAL_CARD_ID), player, Collections.singletonList(new AnaerobicMicroorganismsPayment()), Map.of()
        );
        assertEquals("Total payment is not enough to cover the project price", errorMessage);
    }

    @Test
    void testEnoughResources() {
        player.addResources(card, 3);
        player.setMc(100);

        String errorMessage = paymentValidationService.validate(
                new AiCentral(AI_CENTRAL_CARD_ID),
                player,
                Arrays.asList(
                        new AnaerobicMicroorganismsPayment(),
                        new MegacreditsPayment(12)
                ),
                Map.of()
        );
        assertNull(errorMessage);
    }

}
