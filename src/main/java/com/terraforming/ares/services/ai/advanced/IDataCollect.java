package com.terraforming.ares.services.ai.advanced;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;

public interface IDataCollect {
    float[] collectData(MarsGame game, Player player);
    void modifyPlayerHandSize(float[] data, int newSize);
    void modifyPlayerWinPoints(float[] data, float wpDelta);

    void modifyOpponentHandSize(float[] data, int newSize);
    void modifyOpponentWinPoints(float[] data, float wpDelta);

    void modifyTagCount(float[] data, Tag tag, int tagCountDelta);
}
