package com.terraforming.ares.services.ai;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ai.AiTurnChoice;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiCollectIncomePhaseService {
    private final DeepNetwork deepNetwork;
    private final CardService cardService;
    private final TerraformingService terraformingService;

    public Integer getDoubleIncomeCard(MarsGame game, Player player) {
        Integer doubleIncomeCard = null;

        List<Card> greenCards = player.getPlayed().getCards().stream().map(cardService::getCard).filter(card -> card.getColor() == CardColor.GREEN && card.canPayAgain()).collect(Collectors.toList());

        if (!greenCards.isEmpty()) {

            boolean canIncreaseOxygen = terraformingService.canIncreaseOxygen(game);

            MarsGame gameCopy = new MarsGame(game);
            Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());

            float initialState = 0;

            if (player.getDifficulty().PICK_PHASE == AiTurnChoice.NETWORK) {
                initialState = deepNetwork.testState(game, player);
            }

            for (Card greenCard : greenCards) {
                greenCard.payAgain(gameCopy, cardService, playerCopy);

                float stateAfterIncome = analyzeStateAfterIncome(gameCopy, player, playerCopy, canIncreaseOxygen);

                if (stateAfterIncome > initialState) {
                    initialState = stateAfterIncome;
                    doubleIncomeCard = greenCard.getId();
                }

                if (player.getHand().size() != playerCopy.getHand().size()) {
                    gameCopy = new MarsGame(game);//restore full game
                } else {
                    gameCopy.getPlayerUuidToPlayer().put(player.getUuid(), new Player(player));//restore only player
                }
                playerCopy = gameCopy.getPlayerByUuid(player.getUuid());
            }
        }

        return doubleIncomeCard;
    }

    private float analyzeStateAfterIncome(MarsGame game, Player player, Player playerCopy, boolean canIncreaseOxygen) {
        switch (player.getDifficulty().PICK_PHASE) {
            case SMART:
            case RANDOM:
                return (playerCopy.getMc() - player.getMc()) +
                        (playerCopy.getHeat() - player.getHeat()) +
                        (playerCopy.getPlants() - player.getPlants()) * (canIncreaseOxygen ? 2 : 1) +
                        (playerCopy.getHand().size() - player.getHand().size()) * 4;

            case NETWORK:
                return deepNetwork.testState(game, playerCopy);
        }
        throw new IllegalStateException("Unreachable state");
    }
}
