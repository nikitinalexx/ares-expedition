package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.blue.MatterGenerator;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

@Component
public class MatterGeneratorAiCardProjection<T extends Card> implements AiCardProjection<MatterGenerator> {
    @Override
    public Class<MatterGenerator> getType() {
        return MatterGenerator.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        //todo it might value an average card too good to discard for 6 MC but a bad card may be worth 6 MC
        int cardsAvailable = player.getHand().size() + (int) (initialDifference.getBlueCards() + initialDifference.getRedCards() + initialDifference.getGreenCards());

        if (cardsAvailable < 1) {
            return new MarsGameRowDifference();
        }

        player.setMc(player.getMc() + 6);

        return MarsGameRowDifference.builder()
                .greenCards(-Constants.GREEN_CARDS_RATIO)
                .redCards(-Constants.RED_CARDS_RATIO)
                .blueCards(-Constants.BLUE_CARDS_RATIO)
                .build();
    }
}
