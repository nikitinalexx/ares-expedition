package com.terraforming.ares.dataset;

import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 23.02.2026
 */
@Getter
public class PolicyGameResult {
    private final List<PolicyRecord> firstPlayerStates;
    private final List<PolicyRecord> secondPlayerStates;
    private int winner;
    private boolean draw;

    public PolicyGameResult() {
        firstPlayerStates = new ArrayList<>(500);
        secondPlayerStates = new ArrayList<>(500);
    }

    public void addFirstPlayerState(PolicyRecord record) {
        firstPlayerStates.add(record);
    }

    public void addSecondPlayerState(PolicyRecord record) {
        secondPlayerStates.add(record);
    }

    public void markWinner(int winner) {
        this.winner = winner;
    }

    public void markDraw() {
        this.draw = true;
    }

}
