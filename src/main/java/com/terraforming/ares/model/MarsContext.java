package com.terraforming.ares.model;

import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.services.*;
import lombok.Builder;
import lombok.Value;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@Value
@Builder
public class MarsContext {
    MarsGame game;
    Player player;
    TerraformingService terraformingService;
    CardService cardService;
    BuildService buildService;
    CrysisService crysisService;
    CardResourceService cardResourceService;

    public AutoPickCardsAction dealCards(int count) {
        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : cardService.dealCards(game, count)) {
            player.getHand().addCard(card);
            resultBuilder.takenCard(CardDto.from(cardService.getCard(card)));
        }

        return resultBuilder.build();
    }

}
