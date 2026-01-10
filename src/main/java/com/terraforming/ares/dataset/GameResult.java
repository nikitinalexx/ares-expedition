package com.terraforming.ares.dataset;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.11.2022
 */
@Getter
public class GameResult {
    private final List<float[]> firstPlayerStates;
    private final List<float[]> secondPlayerStates;
    private int winner;
    private boolean draw;

    public GameResult() {
        firstPlayerStates = new ArrayList<>(120);
        secondPlayerStates = new ArrayList<>(120);
    }

    public void addFirstPlayerState(float[] state) {
        firstPlayerStates.add(state);
    }

    public void addSecondPlayerState(float[] state) {
        secondPlayerStates.add(state);
    }

    public void markWinner(int winner) {
        this.winner = winner;
    }

    public void markDraw() {
        this.draw = true;
    }

    public float[] getLocalMax() {
        int vectorSize = firstPlayerStates.get(0).length;
        float[] localMax = new float[vectorSize];

        for (float[] state : firstPlayerStates) {
            for (int i = 0; i < vectorSize; i++) {
                localMax[i] = Math.max(localMax[i], state[i]);
            }
        }

        for (float[] state : secondPlayerStates) {
            for (int i = 0; i < vectorSize; i++) {
                localMax[i] = Math.max(localMax[i], state[i]);
            }
        }
        return localMax;
    }

}
