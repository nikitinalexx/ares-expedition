package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.action.HeadAction;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;

public class CryogenicShipmentResourceEffect extends AbstractTagEffect {

    private final List<Integer> putResource;
    private final CardService cardService;
    private final Player player;

    public CryogenicShipmentResourceEffect(Map<Integer, List<Integer>> params, CardService cardService, Player player) {
        this.putResource = params.get(InputFlag.CRYOGENIC_SHIPMENT_PUT_RESOURCE.getId());
        this.cardService = cardService;
        this.player = player;
    }

    @Override
    public boolean isPresent() {
        return !CollectionUtils.isEmpty(putResource) && putResource.getFirst() != InputFlag.SKIP_ACTION.getId();
    }

    @Override
    public void prepareContext(PolicyRecord ctx) {
        ctx.cryogenicShipmentEffect = true;
    }

    @Override
    public void clearContext(PolicyRecord ctx) {
        ctx.cryogenicShipmentEffect = false;
    }

    @Override
    public List<EffectDecision> resolveDecisions(EffectStateReader state) {
        Card target = cardService.getCard(putResource.getFirst());

        boolean alsoHasAnotherTarget = alsoHasAnotherMicrobeAnimalTarget(player, cardService, target, state);

        if (!alsoHasAnotherTarget) {
            return List.of();
        }

        return List.of(new EffectDecision() {
            @Override
            public HeadAction getChosenAction() {
                if (target.getCollectableResource() == CardCollectableResource.ANIMAL) {
                    return ActionInputService.getAnimalTargetAction(target);
                } else {
                    return ActionInputService.getMicrobeTargetAction(target);
                }
            }

            @Override
            public void applyRollout(PolicyRecord record) {
                if (target.getCollectableResource() == CardCollectableResource.ANIMAL) {
                    rolloutAnimal(target.getClass(), record, 2);
                } else {
                    rolloutMicrobe(target.getClass(), record, 3);
                }
            }
        });
    }

    @Override
    public void applySideEffect(PolicyRecord record) {
        Card target = cardService.getCard(putResource.getFirst());

        if (target.getCollectableResource() == CardCollectableResource.ANIMAL) {
            rolloutAnimal(target.getClass(), record, 2);
        } else {
            rolloutMicrobe(target.getClass(), record, 3);
        }
    }
}
