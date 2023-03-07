package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.ResolveOceanDetrimentTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CrisisDetrimentService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class ResolveOceanDetrimentTurnProcessor implements TurnProcessor<ResolveOceanDetrimentTurn> {
    private final TerraformingService terraformingService;
    private final CrisisDetrimentService crisisDetrimentService;
    private final MarsContextProvider contextProvider;

    @Override
    public TurnType getType() {
        return TurnType.RESOLVE_OCEAN_DETRIMENT;
    }

    @Override
    public TurnResponse processTurn(ResolveOceanDetrimentTurn turn, MarsGame game) {
        final List<Integer> input = turn.getInput().get(InputFlag.CRYSIS_INPUT_FLAG.getId());
        int choice = input.get(0);

        int oceanDetrimentSize = crisisDetrimentService.getOceanDetrimentSize(game);

        if (choice == InputFlag.CRYSIS_INPUT_OPTION_1.getId()) {
            for (int i = 0; i < oceanDetrimentSize; i++) {
                terraformingService.reduceTemperature(contextProvider.provide(game));
            }
        } else if (choice == InputFlag.CRYSIS_INPUT_OPTION_2.getId()) {
            for (int i = 0; i < oceanDetrimentSize; i++) {
                terraformingService.reduceOxygen(contextProvider.provide(game));
            }
        }
        return null;
    }
}
