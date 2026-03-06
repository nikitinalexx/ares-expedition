package com.terraforming.ares.services.policyai.input;

import com.terraforming.ares.cards.red.CryogenicShipment;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
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
public class CryogenicShipmentInputEffect extends AbstractPolicyProcessor implements InputGeneratorEffect {

    @Override
    public boolean isPresent(PolicyRecord record, TableContext tableContext, Card card, Map<Integer, List<Integer>> input) {
        return card.getClass() == CryogenicShipment.class;
    }

    @Override
    public void generateAndApply(PolicyRecord record, TableContext tableContext, Map<Integer, List<Integer>> input, Card card) {
        List<Card> animalMicrobeCards = ResourceHelper.getAnyAvailableAnimalsAndMicrobes(tableContext);

        if (animalMicrobeCards.isEmpty()) {
            input.put(InputFlag.CRYOGENIC_SHIPMENT_PUT_RESOURCE.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
            return;
        }

        Map<HeadAction, Card> actionToCard = new HashMap<>();
        animalMicrobeCards.forEach(c -> actionToCard.put(c.getCollectableResource() == CardCollectableResource.MICROBE ? ActionInputService.getMicrobeTargetAction(c) : ActionInputService.getAnimalTargetAction(c), c));

        Card target;
        if (actionToCard.size() == 1) {
            target = actionToCard.values().iterator().next();
        } else {
            record.cryogenicShipmentEffect = true;
            HeadAction best = predict(record, new ArrayList<>(actionToCard.keySet()));
            record.cryogenicShipmentEffect = false;
            target = actionToCard.get(best);
        }

        input.put(InputFlag.CRYOGENIC_SHIPMENT_PUT_RESOURCE.getId(), List.of(target.getId()));

        if (target.getCollectableResource() == CardCollectableResource.ANIMAL) {
            rolloutAnimal(target.getClass(), record, 2);
        } else {
            record.microbeResources[0][ActionRegistry.MICROBE_TARGET_TO_OFFSET.get(target.getClass())] += 3;
            microbeAdded(record, 3);
        }
    }
}
