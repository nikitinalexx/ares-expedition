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

public class LargeConvoyEffect extends AbstractTagEffect {

    private final List<Integer> pickPlant;
    private final List<Integer> addAnimal;
    private final CardService cardService;
    private final Player player;

    public LargeConvoyEffect(Map<Integer, List<Integer>> params, CardService cardService, Player player) {
        this.pickPlant = params.get(InputFlag.LARGE_CONVOY_PICK_PLANT.getId());
        this.addAnimal = params.get(InputFlag.LARGE_CONVOY_ADD_ANIMAL.getId());
        this.cardService = cardService;
        this.player = player;
    }

    @Override
    public boolean isPresent() {
        return !CollectionUtils.isEmpty(pickPlant) || !CollectionUtils.isEmpty(addAnimal);
    }

    @Override
    public void prepareContext(PolicyRecord ctx) {
        ctx.largeConvoyEffect = true;
    }

    @Override
    public void clearContext(PolicyRecord ctx) {
        ctx.largeConvoyEffect = false;
    }

    public List<EffectDecision> resolveDecisions(EffectStateReader state) {
        boolean hasAnyAnimalCard = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .anyMatch(c -> c.getCollectableResource() == CardCollectableResource.ANIMAL);

        if (!hasAnyAnimalCard) {
            return List.of(); // applySideEffect добавит 5 растений
        }

        // Игрок выбрал растения несмотря на наличие карт
        if (!CollectionUtils.isEmpty(pickPlant)) {
            return List.of(pickPlantsDecision());
        }

        Card target = cardService.getCard(addAnimal.getFirst());
        return List.of(new EffectDecision() {
            @Override
            public HeadAction getChosenAction() {
                return ActionInputService.getAnimalTargetAction(target);
            }

            @Override
            public void applyRollout(PolicyRecord record) {
                rolloutAnimal(target.getClass(), record, 3);
            }
        });
    }

    private EffectDecision pickPlantsDecision() {
        return new EffectDecision() {
            @Override
            public HeadAction getChosenAction() {
                return ActionInputService.takePlantTargetAction();
            }

            @Override
            public void applyRollout(PolicyRecord record) {
                record.plants[0] += 5;
            }
        };
    }

    @Override
    public void applySideEffect(PolicyRecord record) {
        record.plants[0] += 5;
    }
}
