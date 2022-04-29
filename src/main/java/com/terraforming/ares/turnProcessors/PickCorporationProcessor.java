package com.terraforming.ares.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CorporationCard;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.turn.CorporationChoiceTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.DeckService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class PickCorporationProcessor implements TurnProcessor<CorporationChoiceTurn> {
    private final DeckService marsDeckService;

    @Override
    public void processTurn(CorporationChoiceTurn turn, MarsGame game) {
        PlayerContext playerContext = game.getPlayerContexts().get(turn.getPlayerUuid());
        playerContext.setSelectedCorporationCard(turn.getCorporationCardId());

        CorporationCard card = marsDeckService.getCorporationCard(turn.getCorporationCardId());
        card.buildProject(playerContext);
    }

    @Override
    public TurnType getType() {
        return TurnType.PICK_CORPORATION;
    }
}
