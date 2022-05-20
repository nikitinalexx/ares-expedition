package com.terraforming.ares.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Your opponent
 *
 * Created by oleksii.nikitin
 * Creation date 18.05.2022
 */
@Builder
@Getter
public class AnotherPlayerDto {
    private final Integer phase;
    private final int winPoints;
    private final List<CardDto> hand;
    private final List<CardDto> played;

    private final int terraformingRating;
    private final int forests;

    private final int mc;
    private final int mcIncome;

    private final int cardIncome;

    private final int heat;
    private final int heatIncome;

    private final int plants;
    private final int plantsIncome;

    private final int steelIncome;
    private final int titaniumIncome;
}
