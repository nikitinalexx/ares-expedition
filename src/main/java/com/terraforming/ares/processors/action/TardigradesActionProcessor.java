package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.Tardigrades;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class TardigradesActionProcessor implements BlueActionCardProcessor<Tardigrades> {
    private final CardResourceService cardResourceService;

    @Override
    public Class<Tardigrades> getType() {
        return Tardigrades.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard) {
        cardResourceService.addResources(player, actionCard, 1);

        return null;
    }

}
