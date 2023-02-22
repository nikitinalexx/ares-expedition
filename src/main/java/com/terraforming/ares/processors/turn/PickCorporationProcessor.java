package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.turn.CorporationChoiceTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
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
    private final TerraformingService terraformingService;

    @Override
    public TurnResponse processTurn(CorporationChoiceTurn turn, MarsGame game) {
        Player player = game.getPlayerUuidToPlayer().get(turn.getPlayerUuid());
        player.setSelectedCorporationCard(turn.getCorporationCardId());
        player.setMulligan(false);
        player.getPlayed().addCard(turn.getCorporationCardId());

        Card card = cardService.getCard(turn.getCorporationCardId());
        TurnResponse turnResponse = card.buildProject(
                MarsContext.builder()
                        .game(game)
                        .player(player)
                        .terraformingService(terraformingService)
                        .cardService(cardService)
                        .build()
        );
        if (card.onBuiltEffectApplicableToItself()) {
            final MarsContext marsContext = MarsContext.builder()
                    .game(game)
                    .player(player)
                    .terraformingService(terraformingService)
                    .cardService(cardService)
                    .build();

            card.postProjectBuiltEffect(marsContext, card, turn.getInputParams());
        }
        return turnResponse;
    }

    @Override
    public TurnType getType() {
        return TurnType.PICK_CORPORATION;
    }
}
