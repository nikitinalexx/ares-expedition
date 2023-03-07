package com.terraforming.ares.cards.crysis;

import com.terraforming.ares.model.*;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 03.03.2023
 */
public class DustClouds implements CrysisCard {

    @Getter
    private final int id;

    public DustClouds(int id) {
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
        return "Dust Clouds";
    }

    @Override
    public CrysisCardAction cardAction() {
        return CrysisCardAction.DUST_CLOUDS;
    }

    @Override
    public void onPersistentEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        marsContext.getTerraformingService().reduceOxygen(marsContext);
    }

    @Override
    public boolean persistentEffectRequiresChoice() {
        return false;
    }

    @Override
    public String immediateEffect() {
        return "Discard a card in hand. Flip an ocean facedown";
    }

    @Override
    public void onImmediateEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        marsContext.getCrysisService().hideRandomOcean(marsContext);

        final List<Integer> crysisInput = input.get(InputFlag.CRYSIS_INPUT_FLAG.getId());
        final Integer cardId = crysisInput.get(0);

        marsContext.getPlayer().getHand().removeCard(cardId);
    }

    @Override
    public boolean immediateEffectRequiresChoice() {
        return true;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card card, Map<Integer, List<Integer>> inputParams) {
        //no effects
    }

    @Override
    public void onTemperatureChangedEffect(MarsContext context) {
        context.getCrysisService().reduceTokens(context.getGame(), this, 1);
    }

    @Override
    public CrysisActiveCardAction getActiveCardAction() {
        return CrysisActiveCardAction.HEAT_INTO_TOKENS;
    }
}
