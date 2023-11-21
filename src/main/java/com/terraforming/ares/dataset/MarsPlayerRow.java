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
    public static final int DATA_SIZE_NO_BLUE_CARDS = 61;

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
    int fibrousCompositeScience;

    //todo add tags?
    int scienceTagsCount;
    int jupiterTagsCount;

    int amplifyEffect;
    int cardsPriceEffect;
    int cardForScienceEffect;
    int phaseRevealBonus;
    int eventDiscount;
    int cardDiscount;
    int cardPerEvent;
    int wpPerJupiter;

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

    int[] dynamicEffects = new int[3];

    int[] type1Upgrades;
    int[] type2Upgrades;

    List<Integer> blueCardsList;
}
