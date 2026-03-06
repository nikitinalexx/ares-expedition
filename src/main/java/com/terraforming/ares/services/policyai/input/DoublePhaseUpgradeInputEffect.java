package com.terraforming.ares.services.policyai.input;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.rollout.EffectStateReader;
import com.terraforming.ares.services.policyai.service.AbstractPolicyProcessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class DoublePhaseUpgradeInputEffect extends AbstractPolicyProcessor implements InputGeneratorEffect {

    @Override
    public boolean isPresent(PolicyRecord record, TableContext tableContext, Card card, Map<Integer, List<Integer>> input) {
        return card.getCardMetadata().getCardAction() == CardAction.UPDATE_PHASE_CARD_TWICE;
    }

    @Override
    public void generateAndApply(PolicyRecord record, TableContext tableContext, Map<Integer, List<Integer>> input, Card card) {
        record.universalPhaseUpgradeCount = 2;
        generatePhaseUpgrade(record, input);
        List<Integer> firstUpgrade = input.get(InputFlag.PHASE_UPGRADE_CARD.getId());
        input.remove(InputFlag.PHASE_UPGRADE_CARD.getId());
        record.universalPhaseUpgradeCount--;

        generatePhaseUpgrade(record, input);
        record.universalPhaseUpgradeCount--;
        List<Integer> secondUpgrade = input.get(InputFlag.PHASE_UPGRADE_CARD.getId());

        List<Integer> result = (Objects.equals(firstUpgrade.getFirst(), secondUpgrade.getFirst())) ? firstUpgrade : (List.of(firstUpgrade.getFirst(), secondUpgrade.getFirst()));

        input.put(InputFlag.PHASE_UPGRADE_CARD.getId(), result);
    }
}
