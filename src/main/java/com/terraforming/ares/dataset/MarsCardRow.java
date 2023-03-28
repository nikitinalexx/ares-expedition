package com.terraforming.ares.dataset;

import lombok.*;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.11.2022
 */
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarsCardRow {
    //player

    MarsCardAiData cardData;

    int turn;

    float winPoints;
    int mcIncome;
    int mc;
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

    List<Integer> blueCardsList;

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

    int spaceTags;
    int earthTags;
    int eventTags;
    int scienceTags;
    int plantTags;
    int energyTags;
    int buildingTags;
    int animalTags;
    int jupiterTags;
    int microbeTags;

    //todo all blue cards?

    int oxygenLevel;
    int temperatureLevel;
    int oceansLevel;

    //his opponent
    float opponentWinPoints;
    int opponentMcIncome;
    int opponentMc;
    int opponentSteelIncome;
    int opponentTitaniumIncome;
    int opponentPlantsIncome;
    int opponentPlants;
    int opponentHeatIncome;
    int opponentHeat;
    int opponentCardsIncome;
    int opponentCardsBuilt;
    int opponentExtraSeeCards;
    int opponentExtraTakeCards;

    int winner;

}
