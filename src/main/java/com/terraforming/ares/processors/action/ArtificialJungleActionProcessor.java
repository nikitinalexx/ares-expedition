package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.ArtificialJungle;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.PlayerContext;
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
public class ArtificialJungleActionProcessor implements BlueActionCardProcessor<ArtificialJungle> {
    private final DeckService deckService;

    @Override
    public Class<ArtificialJungle> getType() {
        return ArtificialJungle.class;
    }

    @Override
    public TurnResponse process(MarsGame game, PlayerContext player) {
        player.setPlants(player.getPlants() - 1);

        Deck deck = game.getProjectsDeck().dealCards(1);

        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : deck.getCards()) {
            player.getHand().addCard(card);
            resultBuilder.takenCard(CardDto.from(deckService.getProjectCard(card)));
        }

        return resultBuilder.build();
    }


}
