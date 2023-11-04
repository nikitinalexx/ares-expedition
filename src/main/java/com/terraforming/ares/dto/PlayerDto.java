package com.terraforming.ares.dto;

import com.terraforming.ares.model.BuildDto;
import com.terraforming.ares.model.Tag;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
@Builder
@Getter
public class PlayerDto {
    private final String playerUuid;
    private final String name;
    private final List<CardDto> corporations;
    private final List<CardDto> hand;
    private final List<CardDto> played;
    private final Integer corporationId;
    private final Integer phase;
    private final Integer previousPhase;
    private final TurnDto nextTurn;
    private final Map<Integer, Integer> cardResources;
    private final Map<Integer, List<Tag>> cardToTag;
    private final List<Integer> activatedBlueCards;
    private final int blueActionExtraActivationsLeft;
    private final int terraformingRating;
    private final int winPoints;
    private final int forests;
    private final boolean builtSpecialDesignLastTurn;
    private final List<Integer> phaseCards;
    private final List<BuildDto> builds;
    private int extraPoints;

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
