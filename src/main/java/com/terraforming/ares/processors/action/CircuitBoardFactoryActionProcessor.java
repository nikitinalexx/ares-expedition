package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.CircuitBoardFactory;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
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
    public TurnResponse process(MarsGame game, Player player) {
        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : game.dealCards(1)) {
            player.getHand().addCard(card);

            ProjectCard projectCard = deckService.getProjectCard(card);
            resultBuilder.takenCard(CardDto.from(projectCard));
        }

        return resultBuilder.build();
    }
}
