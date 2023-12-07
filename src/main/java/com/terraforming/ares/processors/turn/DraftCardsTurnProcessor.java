package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.model.turn.DraftCardsTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.SpecialEffectsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class DraftCardsTurnProcessor implements TurnProcessor<DraftCardsTurn> {
    private final SpecialEffectsService specialEffectsService;
    private final CardService cardService;
    private final DraftCardsService draftCardsService;

    @Override
    public TurnType getType() {
        return TurnType.DRAFT_CARDS;
    }

    @Override
    public TurnResponse processTurn(DraftCardsTurn turn, MarsGame game) {
        Player player = game.getPlayerByUuid(turn.getPlayerUuid());

        int initialCardsToDraft = (player.getChosenPhase() == 5 ? 5 : 2);
        int initialCardsToTake = (player.getChosenPhase() == 5 ? 2 : 1);

        if (player.getChosenPhase() == 5 && player.hasPhaseUpgrade(Constants.PHASE_5_UPGRADE_KEEP_EXTRA)) {
            initialCardsToDraft = 4;
            initialCardsToTake = 3;
        }

        if (player.getChosenPhase() == 5 && player.hasPhaseUpgrade(Constants.PHASE_5_UPGRADE_SEE_EXTRA)) {
            initialCardsToDraft = 8;
        }

        int cardsToDraft = initialCardsToDraft + draftCardsService.countExtraCardsToDraft(player);
        int cardsToTake = initialCardsToTake + draftCardsService.countExtraCardsToTake(player);

        cardsToTake = Math.min(cardsToTake, cardsToDraft);

        List<Integer> draftedCards = cardService.dealCards(game, cardsToDraft);
        player.getHand().addCards(draftedCards);

        player.addNextTurn(
                new DiscardCardsTurn(
                        player.getUuid(),
                        draftedCards,
                        cardsToDraft - cardsToTake,
                        true,
                        true,
                        List.of()
                )
        );

        return null;
    }
}
