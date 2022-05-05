package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.AiCentral;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.DeckService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Service
@RequiredArgsConstructor
public class AiCentralBlueActionProcessor implements BlueActionCardProcessor<AiCentral> {
    private final DeckService deckService;

    @Override
    public Class<AiCentral> getType() {
        return AiCentral.class;
    }

    @Override
    public TurnResponse process(MarsGame game, PlayerContext player) {
        Deck deck = game.getProjectsDeck().dealCards(2);

        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : deck.getCards()) {
            player.getHand().addCard(card);

            ProjectCard projectCard = deckService.getProjectCard(card);
            resultBuilder.takenCard(CardDto.from(projectCard));
        }

        return resultBuilder.build();
    }


}