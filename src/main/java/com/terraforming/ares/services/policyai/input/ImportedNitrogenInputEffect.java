package com.terraforming.ares.services.policyai.input;

import com.terraforming.ares.cards.red.ImportedNitrogen;
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

import java.util.*;

@Component
public class ImportedNitrogenInputEffect extends AbstractPolicyProcessor implements InputGeneratorEffect {

    @Override
    public boolean isPresent(PolicyRecord record, TableContext tableContext, Card card, Map<Integer, List<Integer>> input) {
        return card.getClass() ==  ImportedNitrogen.class;
    }

    @Override
    public void generateAndApply(PolicyRecord record, TableContext tableContext, Map<Integer, List<Integer>> input, Card card) {
        List<Card> microbeCards = ResourceHelper.getAnyAvailableMicrobes(tableContext);
        List<Card> animalCards = ResourceHelper.getAnyAvailableAnimals(tableContext);

        record.microbePutCount = 3;
        record.animalPutCount = 2;

        // Микробы всегда первыми
        if (!microbeCards.isEmpty()) {
            Card target;
            if (microbeCards.size() == 1) {
                target = microbeCards.getFirst();
            } else {
                Map<HeadAction, Card> actionToCard = new HashMap<>();
                microbeCards.forEach(c -> actionToCard.put(ActionInputService.getMicrobeTargetAction(c), c));

                HeadAction best = predict(record, new ArrayList<>(actionToCard.keySet()));
                target = actionToCard.get(best);
            }
            input.put(InputFlag.IMPORTED_NITROGEN_ADD_MICROBES.getId(), List.of(target.getId()));
            record.microbeResources[0][ActionRegistry.MICROBE_TARGET_TO_OFFSET.get(target.getClass())] += 3;
            microbeAdded(record, 3);
        } else {
            input.put(InputFlag.IMPORTED_NITROGEN_ADD_MICROBES.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
        }
        record.microbePutCount = 0;

        if (!animalCards.isEmpty()) {
            Card target;
            if (animalCards.size() == 1) {
                target = animalCards.getFirst();
            } else {
                Map<HeadAction, Card> actionToCard = new HashMap<>();
                animalCards.forEach(c -> actionToCard.put(ActionInputService.getAnimalTargetAction(c), c));

                HeadAction best = predict(record, new ArrayList<>(actionToCard.keySet()));
                target = actionToCard.get(best);
            }

            input.put(InputFlag.IMPORTED_NITROGEN_ADD_ANIMALS.getId(), List.of(target.getId()));
            rolloutAnimal(target.getClass(), record, 2);
        } else {
            input.put(InputFlag.IMPORTED_NITROGEN_ADD_ANIMALS.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
        }
        record.animalPutCount = 0;
    }
}
