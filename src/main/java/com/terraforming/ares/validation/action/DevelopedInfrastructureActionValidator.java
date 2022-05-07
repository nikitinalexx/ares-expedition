package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.DevelopedInfrastructure;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.services.CardService;
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

    @Override
    public Class<DevelopedInfrastructure> getType() {
        return DevelopedInfrastructure.class;
    }

    @Override
    public String validate(Planet planet, Player player) {
        if (planet.isTemperatureMax()) {
            //TODO not applicable if the temperature maxed in current phase
            return "Temperature is already maximum";
        }

        int temperaturePrice = 10;

        long minFiveBlueCards = player.getPlayed().getCards().stream()
                .map(cardService::getProjectCard)
                .map(ProjectCard::getColor)
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
