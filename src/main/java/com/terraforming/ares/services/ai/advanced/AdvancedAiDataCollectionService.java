package com.terraforming.ares.services.ai.advanced;

import com.terraforming.ares.dataset.GameResult;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.ai.AiConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AdvancedAiDataCollectionService implements IDataCollect {
    private static final int WIN_POINTS_INDEX_OFFSET = 38;
    private static final int OPPONENT_WIN_POINTS_INDEX_OFFSET = 242;
    private static final int HAND_SIZE_INDEX_OFFSET = 52;
    private static final int OPPONENT_HAND_SIZE_INDEX_OFFSET = 256;
    private static final int PLAYED_TAG_OFFSET = 58;

    private final CompleteTableEncoder tableEncoder;

    public void collectData(GameResult gameResult, MarsGame marsGame, List<Player> players) {
        Player currentPlayer = players.get(0);
        Player anotherPlayer = players.get(1);

        gameResult.addFirstPlayerState(collectData(marsGame, currentPlayer));
        gameResult.addSecondPlayerState(collectData(marsGame, anotherPlayer));
    }


    @Override
    public void modifyPlayerHandSize(float[] data, int newSize) {
        modifyHandSize(data, newSize, HAND_SIZE_INDEX_OFFSET);
    }

    @Override
    public void modifyOpponentHandSize(float[] data, int newSize) {
        modifyHandSize(data, newSize, OPPONENT_HAND_SIZE_INDEX_OFFSET);
    }

    private void modifyHandSize(float[] data, int newSize, int indexOffset) {
        modifyDataInline(data, newSize, indexOffset);
        modifyDataInline(data, Math.max(0, newSize - 10), indexOffset + 1);
        modifyDataInline(data, Math.min(newSize, 10), indexOffset + 2);
    }

    @Override
    public void modifyPlayerWinPoints(float[] data, float wpDelta) {
        modifyWinPoints(data, wpDelta, WIN_POINTS_INDEX_OFFSET);
    }

    @Override
    public void modifyOpponentWinPoints(float[] data, float wpDelta) {
        modifyWinPoints(data, wpDelta, OPPONENT_WIN_POINTS_INDEX_OFFSET);
    }

    @Override
    public void modifyTagCount(float[] data, Tag tag, int tagCountDelta) {
        int indexOffset = PLAYED_TAG_OFFSET + tag.ordinal();

        float tagCount = data[indexOffset] * AiConstants.NORMALIZATION_VECTOR[indexOffset];
        tagCount += tagCountDelta;
        modifyDataInline(data, tagCount, indexOffset);
    }

    private void modifyWinPoints(float[] data, float wpDelta, int indexOffset) {
        float maxPointsOriginal = data[indexOffset] * AiConstants.NORMALIZATION_VECTOR[indexOffset];
        maxPointsOriginal += wpDelta;
        modifyDataInline(data, maxPointsOriginal, indexOffset);
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
