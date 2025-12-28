package com.terraforming.ares.services.ai.turnProcessors.random;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AiRandomBuildGreenProjectTurnService {
    private final CardService cardService;
    private final AiRandomCardBuildParamsService aiRandomCardBuildParamsService;
    private final AiRandomPaymentService aiRandomPaymentService;
    private final CardValidationService cardValidationService;
    private final AiTurnService aiTurnService;

    public void performRandomBuildGreenTurn(MarsGame game, Player player) {
        List<Integer> hand = new ArrayList<>(player.getHand().getCards());
        Collections.shuffle(hand);

        boolean built = hand.stream()
                .map(cardService::getCard)
                .filter(card -> card.getColor() == CardColor.GREEN)
                .anyMatch(card -> {
                    Map<Integer, List<Integer>> inputParams =
                            aiRandomCardBuildParamsService.getInputParamsForBuild(game, player, card);
                    if (inputParams == null) {
                        return false;
                    }

                    List<Payment> payments =
                            aiRandomPaymentService.getCardPayments(game, player, card, inputParams);

                    if (cardValidationService.validateCard(
                            player, game, card.getId(), payments, inputParams) != null) {
                        return false;
                    }

                    aiTurnService.buildProject(game, player, card.getId(), payments, inputParams);
                    return true;
                });
        if (!built) {
            aiTurnService.skipTurn(player);
        }
    }
}
