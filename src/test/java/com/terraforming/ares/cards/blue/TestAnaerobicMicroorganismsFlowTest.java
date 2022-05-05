package com.terraforming.ares.cards.blue;

import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.payments.AnaerobicMicroorganismsPayment;
import com.terraforming.ares.model.payments.MegacreditsPayment;
import com.terraforming.ares.services.DeckService;
import com.terraforming.ares.services.PaymentValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@SpringBootTest
public class TestAnaerobicMicroorganismsFlowTest {
    private static final int ANAEROBIC_MICROORGANISMS_CARD_ID = 21;

    @Autowired
    private PaymentValidationService paymentValidationService;

    @Autowired
    private DeckService deckService;

    private PlayerContext player;

    @BeforeEach
    public void setUp() {
        player = PlayerContext.builder().build();
        player.getPlayed().addCard(ANAEROBIC_MICROORGANISMS_CARD_ID);
    }

    @Test
    void testNotEnoughResources() {
        player.getCardIdToResourcesCount().put(ANAEROBIC_MICROORGANISMS_CARD_ID, 1);
        String errorMessage = paymentValidationService.validate(
                new AiCentral(100500),
                player,
                Collections.singletonList(new AnaerobicMicroorganismsPayment(
                        2
                ))
        );
        assertEquals("Invalid payment: Anaerobic Microorganisms < 2", errorMessage);
    }

    @Test
    void testNotEnoughTotalMcToPay() {
        player.getCardIdToResourcesCount().put(ANAEROBIC_MICROORGANISMS_CARD_ID, 3);

        String errorMessage = paymentValidationService.validate(
                new AiCentral(100500), player, Collections.singletonList(new AnaerobicMicroorganismsPayment(
                        2
                ))
        );
        assertEquals("Total payment is not enough to cover the project price", errorMessage);
    }

    @Test
    void testEnoughResources() {
        player.getCardIdToResourcesCount().put(ANAEROBIC_MICROORGANISMS_CARD_ID, 3);
        player.setMc(100);

        String errorMessage = paymentValidationService.validate(
                new AiCentral(100500),
                player,
                Arrays.asList(
                        new AnaerobicMicroorganismsPayment(2),
                        new MegacreditsPayment(12)
                )
        );
        assertNull(errorMessage);
    }

}
