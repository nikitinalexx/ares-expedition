package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.ThinkTank;
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

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class ThinkTankActionProcessor implements BlueActionCardProcessor<ThinkTank> {
    private final CardService cardService;

    @Override
    public Class<ThinkTank> getType() {
        return ThinkTank.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player) {
        player.setMc(player.getMc() - 2);

        Deck deck = game.getProjectsDeck().dealCards(1);

        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : deck.getCards()) {
            player.getHand().addCard(card);

            ProjectCard projectCard = cardService.getProjectCard(card);
            resultBuilder.takenCard(CardDto.from(projectCard));
        }

        return resultBuilder.build();
    }
}
