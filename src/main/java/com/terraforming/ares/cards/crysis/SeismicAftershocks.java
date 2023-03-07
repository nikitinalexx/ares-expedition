package com.terraforming.ares.cards.crysis;

import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CrysisService;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 03.03.2023
 */
public class SeismicAftershocks implements CrysisCard {

    @Getter
    private final int id;

    public SeismicAftershocks(int id) {
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
        return "Seismic Aftershocks";
    }

    @Override
    public CrysisCardAction cardAction() {
        return CrysisCardAction.SEISMIC_AFTERSHOCKS;
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
        return "Remove one of your played green cards, or flip an ocean facedown";
    }

    @Override
    public void onImmediateEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        final CrysisService crysisService = marsContext.getCrysisService();

        final List<Integer> crysisInput = input.get(InputFlag.CRYSIS_INPUT_FLAG.getId());

        final Integer option = crysisInput.get(0);
        if (option == InputFlag.CRYSIS_INPUT_OPTION_1.getId()) {
            int cardId = crysisInput.get(1);
            final Player player = marsContext.getPlayer();
            player.getPlayed().removeCard(cardId);
            final CardService cardService = marsContext.getCardService();
            player.getPlayed().getCards().stream().map(cardService::getCard).forEach(
                    card -> card.revertPlayedTags(cardService, cardService.getCard(cardId), player)
            );
            cardService.getCard(cardId).revertCardIncome(marsContext);
        } else if (option == InputFlag.CRYSIS_INPUT_OPTION_2.getId()) {
            crysisService.hideRandomOcean(marsContext);
        }

    }

    @Override
    public boolean immediateEffectRequiresChoice() {
        return true;
    }

    @Override
    public List<String> immediateOptions() {
        return List.of("Remove played Green card", "Remove an ocean");
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card card, Map<Integer, List<Integer>> inputParams) {
        //no effects
    }

    @Override
    public void onOceanFlippedEffect(MarsContext context) {
        context.getCrysisService().reduceTokens(context.getGame(), this, 1);
    }

    @Override
    public CrysisActiveCardAction getActiveCardAction() {
        return CrysisActiveCardAction.CARDS_INTO_TOKENS;
    }
}
