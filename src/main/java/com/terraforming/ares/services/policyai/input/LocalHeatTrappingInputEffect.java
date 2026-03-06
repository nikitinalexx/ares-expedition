package com.terraforming.ares.services.policyai.input;

import com.terraforming.ares.cards.red.LocalHeatTrapping;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.ResourceHelper;
import com.terraforming.ares.services.policyai.action.ActionInputService;
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
public class LocalHeatTrappingInputEffect extends AbstractPolicyProcessor implements InputGeneratorEffect {

    @Override
    public boolean isPresent(PolicyRecord record, TableContext tableContext, Card card, Map<Integer, List<Integer>> input) {
        return card.getClass() == LocalHeatTrapping.class;
    }

    @Override
    public void generateAndApply(PolicyRecord record, TableContext tableContext, Map<Integer, List<Integer>> input, Card card) {
        List<Card> animalMicrobes = ResourceHelper.getAnyAvailableAnimalsAndMicrobes(tableContext);

        if (animalMicrobes.isEmpty()) {
            input.put(InputFlag.LOCAL_HEAT_TRAPPING_PUT_RESOURCE.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
            return;
        } else if (animalMicrobes.size() == 1) {
            input.put(InputFlag.LOCAL_HEAT_TRAPPING_PUT_RESOURCE.getId(), List.of(animalMicrobes.getFirst().getId()));
            return;
        }

        Map<HeadAction, Card> actionToCard = new HashMap<>();
        animalMicrobes.forEach(c -> actionToCard.put(
                c.getCollectableResource() == CardCollectableResource.MICROBE
                        ? ActionInputService.getMicrobeTargetAction(c)
                        : ActionInputService.getAnimalTargetAction(c), c)
        );

        record.localHeatTrappingEffect = true;
        HeadAction best = predict(record, new ArrayList<>(actionToCard.keySet()));
        record.localHeatTrappingEffect = false;
        Card target = actionToCard.get(best);

        input.put(InputFlag.LOCAL_HEAT_TRAPPING_PUT_RESOURCE.getId(), List.of(target.getId()));
    }
}
