package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.ai.AiCardsChoice;
import com.terraforming.ares.model.ai.AiExperimentalTurn;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiPickCardProjectionService;
import com.terraforming.ares.services.ai.ICardValueService;
import com.terraforming.ares.services.ai.network2.Network2DiscardCardsProcessor;
import com.terraforming.ares.services.policyai.PolicyCollectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiDiscardCardsTurn implements AiTurnProcessor {
    private final AiTurnService aiTurnService;
    private final ICardValueService cardValueService;
    private final CardService cardService;
    private final AiPickCardProjectionService aiPickCardProjectionService;
    private final Network2DiscardCardsProcessor network2DiscardCardsProcessor;
    private final PolicyCollectService policyCollectService;


    @Override
    public TurnType getType() {
        return TurnType.DISCARD_CARDS;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        DiscardCardsTurn nextTurn = (DiscardCardsTurn) player.getNextTurn();

        List<Integer> cardsToDiscardFromm;
        if (nextTurn.isOnlyFromSelectedCards()) {
            cardsToDiscardFromm = new ArrayList<>(nextTurn.getCards());
        } else {
            cardsToDiscardFromm = new ArrayList<>(player.getHand().getCards());
        }

        int cardsToKeepCount = cardsToDiscardFromm.size() - nextTurn.getSize();

        if (player.getDifficulty().CARDS_PICK == AiCardsChoice.NETWORK_PROJECTION) {
            List<Integer> cardsToKeep = aiPickCardProjectionService.getBestCards(game, player, cardsToDiscardFromm, cardsToKeepCount);
            cardsToDiscardFromm.removeAll(cardsToKeep);
        } else if (player.getDifficulty().EXPERIMENTAL_TURN == AiExperimentalTurn.EXPERIMENT) {
            List<Integer> cardsToKeep = network2DiscardCardsProcessor.getBestCards(game, player, cardsToDiscardFromm, cardsToKeepCount);
            cardsToDiscardFromm.removeAll(cardsToKeep);

            if ((game.getStateType() == StateType.PICK_CORPORATIONS || game.getStateType() == StateType.BUILD_GREEN_PROJECTS || game.getStateType() == StateType.BUILD_BLUE_RED_PROJECTS || game.getStateType() == StateType.PERFORM_BLUE_ACTION || game.getStateType() == StateType.DRAFT_CARDS)) {

                if (nextTurn.isOnlyFromSelectedCards()) {
                    policyCollectService.discardingFromSelected(
                            game,
                            player,
                            nextTurn.getCards(),
                            cardsToDiscardFromm
                    );
                } else {
                    policyCollectService.discardingFromHandPostBuild(game, player, cardsToDiscardFromm);
                }
            }

        } else {
            for (int i = 0; i < cardsToKeepCount; i++) {
                //keep best card
                Integer bestCard = null;

                switch (player.getDifficulty().CARDS_PICK) {
                    case FILE_VALUE:
                        bestCard = cardValueService.getBestCard(game, player, cardsToDiscardFromm, game.getTurns());
                        if (bestCard != null && Constants.LOG_NET_COMPARISON) {
                            System.out.println("Keeping statistics " + cardService.getCard(bestCard).getClass().getSimpleName());
                        }
                        break;
                    case RANDOM:
                        bestCard = cardsToDiscardFromm.get(ThreadLocalRandom.current().nextInt(cardsToDiscardFromm.size()));
                        break;
                }

                cardsToDiscardFromm.remove(bestCard);
            }
        }


        if (Constants.LOG_NET_COMPARISON) {
            System.out.println("Discarding " + cardsToDiscardFromm.stream().map(cardService::getCard).map(card -> card.getClass().getSimpleName()).collect(Collectors.joining(",")));
            System.out.println();
        }

        aiTurnService.discardCards(
                game,
                player,
                new DiscardCardsTurn(player.getUuid(), cardsToDiscardFromm, cardsToDiscardFromm.size(), false, false, List.of()),
                cardsToDiscardFromm
        );

        return true;
    }


}
