package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.green.FloydContinuum;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 19.02.2022
 */
@Component
@RequiredArgsConstructor
public class FloydContinuumActionProcessor implements BlueActionCardProcessor<FloydContinuum> {
    private final TerraformingService terraformingService;

    @Override
    public Class<FloydContinuum> getType() {
        return FloydContinuum.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        int payment = 0;

        if (game.getPlanet().isOxygenMax()) {
            payment += 3;
        }
        if (game.getPlanet().isTemperatureMax()) {
            payment += 3;
        }
        if (game.getPlanet().oceansLeft() == 0) {
            payment += 3;
        }
        if (game.getExpansions().contains(Expansion.INFRASTRUCTURE)) {
            if (game.getPlanet().isInfrastructureMax()) {
                payment += 3;
            }
        }

        player.setMc(player.getMc() + payment);

        return null;
    }
}
