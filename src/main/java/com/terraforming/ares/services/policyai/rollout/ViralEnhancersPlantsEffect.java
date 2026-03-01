package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ViralEnhancersPlantsEffect implements TagEffect {

    private final List<Integer> takePlants;
    private final List<Integer> putResources;
    private final Player player;
    private final CardService cardService;

    public ViralEnhancersPlantsEffect(Map<Integer, List<Integer>> params, Player player, CardService cardService) {
        this.takePlants = params.get(InputFlag.VIRAL_ENHANCERS_TAKE_PLANT.getId());
        this.putResources = params.get(InputFlag.VIRAL_ENHANCERS_PUT_RESOURCE.getId());
        this.player = player;
        this.cardService = cardService;
    }

    @Override
    public boolean isPresent() {
        return !CollectionUtils.isEmpty(takePlants);
    }

    @Override
    public void prepareContext(PolicyRecord record) {
        record.viralEnhancersActivationsLeft = (byte) (takePlantsCount() + putResourcesCount());
    }

    @Override
    public void clearContext(PolicyRecord record) {
        record.viralEnhancersActivationsLeft = 0;
    }

    @Override
    public List<EffectDecision> resolveDecisions(EffectStateReader state) {
        boolean hasAnimalOrMicrobeCard = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .anyMatch(c -> c.getCollectableResource() == CardCollectableResource.ANIMAL
                        || c.getCollectableResource() == CardCollectableResource.MICROBE);

        if (!hasAnimalOrMicrobeCard) {
            // Нет выбора — растения просто берутся
            return List.of();
        }

        int count = takePlantsCount();
        List<EffectDecision> decisions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            decisions.add(new EffectDecision() {
                @Override
                public int getChosenAction() {
                    return ActionInputService.takePlantTargetAction();
                }

                @Override
                public void applyRollout(PolicyRecord record) {
                    record.plants[0]++; // накопленные к этому моменту растения
                    record.viralEnhancersActivationsLeft--;
                }
            });
        }
        return decisions;
    }

    private int takePlantsCount() {
        return CollectionUtils.isEmpty(takePlants) ? 0 : takePlants.getFirst();
    }

    private int putResourcesCount() {
        return CollectionUtils.isEmpty(putResources) ? 0 : putResources.size();
    }

    @Override
    public void applySideEffect(PolicyRecord record) {
        record.plants[0] += (short) takePlantsCount();
    }
}
