package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.ConservedBiome;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@Component
@RequiredArgsConstructor
public class ConservedBiomeActionProcessor implements BlueActionCardProcessor<ConservedBiome> {
    private final CardService cardService;

    @Override
    public Class<ConservedBiome> getType() {
        return ConservedBiome.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard, Map<Integer, List<Integer>> inputParameters) {
        Integer cardIdToAddTo = inputParameters.get(InputFlag.CARD_CHOICE.getId()).get(0);

        Card project = cardService.getCard(cardIdToAddTo);
        player.addResources(project, 1);

        return null;
    }
}
