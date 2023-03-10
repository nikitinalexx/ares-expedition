package com.terraforming.ares.dto;

import com.terraforming.ares.model.Tag;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Your opponent
 *
 * Created by oleksii.nikitin
 * Creation date 18.05.2022
 */
@Builder
@Getter
public class AnotherPlayerDto {
    private final String playerUuid;
    private final Integer phase;
    private final String name;
    private final int winPoints;
    private final List<CardDto> played;
    private final Map<Integer, Integer> cardResources;
    private final Map<Integer, List<Tag>> cardToTag;
    private final List<Integer> phaseCards;

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

    private final int austellarMilestone;
}
