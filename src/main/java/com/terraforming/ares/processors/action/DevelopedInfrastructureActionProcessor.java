package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.DevelopedInfrastructure;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Service
@RequiredArgsConstructor
public class DevelopedInfrastructureActionProcessor implements BlueActionCardProcessor<DevelopedInfrastructure> {
    private final CardService cardService;
    private final TerraformingService terraformingService;

    @Override
    public Class<DevelopedInfrastructure> getType() {
        return DevelopedInfrastructure.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        long minFiveBlueCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .map(Card::getColor)
                .filter(CardColor.BLUE::equals)
                .limit(5)
                .count();

        player.setMc(player.getMc() - (10 - ((minFiveBlueCards == 5) ? 5 : 0)));

        terraformingService.increaseTemperature(game, player);

        return null;
    }


}
