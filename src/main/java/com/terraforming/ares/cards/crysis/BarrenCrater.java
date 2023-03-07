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
public class BarrenCrater implements CrysisCard {

    @Getter
    private final int id;

    public BarrenCrater(int id) {
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
        return 2;
    }

    @Override
    public String name() {
        return "Barren Crater";
    }

    @Override
    public CrysisCardAction cardAction() {
        return CrysisCardAction.BARREN_CRATER;
    }

    @Override
    public void onPersistentEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        final TerraformingService terraformingService = marsContext.getTerraformingService();

        final List<Integer> choiceOptions = input.get(InputFlag.CRYSIS_INPUT_FLAG.getId());
        if (choiceOptions.get(0) == InputFlag.CRYSIS_INPUT_OPTION_1.getId()) {
            marsContext.getCrysisService().hideRandomOcean(marsContext);
        } else {
            terraformingService.reduceTemperature(marsContext);
        }
    }

    @Override
    public List<String> persistentOptions() {
        return List.of("Remove an ocean", "Reduce the temperature");
    }

    @Override
    public boolean persistentEffectRequiresChoice() {
        return true;
    }

    @Override
    public String immediateEffect() {
        return "Draw a card. Decrease the oxygen 1 step.";
    }

    @Override
    public void onImmediateEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        final List<Integer> newCards = marsContext.getCardService().dealCards(marsContext.getGame(), 1);
        marsContext.getPlayer().getHand().addCards(newCards);

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
