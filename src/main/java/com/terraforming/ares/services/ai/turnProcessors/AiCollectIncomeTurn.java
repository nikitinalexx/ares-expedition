package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiCollectIncomeTurn implements AiTurnProcessor {
    private final AiTurnService aiTurnService;
    private final CardService cardService;
    private final TerraformingService terraformingService;

    @Override
    public TurnType getType() {
        return TurnType.COLLECT_INCOME;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        Integer doubleIncomeCard = null;
        if (player.getChosenPhase() == 4 && player.hasPhaseUpgrade(Constants.PHASE_4_UPGRADE_DOUBLE_PRODUCE)) {
            List<Card> greenCards = player.getPlayed().getCards().stream().map(cardService::getCard).filter(card -> card.getColor() == CardColor.GREEN).collect(Collectors.toList());

            if (!greenCards.isEmpty()) {
                int bestIncome = 0;

                boolean canIncreaseOxygen = terraformingService.canIncreaseOxygen(game);

                for (Card greenCard : greenCards) {
                    MarsGame gameCopy = new MarsGame(game);
                    Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());

                    greenCard.payAgain(gameCopy, cardService, playerCopy);

                    int income = (playerCopy.getMc() - player.getMc()) +
                            (playerCopy.getHeat() - player.getHeat()) +
                            (playerCopy.getPlants() - player.getPlants()) * (canIncreaseOxygen ? 2 : 1) +
                            (playerCopy.getHand().size() - player.getHand().size()) * 4;

                    if (income > bestIncome) {
                        bestIncome = income;
                        doubleIncomeCard = greenCard.getId();
                    }
                }
            }
        }
        aiTurnService.collectIncomeTurn(player, doubleIncomeCard);

        return true;
    }

}
