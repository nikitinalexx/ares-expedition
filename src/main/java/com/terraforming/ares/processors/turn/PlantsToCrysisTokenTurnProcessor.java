package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.CrysisActiveCardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.PlantsToCrisisTokenTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.MarsContextProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class PlantsToCrysisTokenTurnProcessor implements TurnProcessor<PlantsToCrisisTokenTurn> {
    private final MarsContextProvider marsContextProvider;
    private final CardService cardService;

    @Override
    public TurnType getType() {
        return TurnType.PLANTS_TO_CRISIS_TOKEN;
    }

    @Override
    public TurnResponse processTurn(PlantsToCrisisTokenTurn turn, MarsGame game) {
        Player player = game.getPlayerByUuid(turn.getPlayerUuid());

        player.setPlants(player.getPlants() - Constants.CRISIS_TOKEN_PLANTS_COST);

        new ArrayList<>(game.getCrysisData().getOpenedCards())
                .stream()
                .map(cardService::getCrysisCard)
                .filter(card -> card.getActiveCardAction() == CrysisActiveCardAction.PLANTS_INTO_TOKENS)
                .forEach(
                        card -> marsContextProvider.provide(game, game.getPlayerByUuid(turn.getPlayerUuid()))
                                .getCrysisService()
                                .reduceTokens(game, card, 1)
                );

        return null;
    }
}
