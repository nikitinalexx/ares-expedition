package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.Sawmill;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 03.12.2023
 */
@Service
@RequiredArgsConstructor
public class SawmillActionProcessor implements BlueActionCardProcessor<Sawmill> {
    private final TerraformingService terraformingService;
    private final MarsContextProvider marsContextProvider;
    private final CardService cardService;

    @Override
    public Class<Sawmill> getType() {
        return Sawmill.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard, Map<Integer, List<Integer>> inputParameters) {
        int plantTagsPlayed = cardService.countPlayedTags(player, Set.of(Tag.PLANT));

        int infrastructurePrice = Math.max(0, 10 - 2 * plantTagsPlayed);


        player.setMc(player.getMc() - infrastructurePrice);

        terraformingService.increaseInfrastructure(marsContextProvider.provide(game, player, inputParameters));

        return null;
    }

}
