package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.CorporationChoiceTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.BuildService;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class PickCorporationProcessor implements TurnProcessor<CorporationChoiceTurn> {
    private final CardService cardService;
    private final MarsContextProvider marsContextProvider;

    @Override
    public TurnResponse processTurn(CorporationChoiceTurn turn, MarsGame game) {
        Player player = game.getPlayerUuidToPlayer().get(turn.getPlayerUuid());
        player.setSelectedCorporationCard(turn.getCorporationCardId());
        player.setMulligan(false);
        player.getPlayed().addCard(turn.getCorporationCardId());

        Card card = cardService.getCard(turn.getCorporationCardId());
        final MarsContext marsContext = marsContextProvider.provide(game, player);
        TurnResponse turnResponse = card.buildProject(marsContext);
        if (card.onBuiltEffectApplicableToItself()) {
            card.postProjectBuiltEffect(marsContext, card, turn.getInputParams());
        }
        return turnResponse;
    }

    @Override
    public TurnType getType() {
        return TurnType.PICK_CORPORATION;
    }
}
