package com.terraforming.ares.services.simulations;

import com.terraforming.ares.dataset.GameResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class GameResultProducer {

    private final BlockingQueue<SampleBatch> queue;
    private final int batchSize;

    private List<float[]> featureBuffer;
    private FloatArrayList labelBuffer;

    public GameResultProducer(BlockingQueue<SampleBatch> queue, int batchSize) {
        this.queue = queue;
        this.batchSize = batchSize;
        this.featureBuffer = new ArrayList<>(batchSize);
        this.labelBuffer = new FloatArrayList(batchSize);
    }

    public synchronized void addResult(GameResult gameResult) throws InterruptedException {
        processPlayerStates(gameResult.getFirstPlayerStates(),
                gameResult.isDraw() ? 0.5f : (gameResult.getWinner() == 1 ? 1f : 0f));

        processPlayerStates(gameResult.getSecondPlayerStates(),
                gameResult.isDraw() ? 0.5f : (gameResult.getWinner() == 2 ? 1f : 0f));
    }

    private void processPlayerStates(LinkedHashSet<FloatArrayWrapper> states, float finalOutcome)
            throws InterruptedException {

        if (states.isEmpty()) return;

        for (FloatArrayWrapper state : states) {
            featureBuffer.add(state.data());
            labelBuffer.add(finalOutcome);
            checkAndFlush();
        }
    }

    private void checkAndFlush() throws InterruptedException {
        if (featureBuffer.size() >= batchSize) {
            queue.put(new SampleBatch(featureBuffer, labelBuffer));
            // Передаём старые буферы в SampleBatch, создаём новые — без копирования
            featureBuffer = new ArrayList<>(batchSize);
            labelBuffer = new FloatArrayList(batchSize);
        }
    }
}

