package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.cards.blue.BacterialAggregates;
import com.terraforming.ares.cards.blue.Decomposers;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.policyai.ResourceHelper;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.action.HeadAction;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;

public class AstrofarmEffect extends AbstractTagEffect {

    private final List<Integer> putResource;
    private final CardService cardService;
    private final Player player;

    public AstrofarmEffect(Map<Integer, List<Integer>> params, CardService cardService, Player player) {
        this.putResource = params.get(InputFlag.ASTROFARM_PUT_RESOURCE.getId());
        this.cardService = cardService;
        this.player = player;
    }

    @Override
    public boolean isPresent() {
        return !CollectionUtils.isEmpty(putResource) && putResource.getFirst() != InputFlag.SKIP_ACTION.getId();
    }

    @Override
    public void prepareContext(PolicyRecord ctx) {
        ctx.microbePutCount = 2;
    }

    @Override
    public void clearContext(PolicyRecord ctx) {
        ctx.microbePutCount = 0;
    }

    @Override
    public List<EffectDecision> resolveDecisions(EffectStateReader state) {
        Card target = cardService.getCard(putResource.getFirst());

        boolean alsoHasAnotherTarget = alsoHasAnotherMicrobeTarget(player, cardService, target, state);

        if (!alsoHasAnotherTarget) {
            return List.of();
        }

        return List.of(new EffectDecision() {
            @Override
            public HeadAction getChosenAction() {
                return ActionInputService.getMicrobeTargetAction(target);
            }

            @Override
            public void applyRollout(PolicyRecord record) {
                rolloutMicrobe(target.getClass(), record, 2);
            }
        });
    }

}
