package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.util.List;
import java.util.Map;

public class DoublePhaseUpgradeEffect extends AbstractTagEffect {

    private final List<Integer> phaseInput;
    private final Card playedCard;

    public DoublePhaseUpgradeEffect(Card playedCard, Map<Integer, List<Integer>> params) {
        this.phaseInput = params.get(InputFlag.PHASE_UPGRADE_CARD.getId());
        this.playedCard = playedCard;
    }

    @Override
    public boolean isPresent() {
        return playedCard.getCardMetadata().getCardAction() == CardAction.UPDATE_PHASE_CARD_TWICE;
    }

    @Override
    public void prepareContext(PolicyRecord record) {
        record.universalPhaseUpgradeCount = 2;
    }

    @Override
    public void clearContext(PolicyRecord record) {
        record.universalPhaseUpgradeCount = 0;
    }

    @Override
    public List<EffectDecision> resolveDecisions(EffectStateReader state) {
        int firstUpgrade = phaseInput.get(0);
        int secondUpgrade = phaseInput.get(1);

        EffectDecision first = new EffectDecision() {
            @Override
            public int getChosenAction() {
                return ActionInputService.choosePhaseUpgradeIndex(firstUpgrade);
            }

            @Override
            public void applyRollout(PolicyRecord record) {
                record.universalPhaseUpgradeCount--;
                record.setPhaseUpgrade(firstUpgrade);
            }
        };

        EffectDecision second = new EffectDecision() {
            @Override
            public int getChosenAction() {
                return ActionInputService.choosePhaseUpgradeIndex(secondUpgrade);
            }

            @Override
            public void applyRollout(PolicyRecord record) {
                record.universalPhaseUpgradeCount--;
                record.setPhaseUpgrade(secondUpgrade);
            }
        };

        return List.of(first, second);
    }

}
