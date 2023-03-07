package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.CrisisVpToTokenTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CrysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class CrisisVpToTokenTurnProcessor implements TurnProcessor<CrisisVpToTokenTurn> {
    private final CrysisService crysisService;
    private final CardService cardService;

    @Override
    public TurnResponse processTurn(CrisisVpToTokenTurn turn, MarsGame game) {

        crysisService.reduceTokens(game, cardService.getCrysisCard(turn.getCards().get(0)), 1);

        game.getCrysisData().setCrisisVp(game.getCrysisData().getCrisisVp() - 2);

        return null;
    }

    @Override
    public TurnType getType() {
        return TurnType.CRISIS_VP_TO_TOKEN;
    }
}
