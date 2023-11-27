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
    public static final int DATA_SIZE_NO_BLUE_CARDS = 67;

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

    float cards;

    int anaerobicMicroorganisms;
    int decomposers;
    int decomposingFungus;
    int ghgProduction;
    int nitriteReducting;
    int regolithEaters;
    int selfReplicatingBacteria;
    int fibrousCompositeScience;

    int spaceTagsCount;
    int earthTagsCount;
    int eventTagsCount;
    int scienceTagsCount;
    int plantTagsCount;
    int energyTagsCount;
    int buildingTagsCount;
    int animalTagsCount;
    int jupiterTagsCount;
    int microbeTagsCount;

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

    int corporationId;

    List<Integer> blueCardsList;
}
