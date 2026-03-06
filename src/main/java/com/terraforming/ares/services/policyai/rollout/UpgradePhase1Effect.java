package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.action.HeadAction;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.util.List;
import java.util.Map;

public class UpgradePhase1Effect extends AbstractTagEffect {

    private final List<Integer> phaseInput;
    private final Card playedCard;

    public UpgradePhase1Effect(Card playedCard, Map<Integer, List<Integer>> params) {
        this.phaseInput = params.get(InputFlag.PHASE_UPGRADE_CARD.getId());
        this.playedCard = playedCard;
    }

    @Override
    public boolean isPresent() {
        return playedCard.getCardMetadata().getCardAction() == CardAction.UPDATE_PHASE_1_CARD;
    }

    @Override
    public void prepareContext(PolicyRecord ctx) {
        ctx.choosingPhase1Upgrade = true;
    }

    @Override
    public void clearContext(PolicyRecord ctx) {
        ctx.choosingPhase1Upgrade = false;
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
