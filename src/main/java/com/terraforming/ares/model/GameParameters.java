package com.terraforming.ares.model;

import lombok.*;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GameParameters {
    private List<String> playerNames;
    private List<PlayerDifficulty> computers;
    private boolean mulligan;
    @Singular
    private List<Expansion> expansions;
    private boolean dummyHand;
    private boolean beginner;
    private int[] extraPoints;

    public boolean isCrysis() {
        return expansions.contains(Expansion.CRYSIS);
    }
}
