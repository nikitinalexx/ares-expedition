package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.MaglevTrains;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 03.12.2023
 */
@Service
@RequiredArgsConstructor
public class MaglevTrainsActionProcessor implements BlueActionCardProcessor<MaglevTrains> {
    private final CardService deckService;

    @Override
    public Class<MaglevTrains> getType() {
        return MaglevTrains.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        ParameterColor infrastructureColor = game.getPlanetAtTheStartOfThePhase().getInfrastructureColor();

        int cardsToTake = (infrastructureColor == ParameterColor.Y || infrastructureColor == ParameterColor.W) ? 2 : 1;

        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : deckService.dealCards(game, cardsToTake)) {
            player.getHand().addCard(card);

            Card projectCard = deckService.getCard(card);
            resultBuilder.takenCard(CardDto.from(projectCard));
        }

        return resultBuilder.build();
    }

}
