package com.terraforming.ares;

import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.PaymentValidationService;
import com.terraforming.ares.services.SpecialEffectsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@ExtendWith(MockitoExtension.class)
class TestCardValidationService {
    private CardValidationService cardValidationService;

    @Mock
    private CardService mockDeckService;

    @Mock
    private PaymentValidationService paymentValidationService;

    @Mock
    private SpecialEffectsService specialEffectsService;

    @BeforeEach
    public void setUp() {
        cardValidationService = new CardValidationService(
                mockDeckService,
                paymentValidationService,
                specialEffectsService,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    @Test
    void testNormalRequirements() {
        /*Mockito.when(mockDeckService.getProjectCard(0)).thenReturn(new GeothermalPower() {

            @Override
            public List<ParameterColor> getOxygenRequirement() {
                return Arrays.asList(ParameterColor.YELLOW, ParameterColor.WHITE);
            }

        });

        cardValidationService.validateCard(null, null, 0, null);*/
    }

}
