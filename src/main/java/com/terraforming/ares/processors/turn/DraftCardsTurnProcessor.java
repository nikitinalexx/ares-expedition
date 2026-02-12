package com.terraforming.ares.processors.turn;

import com.terraforming.ares.dto.DraftCardsDto;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.model.turn.DraftCardsTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DraftCardsService;
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
    private final CardService cardService;
    private final DraftCardsService draftCardsService;

    @Override
    public TurnType getType() {
        return TurnType.DRAFT_CARDS;
    }

    @Override
    public TurnResponse processTurn(DraftCardsTurn turn, MarsGame game) {
        Player player = game.getPlayerByUuid(turn.getPlayerUuid());

        DraftCardsDto draftCardsDto = draftCardsService.countCardsToTakeAndDraftByChosenPhase(player);

        List<Integer> draftedCards = cardService.dealCards(game, draftCardsDto.getCardsToSee());
        player.getHand().addCards(draftedCards);

        player.addNextTurn(
                new DiscardCardsTurn(
                        player.getUuid(),
                        draftedCards,
                        draftCardsDto.getCardsToSee() - draftCardsDto.getCardsToTake(),
                        true,
                        true,
                        List.of()
                )
        );

        return null;
    }
}
