package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.cards.blue.BacterialAggregates;
import com.terraforming.ares.cards.blue.FilterFeeders;
import com.terraforming.ares.cards.buffedCorporations.BuffedArclightCorporation;
import com.terraforming.ares.cards.corporations.ArclightCorporation;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.awards.AwardType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.policyai.action.ActionRegistry;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

public abstract class AbstractTagEffect implements TagEffect {

    @Override
    public void prepareContext(PolicyRecord record) {
    }

    @Override
    public void clearContext(PolicyRecord record) {
    }

    protected void rolloutMicrobe(Class<?> target, PolicyRecord record, int count) {
        if (target == BacterialAggregates.class) {
            int currentMicrobes = record.microbeResources[0][ActionRegistry.MICROBE_TARGET_TO_OFFSET.get(target)];
            int maxCanAdd = (5 - currentMicrobes);
            count = Math.min(maxCanAdd, count);
        }
        record.microbeResources[0][ActionRegistry.MICROBE_TARGET_TO_OFFSET.get(target)] += (byte) count;

        int total = count;
        if (record.hasPlayedBlueCard(FilterFeeders.class)) {
            total++;
            record.animalResources[0][ActionRegistry.ANIMAL_TARGET_TO_OFFSET.get(FilterFeeders.class)]++;
        }
        record.addAwardProgress(AwardType.COLLECTOR, total);
    }

    protected void microbeRemoved(PolicyRecord record) {
        record.addAwardProgress(AwardType.COLLECTOR, -1);
    }

    protected void rolloutAnimal(Class<?> target, PolicyRecord record, int count) {
        if (target == ArclightCorporation.class || target == BuffedArclightCorporation.class) {
            record.arclightResources[0] += (byte) count;
        } else {
            record.animalResources[0][ActionRegistry.ANIMAL_TARGET_TO_OFFSET.get(target)] += (byte) count;
        }
        record.addAwardProgress(AwardType.COLLECTOR, count);
    }

    protected boolean alsoHasAnotherMicrobeTarget(Player player, CardService cardService, Card target, EffectStateReader state) {
        boolean alsoHasAnotherTarget = false;
        for (Integer playedCardId : player.getPlayed().getCards()) {
            Card playedCard = cardService.getCard(playedCardId);
            if (playedCard.getCollectableResource() == CardCollectableResource.MICROBE && playedCard != target && !(playedCard.getClass() == BacterialAggregates.class && state.getMicrobeCount(BacterialAggregates.class) == 5)) {
                alsoHasAnotherTarget = true;
            }
        }
        return alsoHasAnotherTarget;
    }

    protected boolean alsoHasAnotherAnimalTarget(Player player, CardService cardService, Card target, EffectStateReader state) {
        boolean alsoHasAnotherTarget = false;
        for (Integer playedCardId : player.getPlayed().getCards()) {
            Card playedCard = cardService.getCard(playedCardId);
            if (playedCard.getCollectableResource() == CardCollectableResource.ANIMAL && playedCard != target) {
                alsoHasAnotherTarget = true;
            }
        }
        return alsoHasAnotherTarget;
    }

    protected boolean alsoHasAnotherMicrobeAnimalTarget(Player player, CardService cardService, Card target, EffectStateReader state) {
        boolean alsoHasAnotherTarget = false;
        for (Integer playedCardId : player.getPlayed().getCards()) {
            Card playedCard = cardService.getCard(playedCardId);
            if ((playedCard.getCollectableResource() == CardCollectableResource.MICROBE || playedCard.getCollectableResource() == CardCollectableResource.ANIMAL) && playedCard != target
                    && !(playedCard.getClass() == BacterialAggregates.class && state.getMicrobeCount(BacterialAggregates.class) == 5)) {
                alsoHasAnotherTarget = true;
            }
        }
        return alsoHasAnotherTarget;
    }


}
