package com.terraforming.ares.cards.crysis;

import com.terraforming.ares.model.*;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.services.TerraformingService;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 03.03.2023
 */
public class LocalizedTsunami implements CrysisCard {

    @Getter
    private final int id;

    public LocalizedTsunami(int id) {
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
        return "Localized Tsunami";
    }

    @Override
    public CrysisCardAction cardAction() {
        return CrysisCardAction.LOCALIZED_TSUNAMI;
    }

    @Override
    public void onPersistentEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        final TerraformingService terraformingService = marsContext.getTerraformingService();

        final List<Integer> choiceOptions = input.get(InputFlag.CRYSIS_INPUT_FLAG.getId());
        if (choiceOptions.get(0) == InputFlag.CRYSIS_INPUT_OPTION_1.getId()) {
            terraformingService.reduceTemperature(marsContext);
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
        return List.of("Reduce the temperature", "Reduce the oxygen");
    }

    @Override
    public String immediateEffect() {
        return "Draw a card and then discard 2 cards. Flip an ocean facedown";
    }

    @Override
    public void onImmediateEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        marsContext.getCrysisService().hideRandomOcean(marsContext);

        marsContext.getPlayer().getHand().addCards(
                marsContext.getCardService().dealCards(marsContext.getGame(), 1)
        );

        marsContext.getPlayer().addNextTurn(
                new DiscardCardsTurn(
                        marsContext.getPlayer().getUuid(),
                        List.of(),
                        2,
                        false,
                        true
                )
        );
    }

    @Override
    public boolean immediateEffectRequiresChoice() {
        return false;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card card, Map<Integer, List<Integer>> inputParams) {
        int tagsCount = marsContext.getCardService().countCardTags(card, Set.of(Tag.ENERGY), inputParams);

        if (tagsCount == 0) {
            return;
        }

        marsContext.getCrysisService().reduceTokens(marsContext.getGame(), this, tagsCount);
    }
}
