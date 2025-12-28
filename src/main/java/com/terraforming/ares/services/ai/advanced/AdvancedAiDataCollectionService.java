package com.terraforming.ares.services.ai.advanced;

import com.terraforming.ares.dataset.GameResult;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AdvancedAiDataCollectionService {
    public static final int TABLE_VECTOR_SIZE = 421;

    private static final int HAND_VECTOR_SIZE = 637;

    private final CompleteHandEncoder handEncoder;
    private final CompleteTableEncoder tableEncoder;

    public void collectData(GameResult gameResult, MarsGame marsGame, List<Player> players) {
        Player currentPlayer = players.get(0);
        Player anotherPlayer = players.get(1);

        gameResult.addFirstPlayerState(collectData(marsGame, currentPlayer, anotherPlayer));
        gameResult.addSecondPlayerState(collectData(marsGame, anotherPlayer, currentPlayer));
    }

    private float[] collectData(MarsGame game, Player player, Player anotherPlayer) {
        float[] playerData = new float[TABLE_VECTOR_SIZE + HAND_VECTOR_SIZE];

        tableEncoder.evaluate(game, player, anotherPlayer, playerData, 0);
        handEncoder.evaluate(game, player, anotherPlayer, playerData, TABLE_VECTOR_SIZE);

        return playerData;
    }

}
