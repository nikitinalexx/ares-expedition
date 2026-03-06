package com.terraforming.ares.services.policyai.input;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.rollout.EffectStateReader;
import com.terraforming.ares.services.policyai.service.AbstractPolicyProcessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class UniversalPhaseUpgradeInputEffect extends AbstractPolicyProcessor implements InputGeneratorEffect {

    @Override
    public boolean isPresent(PolicyRecord record, TableContext tableContext, Card card, Map<Integer, List<Integer>> input) {
        CardAction cardAction = card.getCardMetadata().getCardAction();
        return cardAction == CardAction.CRYOGENIC_SHIPMENT
                || cardAction == CardAction.TOPOGRAPHIC_MAPPING
                || cardAction == CardAction.UPDATE_PHASE_CARD
                || cardAction == CardAction.SOFTWARE_STREAMLINING;
    }

    @Override
    public void generateAndApply(PolicyRecord record, TableContext tableContext, Map<Integer, List<Integer>> input, Card card) {
        record.universalPhaseUpgradeCount = 1;
        generatePhaseUpgrade(record, input);
        record.universalPhaseUpgradeCount = 0;
    }
}












