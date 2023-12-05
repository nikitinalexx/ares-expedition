package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.InterplanetarySuperhighway;
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
public class InterplanetarySuperhighwayActionProcessor implements BlueActionCardProcessor<InterplanetarySuperhighway> {
    private final TerraformingService terraformingService;
    private final MarsContextProvider marsContextProvider;
    private final CardService cardService;

    @Override
    public Class<InterplanetarySuperhighway> getType() {
        return InterplanetarySuperhighway.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard, Map<Integer, List<Integer>> inputParameters) {
        int scienceTagsPlayed = cardService.countPlayedTags(player, Set.of(Tag.SCIENCE));

        int infrastructurePrice = 10;

        if (scienceTagsPlayed >= 4) {
            infrastructurePrice -= 5;
        }
        player.setMc(player.getMc() - infrastructurePrice);

        terraformingService.increaseInfrastructure(marsContextProvider.provide(game, player, inputParameters));

        return null;
    }

}
