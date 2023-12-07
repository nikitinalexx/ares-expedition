package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.green.BusinessNetwork;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class BusinessNetworkActionProcessor implements BlueActionCardProcessor<BusinessNetwork> {
    private final CardService cardService;

    @Override
    public Class<BusinessNetwork> getType() {
        return BusinessNetwork.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        player.setMc(player.getMc() - 3);

        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : cardService.dealCards(game, 1)) {
            player.getHand().addCard(card);

            Card projectCard = cardService.getCard(card);
            resultBuilder.takenCard(CardDto.from(projectCard));
        }

        return resultBuilder.build();
    }
}
