package com.terraforming.ares.services.policyai.input;

import com.terraforming.ares.cards.red.ImportedHydrogen;
import com.terraforming.ares.cards.red.SyntheticCatastrophe;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.ResourceHelper;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.action.HeadAction;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.service.AbstractPolicyProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ImportedHydrogenInputEffect extends AbstractPolicyProcessor implements InputGeneratorEffect {

    @Override
    public boolean isPresent(PolicyRecord record, TableContext tableContext, Card card, Map<Integer, List<Integer>> input) {
        return card.getClass() == ImportedHydrogen.class;
    }

    @Override
    public void generateAndApply(PolicyRecord record, TableContext tableContext, Map<Integer, List<Integer>> input, Card card) {
        List<Card> microbeAnimalCards = ResourceHelper.getAnyAvailableAnimalsAndMicrobes(tableContext);

        if (microbeAnimalCards.isEmpty()) {
            input.put(InputFlag.IMPORTED_HYDROGEN_PICK_PLANT.getId(), List.of());
            return;
        }

        Map<HeadAction, Card> actionToCard = new LinkedHashMap<>();
        microbeAnimalCards.forEach(c -> {
            if (c.getCollectableResource() == CardCollectableResource.ANIMAL) {
                actionToCard.put(ActionInputService.getAnimalTargetAction(c), c);
            } else {
                actionToCard.put(ActionInputService.getMicrobeTargetAction(c), c);
            }
        });

        List<HeadAction> validActions = new ArrayList<>(actionToCard.keySet());
        validActions.add(ActionInputService.takePlantTargetAction());

        record.importedHydrogenEffect = true;
        HeadAction best = predict(record, validActions);
        record.importedHydrogenEffect = false;

        if (best == ActionInputService.takePlantTargetAction()) {
            input.put(InputFlag.IMPORTED_HYDROGEN_PICK_PLANT.getId(), List.of());
        } else {
            Card target = actionToCard.size() == 1
                    ? actionToCard.values().iterator().next()
                    : actionToCard.get(best);
            input.put(InputFlag.IMPORTED_HYDROGEN_PUT_RESOURCE.getId(), List.of(target.getId()));
        }
    }

}
