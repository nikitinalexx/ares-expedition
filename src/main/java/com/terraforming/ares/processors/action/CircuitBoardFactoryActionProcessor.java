package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.CircuitBoardFactory;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@Component
@RequiredArgsConstructor
public class CircuitBoardFactoryActionProcessor implements BlueActionCardProcessor<CircuitBoardFactory> {
    private final CardService deckService;

    @Override
    public Class<CircuitBoardFactory> getType() {
        return CircuitBoardFactory.class;
    }

    @Override
    public TurnResponse process(MarsGame game, PlayerContext player) {
        Deck deck = game.getProjectsDeck().dealCards(1);

        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : deck.getCards()) {
            player.getHand().addCard(card);

            ProjectCard projectCard = deckService.getProjectCard(card);
            resultBuilder.takenCard(CardDto.from(projectCard));
        }

        return resultBuilder.build();
    }
}
