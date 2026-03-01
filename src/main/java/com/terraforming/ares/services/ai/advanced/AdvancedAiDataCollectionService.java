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
    private static final int HAND_SIZE_INDEX_OFFSET = 89;
    private static final int OPPONENT_HAND_SIZE_INDEX_OFFSET = 267;
    private static final int PLAYED_TAG_OFFSET = 90;
    private static final int OPPONENT_PLAYED_TAG_OFFSET = 268;

    private final CompleteTableEncoder tableEncoder;
    private final CompleteHandEncoder handEncoder;

    @Override
    public void collectGameData(GameResult gameResult, MarsGame marsGame, List<Player> players) {
        Player currentPlayer = players.get(0);
        Player anotherPlayer = players.get(1);

        gameResult.addFirstPlayerState(collectData(marsGame, currentPlayer.getUuid()));
        gameResult.addSecondPlayerState(collectData(marsGame, anotherPlayer.getUuid()));
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
    }

    @Override
    public void modifyTagCount(float[] data, Tag tag, int tagCountDelta) {
        int indexOffset = PLAYED_TAG_OFFSET + tag.ordinal();

        float tagCount = data[indexOffset] * AiConstants.NORMALIZATION_VECTOR[indexOffset];
        tagCount += tagCountDelta;
        modifyDataInline(data, tagCount, indexOffset);
    }

    @Override
    public void modifyOpponentTagCount(float[] data, Tag tag, int tagCountDelta) {
        int indexOffset = OPPONENT_PLAYED_TAG_OFFSET + tag.ordinal();

        float tagCount = data[indexOffset] * AiConstants.NORMALIZATION_VECTOR[indexOffset];
        tagCount += tagCountDelta;
        modifyDataInline(data, tagCount, indexOffset);
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
    public float[] collectData(MarsGame game, String playerUuid) {
        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player player = game.getPlayerByUuid(playerUuid);
        Player anotherPlayer = (players.get(0).getUuid().equals(player.getUuid())) ? players.get(1) : players.get(0);

        FeatureWriter featureWriter = new FeatureWriter(AiConstants.TABLE_VECTOR_SIZE + AiConstants.HAND_VECTOR_SIZE);

        tableEncoder.encodeTable(game, player, anotherPlayer, featureWriter);
        handEncoder.encodeHand(game, player, anotherPlayer, featureWriter);

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
