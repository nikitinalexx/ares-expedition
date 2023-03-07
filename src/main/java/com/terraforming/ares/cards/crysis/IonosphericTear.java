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
public class IonosphericTear implements CrysisCard {

    @Getter
    private final int id;

    public IonosphericTear(int id) {
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
        return "Ionospheric Tear";
    }

    @Override
    public CrysisCardAction cardAction() {
        return CrysisCardAction.IONOSPHERIC_TEAR;
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
        return "Decrease the temperature 2 steps.";
    }

    @Override
    public void onImmediateEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        marsContext.getTerraformingService().reduceTemperature(marsContext);
        marsContext.getTerraformingService().reduceTemperature(marsContext);
    }

    @Override
    public boolean immediateEffectRequiresChoice() {
        return false;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card card, Map<Integer, List<Integer>> inputParams) {
        int tagsCount = marsContext.getCardService().countCardTags(card, Set.of(Tag.EARTH, Tag.JUPITER), inputParams);

        if (tagsCount == 0) {
            return;
        }

        marsContext.getCrysisService().reduceTokens(marsContext.getGame(), this, tagsCount);
    }
}
