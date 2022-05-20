package com.terraforming.ares.cards.blue;

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

    @BeforeEach
    public void setUp() {
        player = Player.builder().build();
        player.getPlayed().addCard(ANAEROBIC_MICROORGANISMS_CARD_ID);
    }

    @Test
    void testNotEnoughResources() {
        player.getCardResourcesCount().put(AnaerobicMicroorganisms.class, 1);
        String errorMessage = paymentValidationService.validate(
                new AiCentral(AI_CENTRAL_CARD_ID),
                player,
                Collections.singletonList(new AnaerobicMicroorganismsPayment())
        );
        assertEquals("Invalid payment: Anaerobic Microorganisms < 2", errorMessage);
    }

    @Test
    void testNotEnoughTotalMcToPay() {
        player.getCardResourcesCount().put(AnaerobicMicroorganisms.class, 3);

        String errorMessage = paymentValidationService.validate(
                new AiCentral(AI_CENTRAL_CARD_ID), player, Collections.singletonList(new AnaerobicMicroorganismsPayment())
        );
        assertEquals("Total payment is not enough to cover the project price", errorMessage);
    }

    @Test
    void testEnoughResources() {
        player.getCardResourcesCount().put(AnaerobicMicroorganisms.class, 3);
        player.setMc(100);

        String errorMessage = paymentValidationService.validate(
                new AiCentral(AI_CENTRAL_CARD_ID),
                player,
                Arrays.asList(
                        new AnaerobicMicroorganismsPayment(),
                        new MegacreditsPayment(12)
                )
        );
        assertNull(errorMessage);
    }

}
