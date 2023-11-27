package com.terraforming.ares.dataset;

import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarsGameRowDifference {
    float cards;
    float winPoints;
    float mc;
    float mcIncome;

    //mutable
    public void add(MarsGameRowDifference another) {
        this.cards += another.cards;
        this.winPoints += another.winPoints;
        this.mc += another.mc;
        this.mcIncome += another.mcIncome;
    }
}
