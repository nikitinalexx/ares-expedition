package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.corporations.CelestiorCorporation;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickDiscardCardsAction;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
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
public class CelestiorCorporationActionProcessor implements BlueActionCardProcessor<CelestiorCorporation> {
    private final CardService deckService;

    @Override
    public Class<CelestiorCorporation> getType() {
        return CelestiorCorporation.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player) {
        AutoPickDiscardCardsAction.AutoPickDiscardCardsActionBuilder resultBuilder = AutoPickDiscardCardsAction.builder();

        for (Integer card : game.dealCards(3)) {
            Card projectCard = deckService.getCard(card);
            if (projectCard.getTags().contains(Tag.EVENT)) {
                player.getHand().addCard(card);
                resultBuilder.takenCard(CardDto.from(projectCard));
            } else {
                resultBuilder.discardedCard(CardDto.from(projectCard));
            }
        }

        return resultBuilder.build();
    }


}
