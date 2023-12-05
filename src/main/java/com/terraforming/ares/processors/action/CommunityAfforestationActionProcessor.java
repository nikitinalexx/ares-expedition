package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.CommunityAfforestation;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Service
@RequiredArgsConstructor
public class CommunityAfforestationActionProcessor implements BlueActionCardProcessor<CommunityAfforestation> {
    private final TerraformingService terraformingService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public Class<CommunityAfforestation> getType() {
        return CommunityAfforestation.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        int milestonesAchieved = (int) game.getMilestones().stream().filter(milestone -> milestone.isAchieved(player)).count();

        int forestPrice = Math.max(0, 14 - 4 * milestonesAchieved);

        player.setMc(player.getMc() - forestPrice);

        terraformingService.buildForest(marsContextProvider.provide(game, player, Map.of()));

        return null;
    }

}
