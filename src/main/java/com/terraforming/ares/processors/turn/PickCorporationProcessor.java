package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CorporationCard;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.CorporationChoiceTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class PickCorporationProcessor implements TurnProcessor<CorporationChoiceTurn> {
    private final CardService marsDeckService;

    @Override
    public TurnResponse processTurn(CorporationChoiceTurn turn, MarsGame game) {
        PlayerContext playerContext = game.getPlayerContexts().get(turn.getPlayerUuid());
        playerContext.setSelectedCorporationCard(turn.getCorporationCardId());

        CorporationCard card = marsDeckService.getCorporationCard(turn.getCorporationCardId());
        card.buildProject(playerContext);

        return null;
    }

    @Override
    public TurnType getType() {
        return TurnType.PICK_CORPORATION;
    }
}
