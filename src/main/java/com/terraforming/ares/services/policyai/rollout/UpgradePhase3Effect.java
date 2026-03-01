package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.util.List;
import java.util.Map;

public class UpgradePhase3Effect extends AbstractTagEffect {

    private final List<Integer> phaseInput;
    private final Card playedCard;

    public UpgradePhase3Effect(Card playedCard, Map<Integer, List<Integer>> params) {
        this.phaseInput = params.get(InputFlag.PHASE_UPGRADE_CARD.getId());
        this.playedCard = playedCard;
    }

    @Override
    public boolean isPresent() {
        return playedCard.getCardMetadata().getCardAction() == CardAction.COMMUNICATIONS_STREAMLINING;
    }

    @Override
    public void prepareContext(PolicyRecord ctx) {
        ctx.choosingPhase3Upgrade = true;
    }

    @Override
    public void clearContext(PolicyRecord ctx) {
        ctx.choosingPhase3Upgrade = false;
    }

    @Override
    public List<EffectDecision> resolveDecisions(EffectStateReader state) {
        int phaseUpgradeIndex = phaseInput.getFirst();

        return List.of(new EffectDecision() {
            @Override
            public int getChosenAction() {
                return ActionInputService.choosePhaseUpgradeIndex(phaseUpgradeIndex);
            }

            @Override
            public void applyRollout(PolicyRecord record) {
                record.setPhaseUpgrade(phaseUpgradeIndex);
            }
        });
    }

}
