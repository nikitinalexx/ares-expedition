package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.MatterManufactoring;
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
public class MatterManufactoringActionProcessor implements BlueActionCardProcessor<MatterManufactoring> {
    private final CardService deckService;

    @Override
    public Class<MatterManufactoring> getType() {
        return MatterManufactoring.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player) {
        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : game.dealCards(1)) {
            player.getHand().addCard(card);

            Card projectCard = deckService.getCard(card);
            resultBuilder.takenCard(CardDto.from(projectCard));
        }

        return resultBuilder.build();
    }


}
