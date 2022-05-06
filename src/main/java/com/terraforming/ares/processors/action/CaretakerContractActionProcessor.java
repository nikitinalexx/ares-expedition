package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.CaretakerContract;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.TurnResponse;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@Component
public class CaretakerContractActionProcessor implements BlueActionCardProcessor<CaretakerContract> {
    @Override
    public Class<CaretakerContract> getType() {
        return CaretakerContract.class;
    }

    @Override
    public TurnResponse process(MarsGame game, PlayerContext player) {
        player.setHeat(player.getHeat() - 8);
        player.setTerraformingRating(player.getTerraformingRating() + 1);

        return null;
    }
}
