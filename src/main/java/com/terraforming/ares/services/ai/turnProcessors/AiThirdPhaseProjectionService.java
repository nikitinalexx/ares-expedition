package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.dataset.DatasetCollectionService;
import com.terraforming.ares.dataset.MarsGameRow;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.ai.DeepNetwork;
import com.terraforming.ares.services.ai.dto.PhaseChoiceProjection;
import com.terraforming.ares.services.ai.thirdPhaseCards.AiCardProjection;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AiThirdPhaseProjectionService {
    private final Map<Class<?>, AiCardProjection<?>> actionProjections;
    private final CardService cardService;
    private final DeepNetwork deepNetwork;
    private final DatasetCollectionService datasetCollectionService;
    private final SpecialEffectsService specialEffectsService;

    public AiThirdPhaseProjectionService(List<AiCardProjection<?>> actionProjections, CardService cardService, DeepNetwork deepNetwork, DatasetCollectionService datasetCollectionService, SpecialEffectsService specialEffectsService) {
        this.actionProjections = actionProjections.stream().collect(
                Collectors.toMap(
                        AiCardProjection::getType,
                        Function.identity()
                )
        );
        this.cardService = cardService;
        this.deepNetwork = deepNetwork;
        this.datasetCollectionService = datasetCollectionService;
        this.specialEffectsService = specialEffectsService;
    }

    public PhaseChoiceProjection projectThirdPhase(MarsGame game, Player player) {
        game = new MarsGame(game);
        player = game.getPlayerByUuid(player.getUuid());
        player.setBlueActionExtraActivationsLeft(1);
        return projectThirdPhase(game, player, new MarsGameRowDifference());
    }

    public PhaseChoiceProjection projectThirdPhase(MarsGame game, Player player, MarsGameRowDifference initialDifference) {
        //TODO where to put Heat Exchange?
        //TODO project UNMI action
        //TODO project standard action

        //project cards
        List<Card> activeCards = player.getPlayed().getCards().stream().map(cardService::getCard).filter(Card::isActiveCard).filter(card -> !player.getActivatedBlueCards().containsCard(card.getId()) || player.getBlueActionExtraActivationsLeft() != 0).collect(Collectors.toList());

        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        MarsGameRow playerData = datasetCollectionService.collectPlayerData(game, player, anotherPlayer);
        if (playerData == null) {
            return PhaseChoiceProjection.SKIP_PHASE;
        }
        float bestState = deepNetwork.testState(playerData.applyDifference(initialDifference), player.isFirstBot() ? 1 : 2);

        MarsGameRowDifference bestProjectionRow = null;
        MarsGame bestGameProjection = null;
        Card bestCard = null;

        for (Card activeCard : activeCards) {
            MarsGame gameCopy = new MarsGame(game);
            Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());

            MarsGameRowDifference difference = actionProjections.get(activeCard.getClass()).project(initialDifference, gameCopy, playerCopy, activeCard);
            difference.add(initialDifference);

            if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.ASSEMBLY_LINES)) {
                playerCopy.setMc(playerCopy.getMc() + 1);
            }

            //TODO is it better to project 2 steps ahead? implement and compare 2 computers
            //TODO reuse anotherPlayer
            float futureState = deepNetwork.testState(datasetCollectionService.collectPlayerData(gameCopy, playerCopy, anotherPlayer).applyDifference(difference), player.isFirstBot() ? 1 : 2);

            if (futureState > bestState) {
                bestState = futureState;
                bestProjectionRow = difference;
                bestGameProjection = gameCopy;
                bestCard = activeCard;
            }
        }

        //project plants/heat  exchange
        //project opponent

        if (bestProjectionRow != null) {
            Player bestProjectionPlayer = bestGameProjection.getPlayerByUuid(player.getUuid());
            Deck activatedBlueCards = bestProjectionPlayer.getActivatedBlueCards();
            if (activatedBlueCards.containsCard(bestCard.getId())) {
                bestProjectionPlayer.setBlueActionExtraActivationsLeft(bestProjectionPlayer.getBlueActionExtraActivationsLeft() - 1);
            } else {
                activatedBlueCards.addCard(bestCard.getId());
            }
            PhaseChoiceProjection anotherProjection = projectThirdPhase(bestGameProjection, bestProjectionPlayer, bestProjectionRow);
            if (anotherProjection.isPickPhase()) {
                return anotherProjection;
            }
            return PhaseChoiceProjection.builder().phase(3).pickPhase(true).chance(bestState).build();
        }

        return PhaseChoiceProjection.SKIP_PHASE;
    }

}
