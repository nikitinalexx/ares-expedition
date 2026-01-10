package com.terraforming.ares.services.simulations;

import com.terraforming.ares.dataset.GameResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class GameResultProducer {

    private final BlockingQueue<SampleBatch> queue;
    private final int batchSize;

    private final List<float[]> featureBuffer = new ArrayList<>();
    private final List<Float> labelBuffer = new ArrayList<>();

    public GameResultProducer(BlockingQueue<SampleBatch> queue, int batchSize) {
        this.queue = queue;
        this.batchSize = batchSize;
    }

    public void addResult(GameResult gameResult) throws InterruptedException {
        float firstLabel = gameResult.isDraw() ? 0.5f : (gameResult.getWinner() == 1 ? 1f : 0f);
        float secondLabel = gameResult.isDraw() ? 0.5f : (gameResult.getWinner() == 2 ? 1f : 0f);

        for (float[] state : gameResult.getFirstPlayerStates()) {
            featureBuffer.add(state);
            labelBuffer.add(firstLabel);
        }
        for (float[] state : gameResult.getSecondPlayerStates()) {
            featureBuffer.add(state);
            labelBuffer.add(secondLabel);
        }

        if (featureBuffer.size() >= batchSize) {
            queue.put(
                    new SampleBatch(
                            new ArrayList<>(featureBuffer),
                            new ArrayList<>(labelBuffer)
                    )
            );
            featureBuffer.clear();
            labelBuffer.clear();
        }
    }
}

