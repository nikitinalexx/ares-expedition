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
public class GameParameters {
    private List<String> playerNames;
    private List<Boolean> computers;
    private boolean mulligan;
    private List<Expansion> expansions;
}
