package com.terraforming.ares.logic;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.turn.CorporationChoiceTurn;
import com.terraforming.ares.model.turn.TurnType;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class PickCorporationProcessor implements TurnProcessor<CorporationChoiceTurn> {

    @Override
    public void processTurn(CorporationChoiceTurn turn, MarsGame game) {
        PlayerContext playerContext = game.getPlayerContexts().get(turn.getPlayerUuid());
        playerContext.setSelectedCorporationCard(turn.getCorporationCardId());
    }

    @Override
    public TurnType getType() {
        return TurnType.PICK_CORPORATION;
    }
}
