package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.DevelopedInfrastructure;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Component
@RequiredArgsConstructor
public class DevelopedInfrastructureActionValidator implements ActionValidator<DevelopedInfrastructure> {
    private final CardService cardService;
    private final TerraformingService terraformingService;

    @Override
    public Class<DevelopedInfrastructure> getType() {
        return DevelopedInfrastructure.class;
    }

    @Override
    public String validate(MarsGame game, Player player) {
        if (!terraformingService.canIncreaseTemperature(game)) {
            return "Can not increase temperature anymore";
        }

        int temperaturePrice = 10;

        long minFiveBlueCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .map(Card::getColor)
                .filter(CardColor.BLUE::equals)
                .limit(5)
                .count();

        if (minFiveBlueCards == 5) {
            temperaturePrice -= 5;
        }

        if (player.getMc() < temperaturePrice) {
            return "Not enough MC to increase the temperature";
        }

        return null;
    }
}
