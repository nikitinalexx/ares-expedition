package com.terraforming.ares.cards.crysis;

import com.terraforming.ares.model.*;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 03.03.2023
 */
public class DwindlingSupplies implements CrysisCard {

    @Getter
    private final int id;

    public DwindlingSupplies(int id) {
        this.id = id;
    }

    @Override
    public CardTier tier() {
        return CardTier.T4;
    }

    @Override
    public int playerCount() {
        return 1;
    }

    @Override
    public int initialTokens() {
        return 0;
    }

    @Override
    public String name() {
        return "Dwindling Supplies";
    }

    @Override
    public CrysisCardAction cardAction() {
        return CrysisCardAction.PLAY_AND_DISCARD_CRYSIS;
    }

    @Override
    public void onPersistentEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        // no effect
    }

    @Override
    public boolean persistentEffectRequiresChoice() {
        return false;
    }

    @Override
    public String immediateEffect() {
        return "Decrease the oxygen and temperature by 1. Flip an ocean facedown. " +
                "You may spend VP to gain 5 MC any time. Fully terraform Mars before the " +
                "deck runs out and win the game.";
    }

    @Override
    public void onImmediateEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        marsContext.getTerraformingService().reduceTemperature(marsContext);
        marsContext.getTerraformingService().reduceOxygen(marsContext);
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
    public boolean endGameCard() {
        return true;
    }
}
