package com.terraforming.ares.services.policyai.input;

import com.terraforming.ares.cards.red.SyntheticCatastrophe;
import com.terraforming.ares.model.Card;
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
public class SyntheticCatastropheInputEffect extends AbstractPolicyProcessor implements InputGeneratorEffect {

    @Override
    public boolean isPresent(PolicyRecord record, TableContext tableContext, Card card, Map<Integer, List<Integer>> input) {
        return card.getClass() == SyntheticCatastrophe.class;
    }

    @Override
    public void generateAndApply(PolicyRecord record, TableContext tableContext, Map<Integer, List<Integer>> input, Card card) {
        List<Card> builtRedCards = ResourceHelper.getBuiltRedCards(tableContext);

        Card target;
        if (builtRedCards.size() == 1) {
            target = builtRedCards.getFirst();
        } else {
            Map<HeadAction, Card> actionToCard = new LinkedHashMap<>();
            builtRedCards.forEach(c -> actionToCard.put(ActionInputService.getRedCardTargetAction(c), c));

            record.syntheticCatastropheEffect = true;
            HeadAction best = predict(record, new ArrayList<>(actionToCard.keySet()));
            record.syntheticCatastropheEffect = false;

            target = actionToCard.get(best);
        }

        input.put(InputFlag.SYNTHETIC_CATASTROPHE_CARD.getId(), List.of(target.getId()));
    }

}
