package com.terraforming.ares.cards.crysis;

import com.terraforming.ares.model.*;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 03.03.2023
 */
public class BiodiversityLoss implements CrysisCard {

    @Getter
    private final int id;

    public BiodiversityLoss(int id) {
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
        return "Biodiversity Loss";
    }

    @Override
    public CrysisCardAction cardAction() {
        return CrysisCardAction.BIODIVERSITY_LOSS;
    }

    @Override
    public void onPersistentEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        marsContext.getCrysisService().hideRandomOcean(marsContext);
    }

    @Override
    public boolean persistentEffectRequiresChoice() {
        return false;
    }

    @Override
    public String immediateEffect() {
        return "Lose 2 MC. Decrease the temperature 1 step.";
    }

    @Override
    public void onImmediateEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        final Player player = marsContext.getPlayer();
        player.setMc(Math.max(0, player.getMc() - 2));
        marsContext.getTerraformingService().reduceTemperature(marsContext);
    }

    @Override
    public boolean immediateEffectRequiresChoice() {
        return false;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card card, Map<Integer, List<Integer>> inputParams) {
        //no effects
    }

    @Override
    public void onOxygenChangedEffect(MarsContext context) {
        context.getCrysisService().reduceTokens(context.getGame(), this, 1);
    }

    @Override
    public CrysisActiveCardAction getActiveCardAction() {
        return CrysisActiveCardAction.PLANTS_INTO_TOKENS;
    }
}
