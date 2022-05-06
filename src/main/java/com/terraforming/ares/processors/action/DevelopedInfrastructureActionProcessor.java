package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.DevelopedInfrastructure;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.ProjectCard;
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
public class DevelopedInfrastructureActionProcessor implements BlueActionCardProcessor<DevelopedInfrastructure> {
    private final CardService cardService;

    @Override
    public Class<DevelopedInfrastructure> getType() {
        return DevelopedInfrastructure.class;
    }

    @Override
    public TurnResponse process(MarsGame game, PlayerContext player) {
        long minFiveBlueCards = player.getPlayed().getCards().stream()
                .map(cardService::getProjectCard)
                .map(ProjectCard::getColor)
                .filter(CardColor.BLUE::equals)
                .limit(5)
                .count();

        player.setMc(player.getMc() - (10 - ((minFiveBlueCards == 5) ? 5 : 0)));

        player.setTerraformingRating(player.getTerraformingRating() + 1);

        game.getPlanet().increaseTemperature();

        return null;
    }


}
