package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.BrainstormingSession;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BrainstormingSessionAiCardProjection<T extends Card> implements AiCardProjection<BrainstormingSession> {

    @Override
    public Class<BrainstormingSession> getType() {
        return BrainstormingSession.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        return MarsGameRowDifference.builder()
                .mc(Constants.GREEN_CARDS_RATIOO)
                .cards(Constants.RED_CARDS_RATIOO + Constants.BLUE_CARDS_RATIOO)
                .build();
    }

}
