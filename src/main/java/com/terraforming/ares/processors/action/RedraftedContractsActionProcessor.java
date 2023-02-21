package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.RedraftedContracts;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class RedraftedContractsActionProcessor implements BlueActionCardProcessor<RedraftedContracts> {
    private final CardService cardService;

    @Override
    public Class<RedraftedContracts> getType() {
        return RedraftedContracts.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard, Map<Integer, List<Integer>> inputParameters) {
        final List<Integer> cardsInput = inputParameters.get(InputFlag.CARD_CHOICE.getId());

        player.getHand().removeCards(cardsInput);

        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer cardId : cardService.dealCards(game, cardsInput.size())) {
            player.getHand().addCard(cardId);

            Card projectCard = cardService.getCard(cardId);
            resultBuilder.takenCard(CardDto.from(projectCard));
        }

        return resultBuilder.build();
    }
}
