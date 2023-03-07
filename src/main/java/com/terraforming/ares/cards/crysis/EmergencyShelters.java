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
public class EmergencyShelters implements CrysisCard {

    @Getter
    private final int id;

    public EmergencyShelters(int id) {
        this.id = id;
    }

    @Override
    public CardTier tier() {
        return CardTier.T1;
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
        return "Emergency Shelters";
    }

    @Override
    public CrysisCardAction cardAction() {
        return CrysisCardAction.EMERGENCY_SHELTERS;
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
        return "Draw a card. Decrease the temperature 1 step.";
    }

    @Override
    public void onImmediateEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        final List<Integer> newCards = marsContext.getCardService().dealCards(marsContext.getGame(), 1);
        marsContext.getPlayer().getHand().addCards(newCards);

        marsContext.getTerraformingService().reduceTemperature(marsContext);
    }

    @Override
    public boolean immediateEffectRequiresChoice() {
        return false;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card card, Map<Integer, List<Integer>> inputParams) {
        int tagsCount = marsContext.getCardService().countCardTags(card, Set.of(Tag.BUILDING), inputParams);

        if (tagsCount == 0) {
            return;
        }

        marsContext.getCrysisService().reduceTokens(marsContext.getGame(), this, tagsCount);
    }
}
