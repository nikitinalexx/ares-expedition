package com.terraforming.ares.cards.crysis;

import com.terraforming.ares.mars.MarsGame;
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
public class AtmosphereRupture implements CrysisCard {
    @Getter
    private final int id;

    public AtmosphereRupture(int id) {
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
        return "Atmosphere Rupture";
    }

    @Override
    public CrysisCardAction cardAction() {
        return CrysisCardAction.ATMOSPHERE_RUPTURE;
    }

    @Override
    public void onPersistentEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        final TerraformingService terraformingService = marsContext.getTerraformingService();

        terraformingService.reduceOxygen(marsContext);
    }

    @Override
    public boolean persistentEffectRequiresChoice() {
        return false;
    }

    @Override
    public String immediateEffect() {
        return "Lose 5 MC or 1 TR";
    }

    @Override
    public List<String> immediateOptions() {
        return List.of("Lose 5 MC", "Lose 1 TR");
    }

    @Override
    public void onImmediateEffect(MarsContext marsContext, Map<Integer, List<Integer>> input) {
        final Player player = marsContext.getPlayer();

        final Integer choice = input.get(InputFlag.CRYSIS_INPUT_FLAG.getId()).get(0);
        if (choice == InputFlag.CRYSIS_INPUT_OPTION_1.getId()) {
            player.setMc(player.getMc() - 5);
        } else {
            player.setTerraformingRating(player.getTerraformingRating() - 1);
        }
    }

    @Override
    public boolean immediateEffectRequiresChoice() {
        return true;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card card, Map<Integer, List<Integer>> inputParams) {
        int tagsCount = marsContext.getCardService().countCardTags(card, Set.of(Tag.SPACE), inputParams);

        if (tagsCount == 0) {
            return;
        }

        marsContext.getCrysisService().reduceTokens(marsContext.getGame(), this, tagsCount);
    }
}
