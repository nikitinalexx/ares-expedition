package com.terraforming.ares.dataset;

import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarsGameRowDifference {
    float greenCards;
    float redCards;
    float blueCards;
    float winPoints;
    float mc;
    float mcIncome;

    //mutable
    public void add(MarsGameRowDifference another) {
        this.greenCards += another.greenCards;
        this.redCards += another.redCards;
        this.blueCards += another.blueCards;
        this.winPoints += another.winPoints;
        this.mc += another.mc;
        this.mcIncome += another.mcIncome;
    }
}
