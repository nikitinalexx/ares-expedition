package com.terraforming.ares.services.policyai.input;

import com.terraforming.ares.cards.green.EosChasmaNationalPark;
import com.terraforming.ares.model.Card;
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
public class EosChasmaNationalParkInputEffect extends AbstractPolicyProcessor implements InputGeneratorEffect {

    @Override
    public boolean isPresent(PolicyRecord record, TableContext tableContext, Card card, Map<Integer, List<Integer>> input) {
        return card.getClass() == EosChasmaNationalPark.class;
    }

    @Override
    public void generateAndApply(PolicyRecord record, TableContext tableContext, Map<Integer, List<Integer>> input, Card card) {
        List<Card> animalCards = ResourceHelper.getAnyAvailableAnimals(tableContext);

        if (animalCards.isEmpty()) {
            input.put(InputFlag.EOS_CHASMA_PUT_RESOURCE.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
            return;
        } else if (animalCards.size() == 1) {
            Card target = animalCards.getFirst();
            input.put(InputFlag.EOS_CHASMA_PUT_RESOURCE.getId(), List.of(target.getId()));
            rolloutAnimal(target.getClass(), record, 1);
            return;
        }

        Map<HeadAction, Card> actionToCard = new HashMap<>();
        animalCards.forEach(c -> actionToCard.put(ActionInputService.getAnimalTargetAction(c), c));
        List<HeadAction> validActions = new ArrayList<>(actionToCard.keySet());

        record.animalPutCount = 1;
        HeadAction best = predict(record, validActions);
        record.animalPutCount = 0;

        Card target = actionToCard.get(best);
        input.put(InputFlag.EOS_CHASMA_PUT_RESOURCE.getId(), List.of(target.getId()));
        rolloutAnimal(target.getClass(), record, 1);
    }

}
