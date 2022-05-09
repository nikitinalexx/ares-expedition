package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.AssetLiquidation;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Deck;
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
public class AssetLiquidationActionProcessor implements BlueActionCardProcessor<AssetLiquidation> {
    private final CardService deckService;

    @Override
    public Class<AssetLiquidation> getType() {
        return AssetLiquidation.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player) {
        player.setTerraformingRating(player.getTerraformingRating() - 1);

        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : game.dealCards(3)) {
            player.getHand().addCard(card);
            resultBuilder.takenCard(CardDto.from(deckService.getProjectCard(card)));
        }

        return resultBuilder.build();
    }


}
