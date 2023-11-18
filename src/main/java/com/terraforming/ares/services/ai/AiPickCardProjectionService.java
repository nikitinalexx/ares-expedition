package com.terraforming.ares.services.ai;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.build.PutResourceOnBuild;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.dto.CardValueResponse;
import com.terraforming.ares.services.ai.turnProcessors.AiBuildProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiPickCardProjectionService {
    private final DeepNetwork deepNetwork;
    private final CardService cardService;
    private final AiBuildProjectService aiBuildProjectService;

    public CardValueResponse getWorstCard(MarsGame game, Player player, List<Integer> cards) {
        double worstCardValue = Float.MAX_VALUE;
        int worstCardIndex = 0;

        for (int i = 0; i < cards.size(); i++) {
            MarsGame tempGame = new MarsGame(game);
            Player tempPlayer = tempGame.getPlayerByUuid(player.getUuid());

            float additionalCardValue = cardExtraChanceIfBuilt(tempGame, tempPlayer, cards.get(i));
            if (additionalCardValue < worstCardValue) {
                worstCardValue = additionalCardValue;
                worstCardIndex = i;
            }
        }


        return CardValueResponse.of(cards.get(worstCardIndex), worstCardValue);
    }

    public Integer getBestCard(MarsGame game, Player player, List<Integer> cardsToDiscard) {
        double bestCardValue = Float.MIN_VALUE;
        int bestCardIndex = 0;

        for (int i = 0; i < cardsToDiscard.size(); i++) {
            MarsGame tempGame = new MarsGame(game);
            Player tempPlayer = tempGame.getPlayerByUuid(player.getUuid());

            float additionalCardValue = cardExtraChanceIfBuilt(tempGame, tempPlayer, cardsToDiscard.get(i));
            if (additionalCardValue > bestCardValue) {
                bestCardValue = additionalCardValue;
                bestCardIndex = i;
            }
        }
        return cardsToDiscard.get(bestCardIndex);
    }


    public float cardExtraChanceIfBuilt(MarsGame game, Player player, Integer cardId) {
        float initialChance = deepNetwork.testState(game, player);

        Card card = cardService.getCard(cardId);

        CardMetadata cardMetadata = card.getCardMetadata();
        if (player.getCardResourcesCount().isEmpty() && cardMetadata != null && !CollectionUtils.isEmpty(cardMetadata.getResourcesOnBuild())) {
            PutResourceOnBuild putResourceOnBuild = cardMetadata.getResourcesOnBuild().get(0);
            if (putResourceOnBuild.getParamId() == InputFlag.CEOS_FAVORITE_PUT_RESOURCES.getId()) {
                return 0;
            }
        }
        if (card.getCardMetadata().getCardAction() == CardAction.SYNTHETIC_CATASTROPHE) {
            List<Card> playedRedCards = player.getPlayed().getCards().stream().map(cardService::getCard).filter(c -> c.getColor() == CardColor.RED)
                    .collect(Collectors.toList());
            if (playedRedCards.isEmpty()) {
                return 0;
            }
        }

        if (!CollectionUtils.isEmpty(card.getTemperatureRequirement()) && card.getTemperatureRequirement().get(0) == ParameterColor.W && !game.getPlanetAtTheStartOfThePhase().isValidTemperatute(List.of(ParameterColor.Y, ParameterColor.W))) {
            return -1;
        }

        //farming  card
        //mangrove
        //ice cap melting

        if (card.getColor() == CardColor.GREEN) {
            player.setBuilds(List.of(new BuildDto(BuildType.GREEN)));
        } else {
            player.setBuilds(List.of(new BuildDto(BuildType.BLUE_RED)));
        }

        MarsGame stateAfterPlayingTheCard = aiBuildProjectService.projectBuildCardNoRequirements(game, player, card);

        float projectedChance = deepNetwork.testState(stateAfterPlayingTheCard, stateAfterPlayingTheCard.getPlayerByUuid(player.getUuid()));

        return projectedChance - initialChance;
    }


}
