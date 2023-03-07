package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CrysisCard;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.CrysisCardImmediateChoiceTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.MarsContextProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class CrysisCardImmediateChoiceTurnProcessor implements TurnProcessor<CrysisCardImmediateChoiceTurn> {
    private final CardService cardService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public TurnType getType() {
        return TurnType.RESOLVE_IMMEDIATE_WITH_CHOICE;
    }

    @Override
    public TurnResponse processTurn(CrysisCardImmediateChoiceTurn turn, MarsGame game) {
        Player player = game.getPlayerByUuid(turn.getPlayerUuid());

        final CrysisCard card = cardService.getCrysisCard(turn.getCard());

        final MarsContext marsContext = marsContextProvider.provide(game, player);

        card.onImmediateEffect(marsContext, turn.getInput());

        return null;
    }
}
