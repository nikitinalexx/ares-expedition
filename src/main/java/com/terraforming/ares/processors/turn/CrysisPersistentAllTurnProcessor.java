package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CrysisCard;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.CrysisPersistentAllTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.MarsContextProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class CrysisPersistentAllTurnProcessor implements TurnProcessor<CrysisPersistentAllTurn> {
    private final CardService cardService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public TurnType getType() {
        return TurnType.RESOLVE_PERSISTENT_ALL;
    }

    @Override
    public TurnResponse processTurn(CrysisPersistentAllTurn turn, MarsGame game) {
        final Player player = game.getPlayerUuidToPlayer().values().iterator().next();

        if (turn.getPlayerUuid().equals(player.getUuid())) {
            final MarsContext marsContext = marsContextProvider.provide(game, player);

            final List<CrysisCard> crysisCards = game.getCrysisData()
                    .getOpenedCards()
                    .stream()
                    .map(cardService::getCrysisCard)
                    .filter(card -> !card.persistentEffectRequiresChoice())
                    .collect(Collectors.toList());

            crysisCards.forEach(card -> card.onPersistentEffect(marsContext, Map.of()));
        }

        return null;
    }
}
