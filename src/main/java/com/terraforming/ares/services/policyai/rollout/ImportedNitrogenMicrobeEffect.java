package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImportedNitrogenMicrobeEffect extends AbstractTagEffect {

    private final List<Integer> addMicrobes;
    private final CardService cardService;
    private final Player player;

    public ImportedNitrogenMicrobeEffect(Map<Integer, List<Integer>> params, CardService cardService, Player player) {
        this.addMicrobes = params.get(InputFlag.IMPORTED_NITROGEN_ADD_MICROBES.getId());
        this.cardService = cardService;
        this.player = player;
    }

    @Override
    public boolean isPresent() {
        return !CollectionUtils.isEmpty(addMicrobes) && addMicrobes.getFirst() != InputFlag.SKIP_ACTION.getId();
    }

    @Override
    public void prepareContext(PolicyRecord ctx) {
        ctx.microbePutCount = 3;
    }

    @Override
    public void clearContext(PolicyRecord ctx) {
        ctx.microbePutCount = 0;
    }

    @Override
    public List<EffectDecision> resolveDecisions(EffectStateReader state) {
        Card target = cardService.getCard(addMicrobes.getFirst());

        boolean alsoHasAnotherTarget = alsoHasAnotherMicrobeAnimalTarget(player, cardService, target, state);

        if (!alsoHasAnotherTarget) {
            return List.of();
        }

        return List.of(new EffectDecision() {
            @Override
            public int getChosenAction() {
                return ActionInputService.getMicrobeTargetActionIndex(target);
            }

            @Override
            public void applyRollout(PolicyRecord record) {
                rolloutMicrobe(target.getClass(), record, 3);
            }
        });
    }

}
