package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.AquiferPumping;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.TurnResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Service
@RequiredArgsConstructor
public class AquiferPumpingBlueActionProcessor implements BlueActionCardProcessor<AquiferPumping> {

    @Override
    public Class<AquiferPumping> getType() {
        return AquiferPumping.class;
    }

    @Override
    public TurnResponse process(MarsGame game, PlayerContext player) {
        player.setMc(player.getMc() - (10 - player.getSteelIncome() * 2));
        //TODO flip ocean
        //flip ocean
        //get bonus for ocean
        return null;
    }


}
