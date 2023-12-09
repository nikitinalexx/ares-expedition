package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.BuffedCommunityGardens;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@Component
@RequiredArgsConstructor
public class BuffedCommunityGardensActionProcessor implements BlueActionCardProcessor<BuffedCommunityGardens> {
    private final CommunityGardensActionProcessor processor;

    @Override
    public Class<BuffedCommunityGardens> getType() {
        return BuffedCommunityGardens.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        return processor.process(game, player, actionCard);
    }

}
