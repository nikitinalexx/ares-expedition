package com.terraforming.ares.services.ai.turnProcessors.random;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.ai.dto.AvailableTurn;
import com.terraforming.ares.services.ai.dto.AvailableTurnType;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class AiRandomSecondPhaseActionProcessor {
    private final AiTurnService aiTurnService;
    private final CardService cardService;
    private final AiRandomCardBuildParamsService aiRandomCardBuildParamsService;
    private final AiRandomPaymentService aiRandomPaymentService;
    private final CardValidationService cardValidationService;
    private final AiRandomSellCardsService aiRandomSellCardsService;
    private final Random random = new Random();

    public void processTurn(List<TurnType> possibleTurns, MarsGame game, Player player) {
        aiRandomSellCardsService.sellCardsAsyncByChance(player);

        List<AvailableTurn> availableTurns = new ArrayList<>();

        if (possibleTurns.contains(TurnType.UNMI_RT) && player.getMc() >= 6) {
            availableTurns.add(new AvailableTurn(AvailableTurnType.UNMI_RT));
        }
        if (possibleTurns.contains(TurnType.PICK_EXTRA_BONUS_SECOND_PHASE)) {
            availableTurns.add(new AvailableTurn(AvailableTurnType.SECOND_PHASE_BONUS));
        }
        if (!player.cantBuildAnything()) {
            availableTurns.add(new AvailableTurn(AvailableTurnType.BUILD_PROJECT));
        }

        while (!availableTurns.isEmpty()) {
            AvailableTurn availableTurn = availableTurns.get(random.nextInt(availableTurns.size()));

            if (availableTurn.getType() != AvailableTurnType.BUILD_PROJECT) {
                switch (availableTurn.getType()) {
                    case UNMI_RT -> aiTurnService.unmiRtCorporationTurn(game, player);
                    case SECOND_PHASE_BONUS -> aiTurnService.pickExtraCardTurnAsync(player);
                }
                return;
            }

            boolean canBuildGreen = player.canBuildGreen();
            boolean canBuildBlueRed = player.canBuildBlueRed();

            List<Integer> hand = new ArrayList<>(player.getHand().getCards());
            Collections.shuffle(hand);

            boolean built = hand.stream()
                    .map(cardService::getCard)
                    .filter(card ->
                            (canBuildGreen && card.getColor() == CardColor.GREEN) ||
                                    (canBuildBlueRed && (card.getColor() == CardColor.BLUE || card.getColor() == CardColor.RED))
                    )
                    .anyMatch(card -> {
                        Map<Integer, List<Integer>> inputParams = aiRandomCardBuildParamsService.getInputParamsForBuild(game, player, card);
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

            if (built) {
                return;
            }

            availableTurns.remove(availableTurn);
        }

        aiTurnService.skipTurn(player);
    }


}
