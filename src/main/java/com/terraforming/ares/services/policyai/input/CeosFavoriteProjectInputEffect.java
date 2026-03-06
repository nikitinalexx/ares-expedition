package com.terraforming.ares.services.policyai.input;

import com.terraforming.ares.cards.red.CeosFavoriteProject;
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
public class CeosFavoriteProjectInputEffect extends AbstractPolicyProcessor implements InputGeneratorEffect {

    @Override
    public boolean isPresent(PolicyRecord record, TableContext tableContext, Card card, Map<Integer, List<Integer>> input) {
        return card.getClass() == CeosFavoriteProject.class;
    }

    @Override
    public void generateAndApply(PolicyRecord record, TableContext tableContext, Map<Integer, List<Integer>> input, Card card) {
        List<Card> collectingCards = ResourceHelper.getAnyAvailableCollectableResources(tableContext);

        Card target;
        if (collectingCards.size() == 1) {
            target = collectingCards.getFirst();
        } else {
            Map<HeadAction, Card> actionToCard = new LinkedHashMap<>();
            collectingCards.forEach(c -> {
                if (c.getCollectableResource() == CardCollectableResource.ANIMAL) {
                    actionToCard.put(ActionInputService.getAnimalTargetAction(c), c);
                } else if (c.getCollectableResource() == CardCollectableResource.MICROBE) {
                    actionToCard.put(ActionInputService.getMicrobeTargetAction(c), c);
                } else {
                    actionToCard.put(ActionInputService.getScienceResourceTargetAction(c), c);
                }
            });

            record.ceosFavoriteProjectEffect = true;
            HeadAction best = predict(record, new ArrayList<>(actionToCard.keySet()));
            record.ceosFavoriteProjectEffect = false;

            target = actionToCard.get(best);
        }

        input.put(InputFlag.CEOS_FAVORITE_PUT_RESOURCES.getId(), List.of(target.getId()));

        //no need to rollout
    }

}
