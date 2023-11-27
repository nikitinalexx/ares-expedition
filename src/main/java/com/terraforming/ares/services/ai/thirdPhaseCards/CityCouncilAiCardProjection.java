package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.CityCouncil;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

@Component
public class CityCouncilAiCardProjection<T extends Card> implements AiCardProjection<CityCouncil> {
    @Override
    public Class<CityCouncil> getType() {
        return CityCouncil.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        int milestonesAchieved = (int) game.getMilestones().stream().filter(milestone -> milestone.isAchieved(player)).count();

        float totalCardsTaken = 1 + milestonesAchieved;

        return MarsGameRowDifference.builder()
                .cards(totalCardsTaken)
                .build();
    }
}
