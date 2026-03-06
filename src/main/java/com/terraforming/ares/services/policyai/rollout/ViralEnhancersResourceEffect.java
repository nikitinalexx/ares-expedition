package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.action.HeadAction;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ViralEnhancersResourceEffect extends AbstractTagEffect {

    private final List<Integer> putResources;
    private final CardService cardService;

    public ViralEnhancersResourceEffect(Map<Integer, List<Integer>> params, CardService cardService) {
        this.putResources = params.get(InputFlag.VIRAL_ENHANCERS_PUT_RESOURCE.getId());
        this.cardService = cardService;
    }

    @Override
    public boolean isPresent() {
        return !CollectionUtils.isEmpty(putResources);
    }

    @Override
    public void prepareContext(PolicyRecord record) {
        record.viralEnhancersActivationsLeft = (byte) putResources.size();
    }

    @Override
    public void clearContext(PolicyRecord record) {
        record.viralEnhancersActivationsLeft = 0;
    }

    @Override
    public List<EffectDecision> resolveDecisions(EffectStateReader state) {
        // Старый AI кладёт все ресурсы на одну и ту же карту
        Card targetCard = cardService.getCard(putResources.getFirst());

        return Collections.nCopies(putResources.size(), new EffectDecision() {
            @Override
            public HeadAction getChosenAction() {
                return resolveAction(targetCard);
            }

            @Override
            public void applyRollout(PolicyRecord record) {
                rolloutPutResource(targetCard, record);
                record.viralEnhancersActivationsLeft--;
            }
        });
    }

    private HeadAction resolveAction(Card card) {
        if (card.getCollectableResource() == CardCollectableResource.ANIMAL) {
            return ActionInputService.getAnimalTargetAction(card);
        } else {
            return ActionInputService.getMicrobeTargetAction(card);
        }
    }

    private void rolloutPutResource(Card targetCard, PolicyRecord record) {
        if (targetCard.getCollectableResource() == CardCollectableResource.ANIMAL) {
            rolloutAnimal(targetCard.getClass(), record, 1);
        } else {
            rolloutMicrobe(targetCard.getClass(), record, 1);
        }
    }
}
