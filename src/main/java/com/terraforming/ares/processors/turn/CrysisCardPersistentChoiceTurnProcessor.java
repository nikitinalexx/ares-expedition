package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CrysisCard;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.CrysisCardPersistentChoiceTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class CrysisCardPersistentChoiceTurnProcessor implements TurnProcessor<CrysisCardPersistentChoiceTurn> {
    private final CardService cardService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public TurnType getType() {
        return TurnType.RESOLVE_PERSISTENT_WITH_CHOICE;
    }

    @Override
    public TurnResponse processTurn(CrysisCardPersistentChoiceTurn turn, MarsGame game) {
        Player player = game.getPlayerByUuid(turn.getPlayerUuid());

        final CrysisCard card = cardService.getCrysisCard(turn.getCard());

        final MarsContext marsContext = marsContextProvider.provide(game, player);

        card.onPersistentEffect(marsContext, turn.getInput());

        return null;
    }
}
