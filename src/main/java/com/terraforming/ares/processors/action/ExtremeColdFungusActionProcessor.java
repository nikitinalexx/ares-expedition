package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.ExtremeColdFungus;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@Component
@RequiredArgsConstructor
public class ExtremeColdFungusActionProcessor implements BlueActionCardProcessor<ExtremeColdFungus> {
    private final CardService cardService;

    @Override
    public Class<ExtremeColdFungus> getType() {
        return ExtremeColdFungus.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard, Map<Integer, List<Integer>> inputParameters) {
        if (!CollectionUtils.isEmpty(inputParameters.get(InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.getId()))) {
            player.setPlants(player.getPlants() + 1);
        } else {
            Integer cardToAddToId = inputParameters.get(InputFlag.EXTREME_COLD_FUNGUS_PUT_MICROBE.getId()).get(0);

            Card cardToAddTo = cardService.getCard(cardToAddToId);

            player.addResources(cardToAddTo, 1);
        }

        return null;
    }
}
