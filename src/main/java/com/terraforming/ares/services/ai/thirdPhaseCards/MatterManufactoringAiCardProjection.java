package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.MatterManufactoring;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

@Component
public class MatterManufactoringAiCardProjection<T extends Card> implements AiCardProjection<MatterManufactoring> {
    @Override
    public Class<MatterManufactoring> getType() {
        return MatterManufactoring.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        if (player.getMc() < 1) {
            return new MarsGameRowDifference();
        }

        player.setMc(player.getMc() - 1);

        return MarsGameRowDifference.builder()
                .cards(1)
                .build();
    }
}
