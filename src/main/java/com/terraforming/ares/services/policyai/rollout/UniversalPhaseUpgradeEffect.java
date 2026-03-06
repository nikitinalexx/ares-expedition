package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.action.HeadAction;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.util.List;
import java.util.Map;

public class UniversalPhaseUpgradeEffect extends AbstractTagEffect {

    private final List<Integer> phaseInput;
    private final Card playedCard;

    public UniversalPhaseUpgradeEffect(Card playedCard, Map<Integer, List<Integer>> params) {
        this.phaseInput = params.get(InputFlag.PHASE_UPGRADE_CARD.getId());
        this.playedCard = playedCard;
    }

    @Override
    public boolean isPresent() {
        CardAction cardAction = playedCard.getCardMetadata().getCardAction();
        return cardAction == CardAction.CRYOGENIC_SHIPMENT
                || cardAction == CardAction.TOPOGRAPHIC_MAPPING
                || cardAction == CardAction.UPDATE_PHASE_CARD
                || cardAction == CardAction.SOFTWARE_STREAMLINING;
    }

    @Override
    public void prepareContext(PolicyRecord ctx) {
        ctx.universalPhaseUpgradeCount = 1;
    }

    @Override
    public void clearContext(PolicyRecord ctx) {
        ctx.universalPhaseUpgradeCount = 0;
    }

    @Override
    public List<EffectDecision> resolveDecisions(EffectStateReader state) {
        int phaseUpgradeIndex = phaseInput.getFirst();

        return List.of(new EffectDecision() {
            @Override
            public HeadAction getChosenAction() {
                return ActionInputService.choosePhaseUpgradeAction(phaseUpgradeIndex);
            }

            @Override
            public void applyRollout(PolicyRecord record) {
                record.setPhaseUpgrade(phaseUpgradeIndex);
            }
        });
    }

}
