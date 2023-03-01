package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildType;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.SkipTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class SkipTurnProcessor implements TurnProcessor<SkipTurn> {
    private final CardService cardService;

    @Override
    public TurnType getType() {
        return TurnType.SKIP_TURN;
    }

    @Override
    public TurnResponse processTurn(SkipTurn turn, MarsGame game) {
        Player player = game.getPlayerByUuid(turn.getPlayerUuid());

        if (player.canBuildAny(List.of(BuildType.BLUE_RED_OR_MC))) {
            player.setMc(player.getMc() + 6);
        }

        if (player.canBuildAny(List.of(BuildType.BLUE_RED_OR_CARD))) {
            List<Integer> cards = cardService.dealCards(game, 1);

            for (Integer card : cards) {
                player.getHand().addCard(card);
            }
        }

        player.clearPhaseResults();

        return null;
    }
}
