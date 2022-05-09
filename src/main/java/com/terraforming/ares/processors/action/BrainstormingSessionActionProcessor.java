package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.BrainstormingSession;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickDiscardCardsAction;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@Component
@RequiredArgsConstructor
public class BrainstormingSessionActionProcessor implements BlueActionCardProcessor<BrainstormingSession> {
    private final CardService deckService;

    @Override
    public Class<BrainstormingSession> getType() {
        return BrainstormingSession.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player) {
        AutoPickDiscardCardsAction.AutoPickDiscardCardsActionBuilder resultBuilder = AutoPickDiscardCardsAction.builder();

        for (Integer card : game.dealCards(1)) {
            ProjectCard projectCard = deckService.getProjectCard(card);
            if (projectCard.getColor() == CardColor.GREEN) {
                player.setMc(player.getMc() + 1);
                resultBuilder.discardedCard(CardDto.from(projectCard));
            } else {
                player.getHand().addCard(card);
                resultBuilder.takenCard(CardDto.from(projectCard));
            }
        }

        return resultBuilder.build();
    }
}
