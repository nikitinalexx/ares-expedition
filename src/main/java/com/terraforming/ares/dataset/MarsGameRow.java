package com.terraforming.ares.dataset;

import lombok.*;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.11.2022
 */
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarsGameRow {

    //player
    int turn;

    MarsPlayerRow player;
    MarsPlayerRow opponent;

    int corporationId;

    int oxygenLevel;
    int temperatureLevel;
    int oceansLevel;

    float[] milestones;
    float[] awards;

    int resources;

    //todo how to depict the current hand?

    int winner;

    public MarsGameRow applyDifference(MarsGameRowDifference difference) {
        applyDifference(this.player, difference);

        return this;
    }

    public MarsGameRow applyOpponentDifference(MarsGameRowDifference difference) {
        if (difference != null) {
            applyDifference(this.opponent, difference);
        }

        return this;
    }

    private void applyDifference(MarsPlayerRow row, MarsGameRowDifference difference) {
        row.greenCards += difference.greenCards;
        row.redCards += difference.redCards;
        row.blueCards += difference.blueCards;
        row.winPoints += difference.winPoints;
        row.mc += difference.mc;
        row.mcIncome += difference.mcIncome;
    }


    //todo corporation??
}
