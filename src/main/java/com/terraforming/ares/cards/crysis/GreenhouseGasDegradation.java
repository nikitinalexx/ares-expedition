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
public class GreenhouseGasDegradation implements CrysisCard {

    @Getter
    private final int id;

    public GreenhouseGasDegradation(int id) {
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
        return 2;
    }

    @Override
    public String name() {
        return "Greenhouse Gas Degradation";
    }

    @Override
    public CrysisCardAction cardAction() {
        return CrysisCardAction.GREENHOUSE_GAS_DEGRADATION;
    }

    @Override
    public void onPersistentEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        final TerraformingService terraformingService = marsContext.getTerraformingService();

        terraformingService.reduceTemperature(marsContext);
    }

    @Override
    public boolean persistentEffectRequiresChoice() {
        return false;
    }

    @Override
    public String immediateEffect() {
        return "Randomly reveal a phase card from your hand. " +
                "You cannot choose that phase this round. " +
                "Decrease the oxygen 1 step";
    }

    @Override
    public void onImmediateEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        marsContext.getCrysisService().forbidRandomNonPlayedPhase(marsContext.getGame(), marsContext.getPlayer());

        marsContext.getTerraformingService().reduceOxygen(marsContext);
    }

    @Override
    public boolean immediateEffectRequiresChoice() {
        return false;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card card, Map<Integer, List<Integer>> inputParams) {
        if (card.getColor() == CardColor.BLUE) {
            marsContext.getCrysisService().reduceTokens(marsContext.getGame(), this, 1);
        }
    }
}
