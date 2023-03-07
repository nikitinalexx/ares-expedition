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
public class CollapsingCities implements CrysisCard {

    @Getter
    private final int id;

    public CollapsingCities(int id) {
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
        return "Collapsing Cities";
    }

    @Override
    public CrysisCardAction cardAction() {
        return CrysisCardAction.COLLAPSING_CITIES;
    }

    @Override
    public void onPersistentEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        final TerraformingService terraformingService = marsContext.getTerraformingService();

        terraformingService.reduceOxygen(marsContext);
    }

    @Override
    public boolean persistentEffectRequiresChoice() {
        return false;
    }

    @Override
    public String immediateEffect() {
        return "Randomly reveal a phase card from your hand. " +
                "You cannot choose that phase this round. " +
                "Decrease the temperature 2 steps";
    }

    @Override
    public void onImmediateEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        marsContext.getCrysisService().forbidRandomNonPlayedPhase(marsContext.getGame(), marsContext.getPlayer());

        marsContext.getTerraformingService().reduceTemperature(marsContext);
        marsContext.getTerraformingService().reduceTemperature(marsContext);
    }

    @Override
    public boolean immediateEffectRequiresChoice() {
        return false;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card card, Map<Integer, List<Integer>> inputParams) {
        if (card.getColor() == CardColor.GREEN) {
            marsContext.getCrysisService().reduceTokens(marsContext.getGame(), this, 1);
        }
    }
}
