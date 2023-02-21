package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.ExperimentalTechnology;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.UpgradePhaseHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 19.02.2022
 */
@Component
@RequiredArgsConstructor
public class ExperimentalTechnologyActionProcessor implements BlueActionCardProcessor<ExperimentalTechnology> {
    private final CardService cardService;

    @Override
    public Class<ExperimentalTechnology> getType() {
        return ExperimentalTechnology.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard, List<Integer> inputParameters) {
        player.setTerraformingRating(player.getTerraformingRating() - 1);

        UpgradePhaseHelper.upgradePhase(cardService, game, player, inputParameters.get(0));

        return null;
    }
}
