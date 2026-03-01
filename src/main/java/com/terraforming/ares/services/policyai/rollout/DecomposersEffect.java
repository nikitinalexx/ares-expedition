package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.cards.blue.Decomposers;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class DecomposersEffect extends AbstractTagEffect {

    private final List<Integer> takeMicrobes;
    private final List<Integer> takeCards;

    public DecomposersEffect(Map<Integer, List<Integer>> params) {
        this.takeMicrobes = params.get(InputFlag.DECOMPOSERS_TAKE_MICROBE.getId());
        this.takeCards = params.get(InputFlag.DECOMPOSERS_TAKE_CARD.getId());
    }

    @Override
    public boolean isPresent() {
        return !CollectionUtils.isEmpty(takeMicrobes) || !CollectionUtils.isEmpty(takeCards);
    }

    @Override
    public void prepareContext(PolicyRecord record) {
        record.decomposersActivationsLeft = (byte) totalEffectCount();
    }

    @Override
    public void clearContext(PolicyRecord record) {
        record.decomposersActivationsLeft = 0;
    }

    @Override
    public List<EffectDecision> resolveDecisions(EffectStateReader state) {
        int microbes = microbeCount();
        int cards = cardCount();
        int total = totalEffectCount();

        if (total == 1 && state.getMicrobeCount(Decomposers.class) == 0) {
            return List.of(); // side effect — микроб кладётся без решения
        }

        if (total == 1) {
            return List.of(microbes > 0 ? takeMicrobeDecision() : takeCardDecision());
        }

        if (microbes == 2) return List.of(takeMicrobeDecision(), takeMicrobeDecision());
        if (cards == 2) return List.of(takeCardDecision(), takeCardDecision());

        // По одному каждого
        boolean microbeFirst = ThreadLocalRandom.current().nextBoolean();
        // Особый случай: берём карту первой, но микробов ровно 1 —
        // после rollout takeCard микробов станет 0, второй выбор вынужденный
        if (!microbeFirst && state.getMicrobeCount(Decomposers.class) == 1) {
            return List.of(takeCardDecision(), takeMicrobeDecision());
        }

        return microbeFirst
                ? List.of(takeMicrobeDecision(), takeCardDecision())
                : List.of(takeCardDecision(), takeMicrobeDecision());
    }

    private EffectDecision takeMicrobeDecision() {
        return new EffectDecision() {
            @Override
            public int getChosenAction() {
                return ActionInputService.decomposersPickMicrobe();
            }

            @Override
            public void applyRollout(PolicyRecord r) {
                rolloutMicrobe(Decomposers.class, r, 1);
                r.decomposersActivationsLeft--;
            }
        };
    }

    private EffectDecision takeCardDecision() {
        return new EffectDecision() {
            @Override
            public int getChosenAction() {
                return ActionInputService.decomposersPickCard();
            }

            @Override
            public void applyRollout(PolicyRecord r) {
                r.addGenericCardToHand();
                microbeRemoved(r);
                r.decomposersActivationsLeft--;
            }
        };
    }

    private int microbeCount() {
        return CollectionUtils.isEmpty(takeMicrobes) ? 0 : takeMicrobes.getFirst();
    }

    private int cardCount() {
        return CollectionUtils.isEmpty(takeCards) ? 0 : takeCards.getFirst();
    }

    private int totalEffectCount() {
        return microbeCount() + cardCount();
    }

    @Override
    public void applySideEffect(PolicyRecord record) {
        rolloutMicrobe(Decomposers.class, record, 1);
    }

}
