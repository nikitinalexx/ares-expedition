package com.terraforming.ares.services;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 04.03.2023
 */
@Service
@RequiredArgsConstructor
public class MarsContextProvider {
    private final TerraformingService terraformingService;
    private final CardService cardService;
    private final BuildService buildService;
    private final CrysisService crysisService;
    private final CardResourceService cardResourceService;
    private final WinPointsService winPointsService;

    public MarsContext provide(MarsGame game, Player player, Map<Integer, List<Integer>> inputParams) {
        return MarsContext.builder()
                .game(game)
                .player(player)
                .terraformingService(terraformingService)
                .cardService(cardService)
                .buildService(buildService)
                .crysisService(crysisService)
                .cardResourceService(cardResourceService)
                .winPointsService(winPointsService)
                .inputParams(inputParams)
                .build();
    }

    public MarsContext provide(MarsGame game, Player player) {
        return MarsContext.builder()
                .game(game)
                .player(player)
                .terraformingService(terraformingService)
                .cardService(cardService)
                .buildService(buildService)
                .crysisService(crysisService)
                .cardResourceService(cardResourceService)
                .winPointsService(winPointsService)
                .build();
    }

    public MarsContext provide(MarsGame game) {
        return MarsContext.builder()
                .game(game)
                .terraformingService(terraformingService)
                .cardService(cardService)
                .buildService(buildService)
                .crysisService(crysisService)
                .cardResourceService(cardResourceService)
                .winPointsService(winPointsService)
                .build();
    }

}
