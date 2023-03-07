package com.terraforming.ares.cards.crysis;

import com.terraforming.ares.model.*;
import com.terraforming.ares.services.TerraformingService;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 03.03.2023
 */
public class AtmosphericEscape implements CrysisCard {

    @Getter
    private final int id;

    public AtmosphericEscape(int id) {
        this.id = id;
    }

    @Override
    public CardTier tier() {
        return CardTier.T2;
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
        return "Atmospheric Escape";
    }

    @Override
    public CrysisCardAction cardAction() {
        return CrysisCardAction.ATMOSPHERIC_ESCAPE;
    }

    @Override
    public void onPersistentEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        final TerraformingService terraformingService = marsContext.getTerraformingService();

        final List<Integer> choiceOptions = input.get(InputFlag.CRYSIS_INPUT_FLAG.getId());
        if (choiceOptions.get(0) == InputFlag.CRYSIS_INPUT_OPTION_1.getId()) {
            marsContext.getCrysisService().hideRandomOcean(marsContext);
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
        return List.of("Remove an ocean", "Reduce the oxygen");
    }

    @Override
    public String immediateEffect() {
        return "Gain 2 heat or 2 plants. Decrease the temperature";
    }

    @Override
    public void onImmediateEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        final Player player = marsContext.getPlayer();

        final Integer choice = input.get(InputFlag.CRYSIS_INPUT_FLAG.getId()).get(0);
        if (choice == InputFlag.CRYSIS_INPUT_OPTION_1.getId()) {
            player.setHeat(player.getHeat() + 2);
        } else {
            player.setPlants(player.getPlants() + 2);
        }

        marsContext.getTerraformingService().reduceTemperature(marsContext);
    }

    @Override
    public boolean immediateEffectRequiresChoice() {
        return true;
    }

    @Override
    public List<String> immediateOptions() {
        return List.of("Gain 2 heat", "Gain 2 plants");
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card card, Map<Integer, List<Integer>> inputParams) {
        int tagsCount = marsContext.getCardService().countCardTags(card, Set.of(Tag.EVENT), inputParams);

        if (tagsCount == 0) {
            return;
        }

        marsContext.getCrysisService().reduceTokens(marsContext.getGame(), this, tagsCount);
    }
}
