package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CrysisCard;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.CrysisImmediateAllTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class CrysisImmediateAllTurnProcessor implements TurnProcessor<CrysisImmediateAllTurn> {
    private final CardService cardService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public TurnType getType() {
        return TurnType.RESOLVE_IMMEDIATE_ALL;
    }

    @Override
    public TurnResponse processTurn(CrysisImmediateAllTurn turn, MarsGame game) {
        final MarsContext marsContext = marsContextProvider.provide(game, game.getPlayerByUuid(turn.getPlayerUuid()));

        final Integer cardId = game.getCrysisData().getOpenedCards().get(game.getCrysisData().getOpenedCards().size() - 1);

        final CrysisCard crysisCard = cardService.getCrysisCard(cardId);

        crysisCard.onImmediateEffect(marsContext, Map.of());

        return null;
    }
}
