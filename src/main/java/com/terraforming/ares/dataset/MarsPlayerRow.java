package com.terraforming.ares.dataset;

import lombok.*;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 28.03.2023
 */
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarsPlayerRow {
    public static final int DATA_SIZE_NO_BLUE_CARDS = 38;

    float winPoints;
    float mcIncome;
    float mc;
    int steelIncome;
    int titaniumIncome;
    int plantsIncome;
    int plants;
    int heatIncome;
    int heat;
    int cardsIncome;
    int cardsBuilt;
    int extraSeeCards;
    int extraTakeCards;

    float greenCards;
    float redCards;
    float blueCards;

    int anaerobicMicroorganisms;
    int decomposers;
    int decomposingFungus;
    int ghgProduction;
    int nitriteReducting;
    int regolithEaters;
    int selfReplicatingBacteria;

    //todo add tags?
    int scienceTagsCount;

    int heatEarthIncome;
    int mcAnimalPlantIncome;
    int cardScienceIncome;
    int mcEarthIncome;
    int plantPlantIncome;
    int mcScienceIncome;
    int mcTwoBuildingIncome;
    int mcEnergyIncome;
    int mcSpaceIncome;
    int heatSpaceIncome;
    int mcEventIncome;
    int heatEnergyIncome;
    int plantMicrobeIncome;
    int mcForestIncome;

    List<Integer> blueCardsList;
}
