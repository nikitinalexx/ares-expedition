package com.terraforming.ares.services.ai.advanced;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;

public interface IDataCollect {
    float[] collectData(MarsGame game, Player player);
    void modifyPlayerHandSize(float[] data, int newSize);
    void modifyPlayerWinPoints(float[] data, float wpDelta);
}
