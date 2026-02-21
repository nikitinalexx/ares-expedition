package com.terraforming.ares.services.ai.network2.projection;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.advanced.IDataCollect;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BaseChanceProjectionService {
    private final IDataCollect iDataCollect;

    public BaseChanceProjectionTask createBaseChanceProjectionTask(MarsGame game, Player player) {
        return new BaseChanceProjectionTask(iDataCollect.collectData(game, player.getUuid()));
    }

}
