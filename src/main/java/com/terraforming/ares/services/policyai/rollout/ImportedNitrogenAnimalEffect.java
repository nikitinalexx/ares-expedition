package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;

public class ImportedNitrogenAnimalEffect extends AbstractTagEffect {

    private final List<Integer> addAnimals;
    private final CardService cardService;
    private final Player player;

    public ImportedNitrogenAnimalEffect(Map<Integer, List<Integer>> params, CardService cardService, Player player) {
        this.addAnimals = params.get(InputFlag.IMPORTED_NITROGEN_ADD_ANIMALS.getId());
        this.cardService = cardService;
        this.player = player;
    }

    @Override
    public boolean isPresent() {
        return !CollectionUtils.isEmpty(addAnimals) && addAnimals.getFirst() != InputFlag.SKIP_ACTION.getId();
    }

    @Override
    public void prepareContext(PolicyRecord ctx) {
        ctx.animalPutCount = 2;
    }

    @Override
    public void clearContext(PolicyRecord ctx) {
        ctx.animalPutCount = 0;
    }

    @Override
    public List<EffectDecision> resolveDecisions(EffectStateReader state) {
        Card target = cardService.getCard(addAnimals.getFirst());

        boolean alsoHasAnotherTarget = alsoHasAnotherMicrobeAnimalTarget(player, cardService, target, state);

        if (!alsoHasAnotherTarget) {
            return List.of();
        }

        return List.of(new EffectDecision() {
            @Override
            public int getChosenAction() {
                return ActionInputService.getAnimalTargetActionIndex(target);
            }

            @Override
            public void applyRollout(PolicyRecord record) {
                rolloutAnimal(target.getClass(), record, 2);
            }
        });
    }

}
