package com.terraforming.ares.services.ai;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.PrivateInvestorBeach;
import com.terraforming.ares.cards.red.AdvancedEcosystems;
import com.terraforming.ares.cards.red.Crater;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.build.PutResourceOnBuild;
import com.terraforming.ares.model.parameters.OceanRequirement;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.ai.dto.CardProjection;
import com.terraforming.ares.services.ai.dto.CardValueResponse;
import com.terraforming.ares.services.ai.turnProcessors.AiBuildProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiPickCardProjectionService {
    private final DeepNetwork deepNetwork;
    private final CardService cardService;
    private final AiBuildProjectService aiBuildProjectService;
    private final SpecialEffectsService specialEffectsService;

    public CardValueResponse getWorstCard(MarsGame game, Player player, List<Integer> cards) {
        float worstAdditionalValue = Float.MAX_VALUE;
        int worstCardIndex = 0;

        float initialChance = deepNetwork.testState(game, player);

        Map<Integer, Float> cardToChance = new HashMap<>();

        for (int i = 0; i < cards.size(); i++) {
            MarsGame tempGame = new MarsGame(game);
            Player tempPlayer = tempGame.getPlayerByUuid(player.getUuid());

            CardProjection cardProjection = cardExtraChanceIfBuilt(tempGame, tempPlayer, cards.get(i), initialChance, 0);
            if (!cardProjection.isUsable()) {
                return CardValueResponse.notUsableCard(cards.get(i));
            }
            float cardChance = cardProjection.getChance();

            if (cardChance < worstAdditionalValue) {
                worstAdditionalValue = cardChance;
                worstCardIndex = i;
            }
            cardToChance.put(cards.get(i), cardChance);
        }

        if (Constants.LOG_NET_COMPARISON) {
            System.out.println("Get worst card: ");
            cardToChance.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .forEach(entry -> System.out.println("Card " + cardService.getCard(entry.getKey()).getClass().getSimpleName() + ": " + entry.getValue()));
        }

        return CardValueResponse.of(cards.get(worstCardIndex), worstAdditionalValue);
    }

    public long countBadCards(MarsGame game, Player player) {
        float initialChance = deepNetwork.testState(game, player);

        List<Integer> cards = player.getHand().getCards();

        int countWithNegative = 0;

        for (int i = 0; i < cards.size(); i++) {
            MarsGame tempGame = new MarsGame(game);
            Player tempPlayer = tempGame.getPlayerByUuid(player.getUuid());

            CardProjection additionalCardValue = cardExtraChanceIfBuilt(tempGame, tempPlayer, cards.get(i), initialChance, 0);

            if (!additionalCardValue.isUsable() || additionalCardValue.getChance() < 0) {
                countWithNegative++;
            }
        }

        return countWithNegative;
    }

    public float countPositiveAdditionalvalue(MarsGame game, Player player) {
        float initialChance = deepNetwork.testState(game, player);

        List<Integer> cards = player.getHand().getCards();

        float totalExtraValue = 0;

        for (Integer card : cards) {
            MarsGame tempGame = new MarsGame(game);
            Player tempPlayer = tempGame.getPlayerByUuid(player.getUuid());

            CardProjection cardProjection = cardExtraChanceIfBuilt(tempGame, tempPlayer, card, initialChance, 0);

            if (cardProjection.isUsable() && cardProjection.getChance() > 0) {
                totalExtraValue += (cardProjection.getChance() / initialChance);
            }
        }

        return totalExtraValue;
    }


    public List<Integer> getCardsToSell(MarsGame game, Player player, List<Integer> cards, int count) {
        float initialChance = deepNetwork.testState(game, player);

        Map<Integer, Float> cardToChance = new HashMap<>();

        for (Integer card : cards) {
            MarsGame tempGame = new MarsGame(game);
            Player tempPlayer = tempGame.getPlayerByUuid(player.getUuid());

            CardProjection additionalCardValue = cardExtraChanceIfBuilt(tempGame, tempPlayer, card, initialChance, 0);

            cardToChance.put(card, additionalCardValue.getChance());
        }

        if (Constants.LOG_NET_COMPARISON) {
            System.out.println("Get worst card: ");
            cardToChance.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .forEach(entry -> System.out.println("Card " + cardService.getCard(entry.getKey()).getClass().getSimpleName() + ": " + entry.getValue()));
        }

        return cardToChance.entrySet().stream()
                .sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).limit(count).collect(Collectors.toList());
    }

    public List<Integer> getBestCards(MarsGame game, Player player, List<Integer> cardsToDiscard, int count) {
        if (count == 1 && cardsToDiscard.size() == 1) {
            return List.of(cardsToDiscard.get(0));
        }

        float initialChance = deepNetwork.testState(game, player);

        Map<Integer, Float> cardToChance = new HashMap<>();

        for (Integer integer : cardsToDiscard) {
            MarsGame tempGame = new MarsGame(game);
            Player tempPlayer = tempGame.getPlayerByUuid(player.getUuid());

            cardToChance.put(integer, cardExtraChanceIfBuilt(tempGame, tempPlayer, integer, initialChance, 0).getChance());
        }
        if (Constants.LOG_NET_COMPARISON) {
            System.out.println("Get best card: ");
            cardToChance.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .forEach(entry -> System.out.println("Card " + cardService.getCard(entry.getKey()).getClass().getSimpleName() + ": " + entry.getValue()));
        }

        return cardToChance.entrySet().stream()
                .sorted(Map.Entry.<Integer, Float>comparingByValue().reversed()).map(Map.Entry::getKey).limit(count).collect(Collectors.toList());
    }


    public CardProjection cardExtraChanceIfBuilt(MarsGame game, Player player, Integer cardId, float initialChance, int depth) {
        Card card = cardService.getCard(cardId);

        CardMetadata cardMetadata = card.getCardMetadata();
        if (player.getCardResourcesCount().isEmpty() && cardMetadata != null && !CollectionUtils.isEmpty(cardMetadata.getResourcesOnBuild())) {
            PutResourceOnBuild putResourceOnBuild = cardMetadata.getResourcesOnBuild().get(0);
            if (putResourceOnBuild.getParamId() == InputFlag.CEOS_FAVORITE_PUT_RESOURCES.getId()) {
                //TODO should return  more if there is usable good card
                return CardProjection.NOT_USABLE;
            }
        }
        if (card.getCardMetadata().getCardAction() == CardAction.SYNTHETIC_CATASTROPHE) {
            List<Card> playedRedCards = player.getPlayed().getCards().stream().map(cardService::getCard).filter(c -> c.getColor() == CardColor.RED)
                    .collect(Collectors.toList());
            if (playedRedCards.isEmpty()) {
                //TODO should return  more if there is usable good card
                return CardProjection.NOT_USABLE;
            }
        }

        CardProjection extraChanceModifier = CardProjection.usable(1);

        if (!CollectionUtils.isEmpty(card.getTemperatureRequirement())) {
            extraChanceModifier = getChanceReductionBasedOnRequirement(game.getPlanet(), player, GlobalParameter.TEMPERATURE, card.getTemperatureRequirement());
        } else if (!CollectionUtils.isEmpty(card.getOxygenRequirement())) {
            extraChanceModifier = getChanceReductionBasedOnRequirement(game.getPlanet(), player, GlobalParameter.OXYGEN, card.getOxygenRequirement());
        } else if (card.getOceanRequirement() != null) {
            OceanRequirement oceanRequirement = card.getOceanRequirement();

            int revealedOceansCount = game.getPlanet().getRevealedOceans().size();

            if (revealedOceansCount < oceanRequirement.getMinValue()) {
                extraChanceModifier = CardProjection.usable((float) revealedOceansCount / game.getPlanet().getOceans().size());
            } else if (revealedOceansCount > oceanRequirement.getMaxValue()) {
                extraChanceModifier = CardProjection.NOT_USABLE;
            }
        }

        Class<? extends Card> clazz = card.getClass();
        if (clazz == PrivateInvestorBeach.class) {
            if (game.getMilestones().stream().allMatch(milestone -> milestone.isAchieved() && !milestone.isAchieved(player))) {
                extraChanceModifier = CardProjection.NOT_USABLE;
            }
        }

        if (clazz == AdvancedEcosystems.class) {
            int uniqueTagsPlayed = cardService.countUniquePlayedTags(player, Set.of(Tag.PLANT, Tag.MICROBE, Tag.ANIMAL));

            extraChanceModifier = CardProjection.usable((float) uniqueTagsPlayed / 3);
        }

        if (clazz == Crater.class) {
            int eventsPlayed = cardService.countPlayedTags(player, Set.of(Tag.EVENT));

            extraChanceModifier = CardProjection.usable((float) eventsPlayed / 3);
        }

        if (!extraChanceModifier.isUsable()) {
            return extraChanceModifier;
        }

        if (card.getColor() == CardColor.GREEN) {
            player.setBuilds(List.of(new BuildDto(BuildType.GREEN)));
        } else {
            player.setBuilds(List.of(new BuildDto(BuildType.BLUE_RED)));
        }

        MarsGame stateAfterPlayingTheCard = aiBuildProjectService.projectBuildCardNoRequirements(game, player, card);

        float projectedChance = deepNetwork.testState(stateAfterPlayingTheCard, stateAfterPlayingTheCard.getPlayerByUuid(player.getUuid()));

        return CardProjection.usable((projectedChance - initialChance) < 0 ? (projectedChance - initialChance) : extraChanceModifier.getChance() * (projectedChance - initialChance));
    }

    private CardProjection getChanceReductionBasedOnRequirement(Planet planet, Player player, GlobalParameter parameter, List<ParameterColor> requirement) {
        boolean canAmplifyRequirement = specialEffectsService.ownsSpecialEffect(player, SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT);
        if (canAmplifyRequirement) {
            requirement = amplifyRequirement(requirement);
        }
        if (parameter == GlobalParameter.OXYGEN && planet.isValidOxygen(requirement) || parameter == GlobalParameter.TEMPERATURE && planet.isValidTemperatute(requirement)) {
            return CardProjection.usable(1);
        }

        if (requirement.contains(ParameterColor.P) &&
                (canAmplifyRequirement || player.getHand().getCards().stream().map(cardService::getCard).noneMatch(card -> card.getSpecialEffects().contains(SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT)))) {
            return CardProjection.NOT_USABLE;
        } else {
            return CardProjection.usable(planet.getParameterProportionTillMinColor(parameter, ParameterColor.values()[requirement.stream().mapToInt(Enum::ordinal).min().orElse(0)]));
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
