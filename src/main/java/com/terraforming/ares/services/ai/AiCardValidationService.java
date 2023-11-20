package com.terraforming.ares.services.ai;

import com.terraforming.ares.cards.green.TopographicMapping;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsService;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiCardValidationService {
    private final AiCardBuildParamsService aiCardParamsHelper;
    private final CardValidationService cardValidationService;
    private final AiPaymentService aiPaymentService;

    public boolean isValid(MarsGame game, Player player, Card card) {
        Map<Integer, List<Integer>> inputParameters = aiCardParamsHelper.getInputParamsForValidation(game, player, card);
        String errorMessage = cardValidationService.validateCard(
                player, game, card.getId(),
                aiPaymentService.getCardPayments(game, player, card, inputParameters),
                inputParameters
        );
        return errorMessage == null;
    }

}
