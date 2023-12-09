package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.Penguins;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@Service
@RequiredArgsConstructor
public class PenguinsActionProcessor implements BlueActionCardProcessor<Penguins> {
    private final CardResourceService cardResourceService;

    @Override
    public Class<Penguins> getType() {
        return Penguins.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        cardResourceService.addResources(player, actionCard, 1);
        return null;
    }
}
