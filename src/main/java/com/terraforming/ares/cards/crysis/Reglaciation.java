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
public class Reglaciation implements CrysisCard {

    @Getter
    private final int id;

    public Reglaciation(int id) {
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
        return 2;
    }

    @Override
    public String name() {
        return "Reglaciation";
    }

    @Override
    public CrysisCardAction cardAction() {
        return CrysisCardAction.REGLACIATION;
    }

    @Override
    public void onPersistentEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        marsContext.getCrysisService().hideRandomOcean(marsContext);
        marsContext.getCrysisService().hideRandomOcean(marsContext);
    }

    @Override
    public boolean persistentEffectRequiresChoice() {
        return false;
    }

    @Override
    public String immediateEffect() {
        return "Decrease the temperature and oxygen 1 step each. Flip and ocean facedown";
    }

    @Override
    public void onImmediateEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        final TerraformingService terraformingService = marsContext.getTerraformingService();
        terraformingService.reduceTemperature(marsContext);
        terraformingService.reduceOxygen(marsContext);
        marsContext.getCrysisService().hideRandomOcean(marsContext);
    }

    @Override
    public boolean immediateEffectRequiresChoice() {
        return false;
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
