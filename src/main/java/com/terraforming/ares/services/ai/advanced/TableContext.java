package com.terraforming.ares.services.ai.advanced;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.ai.advanced.features.hand.HandEncoderHelperService;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public class TableContext {
    private final MarsGame game;
    private final Player player;
    private final Player opponent;

    private final CardService cardService;
    private final WinPointsService winPointsService;
    private final DraftCardsService draftCardsService;
    private final HandEncoderHelperService handEncoderHelperService;

    private final Map<Tag, Long> playedTagToCount;
    private final Map<CardAction, Long> playedCardActions;
    private final Set<Class<?>> playedCardClasses;
    private final Set<SpecialEffect> specialEffects;

    private final List<Card> hand;
    private final List<Card> playedCards;
    private final Set<Class<?>> handCardClasses;
    private final Map<CardAction, Long> handCardActions;

    private final int[] handTagCounts;
    private final boolean canAmplifyOxygenOrTemperature;
    private final float minRedTemperatureAvailabilityProgress;
    private final float minYellowTemperatureAvailabilityProgress;


    public TableContext(MarsGame game, Player player, Player opponent, CardService cardService, WinPointsService winPointsService, DraftCardsService draftCardsService, SpecialEffectsService specialEffectsService, HandEncoderHelperService handEncoderHelperService) {
        this.game = game;
        this.player = player;
        this.opponent = opponent;
        this.cardService = cardService;
        this.winPointsService = winPointsService;
        this.draftCardsService = draftCardsService;
        this.handEncoderHelperService = handEncoderHelperService;
        this.playedTagToCount = cardService.countPlayedTagsAsMap(player);

        this.playedCardActions = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .map(card -> card.getCardMetadata().getCardAction())
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ));



        this.playedCardClasses = player.getPlayed().getCards().stream().map(cardService::getCard).map(Card::getClass).collect(Collectors.toSet());
        this.specialEffects = specialEffectsService.getPlayerSpecialEffects(player);

        this.hand = player.getHand().getCards().stream().map(cardService::getCard).collect(Collectors.toList());
        this.playedCards = player.getPlayed().getCards().stream().map(cardService::getCard).collect(Collectors.toList());
        this.handCardClasses = hand.stream().map(Card::getClass).collect(Collectors.toSet());

        this.handCardActions =
                hand.stream()
                        .map(card -> card.getCardMetadata().getCardAction())
                        .filter(Objects::nonNull)
                        .collect(Collectors.groupingBy(
                                Function.identity(),
                                Collectors.counting()
                        ));

        handTagCounts = new int[11];
        for (Card card : hand) {
            List<Tag> tags = card.getTags();
            for (Tag tag : tags) {
                handTagCounts[tag.ordinal()]++;
            }
        }

        this.canAmplifyOxygenOrTemperature = specialEffectsService.ownsSpecialEffect(player, SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT);
        this.minRedTemperatureAvailabilityProgress = minRedTemperatureAvailabilityProgressInternal();
        this.minYellowTemperatureAvailabilityProgress = minYellowTemperatureAvailabilityProgress();
    }

    private float minRedTemperatureAvailabilityProgressInternal() {
        int currentLevel = game.getPlanet().getCurrentLevel(GlobalParameter.TEMPERATURE);
        int targetTemperatureLevel = getTemperatureStepFromColor(canAmplifyOxygenOrTemperature ? ParameterColor.P : ParameterColor.R, false);

        float availabilityProgress;
        if (currentLevel >= targetTemperatureLevel) {
            availabilityProgress = 1.0f; // Можно строить
        } else {
            availabilityProgress = (float) currentLevel / targetTemperatureLevel;
        }
        return availabilityProgress;
    }

    private float minYellowTemperatureAvailabilityProgress() {
        int currentLevel = game.getPlanet().getCurrentLevel(GlobalParameter.TEMPERATURE);
        int targetTemperatureLevel = getTemperatureStepFromColor(canAmplifyOxygenOrTemperature ? ParameterColor.R : ParameterColor.Y, false);

        float availabilityProgress;
        if (currentLevel >= targetTemperatureLevel) {
            availabilityProgress = 1.0f; // Можно строить
        } else {
            availabilityProgress = (float) currentLevel / targetTemperatureLevel;
        }
        return availabilityProgress;
    }

    public int getTemperatureStepFromColor(ParameterColor color, boolean isMaxBoundary) {
        return switch (color) {
            case P -> // Purple: 0-5 (Min=0, Max=5)
                    isMaxBoundary ? 5 : 0;
            case R -> // Red: 6-10 (Min=6, Max=10)
                    isMaxBoundary ? 10 : 6;
            case Y -> // Yellow: 11-15 (Min=11, Max=15)
                    isMaxBoundary ? 15 : 11;
            case W -> // White: 16-19 (Min=16, Max=19)
                    isMaxBoundary ? 19 : 16;
            // Если карта не имеет требования по цвету, или требование уже выполнено
        };
    }

    public int getOxygenStepFromColor(ParameterColor color, boolean isMaxBoundary) {
        return switch (color) {
            case P -> // Purple: 0-2 (Min=0, Max=2)
                    isMaxBoundary ? 2 : 0;
            case R -> // Red: 3-6 (Min=3, Max=6)
                    isMaxBoundary ? 6 : 3;
            case Y -> // Yellow: 7-11 (Min=7, Max=11)
                    isMaxBoundary ? 11 : 7;
            case W -> // White: 12-14 (Min=12, Max=14)
                    isMaxBoundary ? 14 : 12;
            // Если карта не имеет требования по цвету, или требование уже выполнено
        };
    }

}
