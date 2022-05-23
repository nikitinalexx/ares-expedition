package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.model.turn.DraftCardsTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
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

    @Override
    public TurnType getType() {
        return TurnType.DRAFT_CARDS;
    }

    @Override
    public TurnResponse processTurn(DraftCardsTurn turn, MarsGame game) {
        Player player = game.getPlayerByUuid(turn.getPlayerUuid());

        int cardsToDraft = player.getChosenPhase() == 5 ? 5 : 2;
        int cardsToTake = player.getChosenPhase() == 5 ? 2 : 1;

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.EXTENDED_RESOURCES)) {
            cardsToTake++;
        }

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.INTERPLANETARY_RELATIONS)) {
            cardsToDraft++;
            cardsToTake++;
        }

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.INTERNS)) {
            cardsToDraft += 2;
        }

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.UNITED_PLANETARY_ALLIANCE)) {
            cardsToDraft++;
            cardsToTake++;
        }

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.THARSIS_REPUBLIC)) {
            cardsToDraft++;
            cardsToTake++;
        }

        cardsToTake = Math.min(cardsToTake, cardsToDraft);

        List<Integer> draftedCards = cardService.dealCards(game, cardsToDraft);
        player.getHand().addCards(draftedCards);

        player.setNextTurn(
                new DiscardCardsTurn(
                        player.getUuid(),
                        draftedCards,
                        cardsToDraft - cardsToTake,
                        true
                )
        );

        return null;
    }
}
