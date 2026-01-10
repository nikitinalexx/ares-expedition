package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.PrivateInvestorBeach;
import com.terraforming.ares.cards.red.AdvancedEcosystems;
import com.terraforming.ares.cards.red.CeosFavoriteProject;
import com.terraforming.ares.cards.red.Crater;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.ai.AiCardsChoice;
import com.terraforming.ares.model.ai.AiExperimentalTurn;
import com.terraforming.ares.model.build.PutResourceOnBuild;
import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.model.parameters.Ocean;
import com.terraforming.ares.model.parameters.OceanRequirement;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.AiPickCardProjectionService;
import com.terraforming.ares.services.ai.ICardValueService;
import com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService;
import com.terraforming.ares.services.ai.dl4j.NNService;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import com.terraforming.ares.services.ai.dto.CardProjection;
import com.terraforming.ares.services.ai.turnProcessors.network2.Network2DiscardCardsProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiDiscardCardsTurn implements AiTurnProcessor {
    private final Random random = new Random();
    private final AiTurnService aiTurnService;
    private final ICardValueService cardValueService;
    private final CardService cardService;
    private final AiPickCardProjectionService aiPickCardProjectionService;
    private final Network2DiscardCardsProcessor network2DiscardCardsProcessor;



    @Override
    public TurnType getType() {
        return TurnType.DISCARD_CARDS;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        DiscardCardsTurn nextTurn = (DiscardCardsTurn) player.getNextTurn();

        List<Integer> cardsToDiscard;
        if (nextTurn.isOnlyFromSelectedCards()) {
            cardsToDiscard = new ArrayList<>(nextTurn.getCards());
        } else {
            cardsToDiscard = new ArrayList<>(player.getHand().getCards());
        }

        int cardsToKeepCount = cardsToDiscard.size() - nextTurn.getSize();

        if (player.getDifficulty().CARDS_PICK == AiCardsChoice.NETWORK_PROJECTION) {
            List<Integer> cardsToKeep = aiPickCardProjectionService.getBestCards(game, player, cardsToDiscard, cardsToKeepCount);
            cardsToDiscard.removeAll(cardsToKeep);
        } else if (player.getDifficulty().EXPERIMENTAL_TURN == AiExperimentalTurn.EXPERIMENT) {
            List<Integer> cardsToKeep = network2DiscardCardsProcessor.getBestCards(game, player, cardsToDiscard, cardsToKeepCount);
            cardsToDiscard.removeAll(cardsToKeep);
        } else {
            for (int i = 0; i < cardsToKeepCount; i++) {
                //keep best card
                Integer bestCard = null;

                switch (player.getDifficulty().CARDS_PICK) {
                    case FILE_VALUE:
                        bestCard = cardValueService.getBestCard(game, player, cardsToDiscard, game.getTurns());
                        if (bestCard != null && Constants.LOG_NET_COMPARISON) {
                            System.out.println("Keeping statistics " + cardService.getCard(bestCard).getClass().getSimpleName());
                        }
                        break;
                    case RANDOM:
                        bestCard = cardsToDiscard.get(random.nextInt(cardsToDiscard.size()));
                        break;
                }

                cardsToDiscard.remove(bestCard);
            }
        }


        if (Constants.LOG_NET_COMPARISON) {
            System.out.println("Discarding " + cardsToDiscard.stream().map(cardService::getCard).map(card -> card.getClass().getSimpleName()).collect(Collectors.joining(",")));
            System.out.println();
        }

        aiTurnService.discardCards(
                game,
                player,
                new DiscardCardsTurn(player.getUuid(), cardsToDiscard, cardsToDiscard.size(), false, false, List.of()),
                cardsToDiscard
        );

        return true;
    }




}
