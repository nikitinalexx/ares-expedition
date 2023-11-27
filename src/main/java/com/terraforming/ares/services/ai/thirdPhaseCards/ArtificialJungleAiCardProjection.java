package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.ArtificialJungle;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArtificialJungleAiCardProjection<T extends Card> implements AiCardProjection<ArtificialJungle> {

    @Override
    public Class<ArtificialJungle> getType() {
        return ArtificialJungle.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        if (player.getPlants() <= 0) {
            return new MarsGameRowDifference();
        }

        player.setPlants(player.getPlants() - 1);

        return MarsGameRowDifference.builder()
                .cards(1)
                .build();
    }
}
