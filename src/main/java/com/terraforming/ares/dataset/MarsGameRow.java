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

    public MarsGameRow applyDifference(MarsGameRowDifference another) {
        this.player.greenCards += another.greenCards;
        this.player.redCards += another.redCards;
        this.player.blueCards += another.blueCards;
        this.player.winPoints += another.winPoints;
        this.player.mc += another.mc;
        this.player.mcIncome += another.mcIncome;

        return this;
    }

    public MarsGameRow applyOpponentDifference(MarsGameRowDifference another) {
        if (another != null) {
            this.opponent.greenCards += another.greenCards;
            this.opponent.redCards += another.redCards;
            this.opponent.blueCards += another.blueCards;
            this.opponent.winPoints += another.winPoints;
            this.opponent.mc += another.mc;
            this.opponent.mcIncome += another.mcIncome;
        }

        return this;
    }


    //todo corporation??
}
