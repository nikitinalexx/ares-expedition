package com.terraforming.ares.dataset;

import com.terraforming.ares.services.simulations.FloatArrayWrapper;
import lombok.Getter;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 26.11.2022
 */
@Getter
public class GameResult {
    private final LinkedHashSet<FloatArrayWrapper> firstPlayerStates;
    private final LinkedHashSet<FloatArrayWrapper> secondPlayerStates;
    private int winner;
    private boolean draw;

    public GameResult() {
        firstPlayerStates = new LinkedHashSet<>();
        secondPlayerStates = new LinkedHashSet<>();
    }

    public void addFirstPlayerState(float[] state) {
        firstPlayerStates.add(new FloatArrayWrapper(state));
    }

    public void addSecondPlayerState(float[] state) {
        secondPlayerStates.add(new FloatArrayWrapper(state));
    }

    public void markWinner(int winner) {
        this.winner = winner;
    }

    public void markDraw() {
        this.draw = true;
    }

}
