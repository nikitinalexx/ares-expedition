package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.RedraftedContracts;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

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
    public TurnResponse process(MarsGame game, Player player, List<Integer> inputParameters) {
        player.getHand().removeCards(inputParameters);

        Deck deck = game.getProjectsDeck().dealCards(inputParameters.size());

        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : deck.getCards()) {
            player.getHand().addCard(card);

            ProjectCard projectCard = cardService.getProjectCard(card);
            resultBuilder.takenCard(CardDto.from(projectCard));
        }

        return resultBuilder.build();
    }
}
