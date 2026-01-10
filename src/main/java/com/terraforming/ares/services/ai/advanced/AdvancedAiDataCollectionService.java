package com.terraforming.ares.services.ai.advanced;

import com.terraforming.ares.dataset.GameResult;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.AiConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AdvancedAiDataCollectionService implements IDataCollect {
    private static final int WIN_POINTS_INDEX_OFFSET = 38;
    private static final int HAND_SIZE_INDEX_OFFSET = 54;

    private final CompleteTableEncoder tableEncoder;

    public void collectData(GameResult gameResult, MarsGame marsGame, List<Player> players) {
        Player currentPlayer = players.get(0);
        Player anotherPlayer = players.get(1);

        gameResult.addFirstPlayerState(collectData(marsGame, currentPlayer));
        gameResult.addSecondPlayerState(collectData(marsGame, anotherPlayer));
    }


    @Override
    public void modifyPlayerHandSize(float[] data, int newSize) {
        modifyDataInline(data, newSize, HAND_SIZE_INDEX_OFFSET);
        modifyDataInline(data, newSize > 10 ? 1 : 0, HAND_SIZE_INDEX_OFFSET + 1);
    }

    @Override
    public void modifyPlayerWinPoints(float[] data, float wpDelta) {
        float maxPointsOriginal = data[WIN_POINTS_INDEX_OFFSET] * AiConstants.NORMALIZATION_VECTOR[WIN_POINTS_INDEX_OFFSET];
        maxPointsOriginal += wpDelta;
        modifyDataInline(data, maxPointsOriginal, WIN_POINTS_INDEX_OFFSET);
    }

    private void modifyDataInline(float[] data, float value, int index) {
        float max = AiConstants.NORMALIZATION_VECTOR[index];
        if (max > 1f) {
            data[index] = value / max;
        } else {
            data[index] = value;
        }
    }

    @Override
    public float[] collectData(MarsGame game, Player player) {
        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = (players.get(0).getUuid().equals(player.getUuid())) ? players.get(1) : players.get(0);

        FeatureWriter featureWriter = new FeatureWriter(AiConstants.TABLE_VECTOR_SIZE);

        tableEncoder.encodeTable(game, player, anotherPlayer, featureWriter);

        return normalize(featureWriter.getData(), AiConstants.NORMALIZATION_VECTOR);
    }

    public static float[] normalize(float[] state, float[] maxVec) {
        int n = state.length;
        float[] normalized = new float[n];

        for (int i = 0; i < n; i++) {
            float max = maxVec[i];
            float value = state[i];

            if (!Float.isFinite(value)) {
                throw new IllegalStateException(
                        "state[" + i + "] is not finite: " + value
                );
            }

            if (max <= 0f) {
                throw new IllegalStateException(
                        "maxVec[" + i + "] <= 0: " + max
                );
            }

            if (max > 1f) {
                normalized[i] = value / max;
            } else {
                // осознанно НЕ нормализуем
                normalized[i] = value;
            }
        }

        return normalized;
    }

}
