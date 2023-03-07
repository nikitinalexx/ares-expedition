package com.terraforming.ares.cards.crysis;

import com.terraforming.ares.mars.CrysisData;
import com.terraforming.ares.model.*;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 03.03.2023
 */
public class SecondImpact implements CrysisCard {

    @Getter
    private final int id;

    public SecondImpact(int id) {
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
        return 0;
    }

    @Override
    public String name() {
        return "Second Impact";
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
        return "Decrease the oxygen and temperature 2 steps each. Flip 2 oceans facedown. Discard this.";
    }

    @Override
    public void onImmediateEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        for (int i = 0; i < 2; i++) {
            marsContext.getTerraformingService().reduceTemperature(marsContext);
            marsContext.getTerraformingService().reduceOxygen(marsContext);
            marsContext.getCrysisService().hideRandomOcean(marsContext);
        }

        final CrysisData crysisData = marsContext.getGame().getCrysisData();
        crysisData.getOpenedCards().remove((Integer) id);
        crysisData.getCardToTokensCount().remove(id);
    }

    @Override
    public boolean immediateEffectRequiresChoice() {
        return false;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card card, Map<Integer, List<Integer>> inputParams) {
        // no effect
    }
}
