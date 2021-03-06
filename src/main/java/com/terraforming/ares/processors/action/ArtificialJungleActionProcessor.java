package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.ArtificialJungle;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Service
@RequiredArgsConstructor
public class ArtificialJungleActionProcessor implements BlueActionCardProcessor<ArtificialJungle> {
    private final CardService cardService;

    @Override
    public Class<ArtificialJungle> getType() {
        return ArtificialJungle.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        player.setPlants(player.getPlants() - 1);

        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : cardService.dealCards(game, 1)) {
            player.getHand().addCard(card);
            resultBuilder.takenCard(CardDto.from(cardService.getCard(card)));
        }

        return resultBuilder.build();
    }


}
