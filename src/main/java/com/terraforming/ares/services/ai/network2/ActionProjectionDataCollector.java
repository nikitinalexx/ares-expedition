package com.terraforming.ares.services.ai.network2;

import com.terraforming.ares.cards.blue.FibrousCompositeMaterial;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.UpgradePhaseHelper;
import com.terraforming.ares.services.ai.advanced.IDataCollect;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActionProjectionDataCollector {
    private final IDataCollect iDataCollect;
    private final CardService cardService;

    public float[] experimentalTechnology(MarsGame game, Player player, int upgrade) {
        List<Integer> originalUpgrades = new ArrayList<>(player.getPhaseCards());
        UpgradePhaseHelper.upgradePhase(player, upgrade);
        player.setTerraformingRating(player.getTerraformingRating() - 1);
        float[] data = iDataCollect.collectData(game, player);
        player.setPhaseCards(originalUpgrades);
        player.setTerraformingRating(player.getTerraformingRating() + 1);
        return data;
    }

    public float[] virtualEmployee(MarsGame game, Player player, int upgrade) {
        List<Integer> originalUpgrades = new ArrayList<>(player.getPhaseCards());
        UpgradePhaseHelper.upgradePhase(player, upgrade);
        float[] data = iDataCollect.collectData(game, player);
        player.setPhaseCards(originalUpgrades);
        return data;
    }

    public float[] fibrousCompositeMaterial(MarsGame game, Player player, int upgrade) {
        List<Integer> originalUpgrades = new ArrayList<>(player.getPhaseCards());
        UpgradePhaseHelper.upgradePhase(player, upgrade);
        player.getCardResourcesCount().put(FibrousCompositeMaterial.class, player.getCardResourcesCount().get(FibrousCompositeMaterial.class) - 3);
        float[] data = iDataCollect.collectData(game, player);
        player.setPhaseCards(originalUpgrades);
        player.getCardResourcesCount().put(FibrousCompositeMaterial.class, player.getCardResourcesCount().get(FibrousCompositeMaterial.class) + 3);
        return data;
    }

}
