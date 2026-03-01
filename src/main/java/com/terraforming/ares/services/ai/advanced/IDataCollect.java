package com.terraforming.ares.services.ai.advanced;

import com.terraforming.ares.dataset.GameResult;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;

import java.util.List;

public interface IDataCollect {
    void collectGameData(GameResult gameResult, MarsGame marsGame, List<Player> players);

    float[] collectData(MarsGame game, String playerUuid);

    void modifyPlayerHandSize(float[] data, int newSize);

    void modifyOpponentHandSize(float[] data, int newSize);

    void modifyTagCount(float[] data, Tag tag, int tagCountDelta);
    void modifyOpponentTagCount(float[] data, Tag tag, int tagCountDelta);
}
