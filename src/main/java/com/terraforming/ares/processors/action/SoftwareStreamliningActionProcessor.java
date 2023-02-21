package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.SoftwareStreamlining;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 21.02.2023
 */
@Component
@RequiredArgsConstructor
public class SoftwareStreamliningActionProcessor implements BlueActionCardProcessor<SoftwareStreamlining> {
    private final CardService cardService;

    @Override
    public Class<SoftwareStreamlining> getType() {
        return SoftwareStreamlining.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : cardService.dealCards(game, 2)) {
            player.getHand().addCard(card);

            Card projectCard = cardService.getCard(card);
            resultBuilder.takenCard(CardDto.from(projectCard));
        }

        player.addNextTurn(
                new DiscardCardsTurn(
                        player.getUuid(),
                        List.of(),
                        2,
                        false,
                        true
                )
        );

        return resultBuilder.build();
    }
}
