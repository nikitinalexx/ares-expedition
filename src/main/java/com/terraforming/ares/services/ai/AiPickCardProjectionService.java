package com.terraforming.ares.services.ai;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.red.AdvancedEcosystems;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.build.PutResourceOnBuild;
import com.terraforming.ares.model.parameters.OceanRequirement;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.ai.dto.CardValueResponse;
import com.terraforming.ares.services.ai.turnProcessors.AiBuildProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiPickCardProjectionService {
    private final DeepNetwork deepNetwork;
    private final CardService cardService;
    private final AiBuildProjectService aiBuildProjectService;
    private final SpecialEffectsService specialEffectsService;

    public CardValueResponse getWorstCard(MarsGame game, Player player, List<Integer> cards) {
        double worstAdditionalValue = Float.MAX_VALUE;
        int worstCardIndex = 0;

        float initialChance = deepNetwork.testState(game, player);

        for (int i = 0; i < cards.size(); i++) {
            MarsGame tempGame = new MarsGame(game);
            Player tempPlayer = tempGame.getPlayerByUuid(player.getUuid());

            float additionalCardValue = cardExtraChanceIfBuilt(tempGame, tempPlayer, cards.get(i), initialChance, 0);
            if (additionalCardValue < worstAdditionalValue) {
                worstAdditionalValue = additionalCardValue;
                worstCardIndex = i;
            }
        }


        return CardValueResponse.of(cards.get(worstCardIndex), worstAdditionalValue);
    }

    public CardValueResponse getBestCard(MarsGame game, Player player, List<Integer> cardsToDiscard) {
        double bestCardValue = Float.MIN_VALUE;
        int bestCardIndex = 0;

        float initialChance = deepNetwork.testState(game, player);

        for (int i = 0; i < cardsToDiscard.size(); i++) {
            MarsGame tempGame = new MarsGame(game);
            Player tempPlayer = tempGame.getPlayerByUuid(player.getUuid());

            float additionalCardValue = cardExtraChanceIfBuilt(tempGame, tempPlayer, cardsToDiscard.get(i), initialChance, 0);
            if (additionalCardValue > bestCardValue) {
                bestCardValue = additionalCardValue;
                bestCardIndex = i;
            }
        }

        return CardValueResponse.of(cardsToDiscard.get(bestCardIndex), bestCardValue);
    }


    public float cardExtraChanceIfBuilt(MarsGame game, Player player, Integer cardId, float initialChance, int depth) {
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

        float extraChanceModifier = 1;

        if (!CollectionUtils.isEmpty(card.getTemperatureRequirement())) {
            extraChanceModifier = getChanceReductionBasedOnRequirement(game.getPlanet(), player, GlobalParameter.TEMPERATURE, card.getTemperatureRequirement());
        } else if (!CollectionUtils.isEmpty(card.getOxygenRequirement())) {
            extraChanceModifier = getChanceReductionBasedOnRequirement(game.getPlanet(), player, GlobalParameter.OXYGEN, card.getOxygenRequirement());
        } else if (card.getOceanRequirement() != null) {
            OceanRequirement oceanRequirement = card.getOceanRequirement();

            int revealedOceansCount = game.getPlanet().getRevealedOceans().size();

            if (revealedOceansCount < oceanRequirement.getMinValue()) {
                extraChanceModifier = (float) revealedOceansCount / game.getPlanet().getOceans().size();
            } else if (revealedOceansCount > oceanRequirement.getMaxValue()) {
                extraChanceModifier = 0;
            }
        }

        if (card.getClass() == AdvancedEcosystems.class) {
            int uniqueTagsPlayed = cardService.countUniquePlayedTags(player, Set.of(Tag.PLANT, Tag.MICROBE, Tag.ANIMAL));

            extraChanceModifier = (float) uniqueTagsPlayed / 3;
        }

        //TODO projection is only checking a single build. what if checking this + one more card from hand will give better results?
        if (card.getColor() == CardColor.GREEN) {
            player.setBuilds(List.of(new BuildDto(BuildType.GREEN)));
        } else {
            player.setBuilds(List.of(new BuildDto(BuildType.BLUE_RED)));
        }

        List<Integer> handCards = new ArrayList<>(player.getHand().getCards());

        MarsGame stateAfterPlayingTheCard = aiBuildProjectService.projectBuildCardNoRequirements(game, player, card);

        float projectedChance = deepNetwork.testState(stateAfterPlayingTheCard, stateAfterPlayingTheCard.getPlayerByUuid(player.getUuid()));

        if (!player.isFirstBot() && depth == 0) {
            handCards.remove(cardId);

            float extraChance = 0;

            for (Integer handCard : handCards) {
                float nextBestCard = cardExtraChanceIfBuilt(stateAfterPlayingTheCard, stateAfterPlayingTheCard.getPlayerByUuid(player.getUuid()), handCard, projectedChance, 1);
                if (nextBestCard > extraChance) {
                    extraChance = nextBestCard;
                }
            }
            if (extraChance > 0) {
                projectedChance += extraChance;
            }
        }
        return (projectedChance - initialChance) < 0 ? (projectedChance - initialChance) : extraChanceModifier * (projectedChance - initialChance);

    }

    private float getChanceReductionBasedOnRequirement(Planet planet, Player player, GlobalParameter parameter, List<ParameterColor> requirement) {
        boolean canAmplifyRequirement = specialEffectsService.ownsSpecialEffect(player, SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT);
        if (canAmplifyRequirement) {
            requirement = amplifyRequirement(requirement);
        }
        if (parameter == GlobalParameter.OXYGEN && planet.isValidOxygen(requirement) || parameter == GlobalParameter.TEMPERATURE && planet.isValidTemperatute(requirement)) {
            return 1;
        }

        if (requirement.contains(ParameterColor.P) &&
                (canAmplifyRequirement || player.getHand().getCards().stream().map(cardService::getCard).noneMatch(card -> card.getSpecialEffects().contains(SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT)))) {
            return 0;
        } else {
            return planet.getParameterProportionTillMinColor(parameter, ParameterColor.values()[requirement.stream().mapToInt(Enum::ordinal).min().orElse(0)]);
        }
    }

    private List<ParameterColor> amplifyRequirement(List<ParameterColor> initialRequirement) {
        List<ParameterColor> resultRequirement = new ArrayList<>(initialRequirement);
        int minRequirement = initialRequirement.stream().mapToInt(Enum::ordinal).min().orElse(0);
        int maxRequirement = initialRequirement.stream().mapToInt(Enum::ordinal).max().orElse(ParameterColor.W.ordinal());

        if (minRequirement > 0) {
            resultRequirement.add(ParameterColor.values()[minRequirement - 1]);
        }
        if (maxRequirement < ParameterColor.W.ordinal()) {
            resultRequirement.add(ParameterColor.values()[maxRequirement + 1]);
        }

        return resultRequirement;
    }


}
