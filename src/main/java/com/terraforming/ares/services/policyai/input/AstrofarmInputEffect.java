package com.terraforming.ares.services.policyai.input;

import com.terraforming.ares.cards.green.Astrofarm;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.ResourceHelper;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.action.ActionRegistry;
import com.terraforming.ares.services.policyai.action.HeadAction;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.rollout.EffectStateReader;
import com.terraforming.ares.services.policyai.service.AbstractPolicyProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AstrofarmInputEffect extends AbstractPolicyProcessor implements InputGeneratorEffect {

    @Override
    public boolean isPresent(PolicyRecord record, TableContext tableContext, Card card, Map<Integer, List<Integer>> input) {
        return card.getClass() == Astrofarm.class;
    }

    @Override
    public void generateAndApply(PolicyRecord record, TableContext tableContext, Map<Integer, List<Integer>> input, Card card) {
        List<Card> microbeCards = ResourceHelper.getAnyAvailableMicrobes(tableContext);

        if (microbeCards.isEmpty()) {
            input.put(InputFlag.ASTROFARM_PUT_RESOURCE.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
            return;
        } else if (microbeCards.size() == 1) {
            input.put(InputFlag.ASTROFARM_PUT_RESOURCE.getId(), List.of(microbeCards.getFirst().getId()));
            return;
        }

        List<HeadAction> validActions = new ArrayList<>();
        Map<HeadAction, Card> actionToCard = new HashMap<>();
        microbeCards.forEach(c -> {
            HeadAction action = ActionInputService.getMicrobeTargetAction(c);
            actionToCard.put(action, c);
            validActions.add(action);
        });

        record.microbePutCount = 2;
        HeadAction best = predict(record, validActions);
        record.microbePutCount = 0;

        Card target = actionToCard.get(best);
        input.put(InputFlag.ASTROFARM_PUT_RESOURCE.getId(), List.of(target.getId()));
        record.microbeResources[0][ActionRegistry.MICROBE_TARGET_TO_OFFSET.get(target.getClass())] += 2;
        microbeAdded(record, 2);
    }

}
