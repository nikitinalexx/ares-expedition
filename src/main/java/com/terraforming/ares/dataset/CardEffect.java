package com.terraforming.ares.dataset;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.corporations.*;
import com.terraforming.ares.cards.green.CommunicationsStreamlining;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

public enum CardEffect {
    AMPLIFY(Map.of(Inventrix.class, 1, AdaptationTechnology.class, 1), false),
    CARDS_PRICE(Map.of(ExocorpCorporation.class, 1, CompostingFactory.class, 1), true),
    CARD_FOR_SCIENCE(Map.of(ApolloIndustriesCorporation.class, 1, OlympusConference.class, 1), true),
    PHASE_REVEAL_BONUS(Map.of(NebuLabsCorporation.class, 2, CommunicationsStreamlining.class, 1), true),
    EVENT_DISCOUNT(Map.of(InterplanetaryCinematics.class, 2, MediaGroup.class, 5), true),
    CARD_DISCOUNT(Map.of(EarthCatapult.class, 2, ResearchOutpost.class, 1, HohmannTransferShipping.class, 1), true),
    CARD_PER_EVENT(Map.of(ImpactAnalysis.class, 1, RecycledDetritus.class, 2), true);

    @Getter
    private Map<Class<?>, Integer> cardsToEffectSignal;

    @Getter
    private boolean doesSummarize;

    CardEffect(Map<Class<?>, Integer> cardsToEffectSignal, boolean doesSummarize) {
        this.cardsToEffectSignal = cardsToEffectSignal;
        this.doesSummarize = doesSummarize;
    }

    public int getEffectSize(Set<Class<?>> playedCards) {

        if (!doesSummarize) {
            for (Class<?> playedCard : playedCards) {
                if (cardsToEffectSignal.containsKey(playedCard)) {
                    return cardsToEffectSignal.get(playedCard);
                }
            }
        } else {
            int initialSize = 0;

            for (Class<?> playedCard : playedCards) {
                if (cardsToEffectSignal.containsKey(playedCard)) {
                    initialSize += cardsToEffectSignal.get(playedCard);
                }
            }

            return initialSize;
        }

        return  0;
    }

}
