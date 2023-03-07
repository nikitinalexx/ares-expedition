package com.terraforming.ares.cards.crysis;

import com.terraforming.ares.model.*;
import com.terraforming.ares.services.TerraformingService;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 03.03.2023
 */
public class InfrastructureCollapse implements CrysisCard {

    @Getter
    private final int id;

    public InfrastructureCollapse(int id) {
        this.id = id;
    }

    @Override
    public CardTier tier() {
        return CardTier.T3;
    }

    @Override
    public int playerCount() {
        return 1;
    }

    @Override
    public int initialTokens() {
        return 1;
    }

    @Override
    public String name() {
        return "Infrastructure Collapse";
    }

    @Override
    public CrysisCardAction cardAction() {
        return CrysisCardAction.INFRASTRUCTURE_COLLAPSE;
    }

    @Override
    public void onPersistentEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        final TerraformingService terraformingService = marsContext.getTerraformingService();

        final List<Integer> choiceOptions = input.get(InputFlag.CRYSIS_INPUT_FLAG.getId());
        if (choiceOptions.get(0) == InputFlag.CRYSIS_INPUT_OPTION_1.getId()) {
            terraformingService.reduceTemperature(marsContext);
        } else {
            terraformingService.reduceOxygen(marsContext);
        }
    }

    @Override
    public boolean persistentEffectRequiresChoice() {
        return true;
    }

    @Override
    public List<String> persistentOptions() {
        return List.of("Reduce the temperature", "Reduce the oxygen");
    }

    @Override
    public String immediateEffect() {
        return "Lose 5 MC. Flip an ocean facedown. Decrease the oxygen 1 step.";
    }

    @Override
    public void onImmediateEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        final TerraformingService terraformingService = marsContext.getTerraformingService();

        final Player player = marsContext.getPlayer();
        player.setMc(Math.max(0, player.getMc() - 5));

        terraformingService.reduceOxygen(marsContext);

        marsContext.getCrysisService().hideRandomOcean(marsContext);
    }

    @Override
    public boolean immediateEffectRequiresChoice() {
        return false;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card card, Map<Integer, List<Integer>> inputParams) {
        // no effect
    }

    @Override
    public SpecialCrysisGoal getSpecialCrysisGoal() {
        return SpecialCrysisGoal.ACTION_IN_THIRD_PHASE;
    }
}
