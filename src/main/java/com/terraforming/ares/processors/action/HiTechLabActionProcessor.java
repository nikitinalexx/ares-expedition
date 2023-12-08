package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.green.HiTechLab;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class HiTechLabActionProcessor implements BlueActionCardProcessor<HiTechLab> {
    private final CardService cardService;

    @Override
    public Class<HiTechLab> getType() {
        return HiTechLab.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard, Map<Integer, List<Integer>> inputParameters) {
        Integer input = inputParameters.get(InputFlag.DISCARD_HEAT.getId()).get(0);

        player.setHeat(player.getHeat() - input);

        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        List<Integer> cards = new ArrayList<>();

        for (Integer card : cardService.dealCards(game, input)) {
            player.getHand().addCard(card);

            Card projectCard = cardService.getCard(card);
            resultBuilder.takenCard(CardDto.from(projectCard));
            cards.add(card);
        }

        if (cards.size() > 1) {
            player.addNextTurn(
                    new DiscardCardsTurn(
                            player.getUuid(),
                            cards,
                            cards.size() - 1,
                            true,
                            true,
                            List.of()
                    )
            );
        }

        return resultBuilder.build();
    }

}
