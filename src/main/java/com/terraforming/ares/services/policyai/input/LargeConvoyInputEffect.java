package com.terraforming.ares.services.policyai.input;

import com.terraforming.ares.cards.red.LargeConvoy;
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
public class LargeConvoyInputEffect extends AbstractPolicyProcessor implements InputGeneratorEffect {

    @Override
    public boolean isPresent(PolicyRecord record, TableContext tableContext, Card card, Map<Integer, List<Integer>> input) {
        return card.getClass() == LargeConvoy.class;
    }

    @Override
    public void generateAndApply(PolicyRecord record, TableContext tableContext, Map<Integer, List<Integer>> input, Card card) {
        List<Card> animalCards = ResourceHelper.getAnyAvailableAnimals(tableContext);

        if (animalCards.isEmpty()) {
            input.put(InputFlag.LARGE_CONVOY_PICK_PLANT.getId(), List.of());
            record.plants[0] += 5;
            return;
        }

        List<HeadAction> validActions = new ArrayList<>();
        Map<HeadAction, Card> actionToCard = new HashMap<>();
        animalCards.forEach(c -> {
            HeadAction action = ActionInputService.getAnimalTargetAction(c);
            actionToCard.put(action, c);
            validActions.add(action);
        });
        validActions.add(ActionInputService.takePlantTargetAction());

        record.largeConvoyEffect = true;
        HeadAction best = predict(record, validActions);

        if (best == ActionInputService.takePlantTargetAction()) {
            input.put(InputFlag.LARGE_CONVOY_PICK_PLANT.getId(), List.of());
            record.plants[0] += 5;
        } else {
            Card target = actionToCard.get(best);
            input.put(InputFlag.LARGE_CONVOY_ADD_ANIMAL.getId(), List.of(target.getId()));
            rolloutAnimal(target.getClass(), record, 3);
        }
        record.largeConvoyEffect = false;
    }
}
