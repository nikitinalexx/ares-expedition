package com.terraforming.ares.dataset;

import lombok.*;

/**
 * Created by oleksii.nikitin
 * Creation date 26.11.2022
 */
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarsGameRow {
    //player
    int turn;
    int winPoints;
    int mcIncome;
    int mc;
    int steelIncome;
    int titaniumIncome;
    int plantsIncome;
    int plants;
    int heatIncome;
    int heat;
    int cardsIncome;
    float cardsInHand;
    int cardsBuilt;

    float greenCards;
    float redCards;

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

    int oxygenLevel;
    int temperatureLevel;
    int oceansLevel;

    //his opponent
    int opponentWinPoints;
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

    int corporationId;

    int winner;


    //todo corporation??
}
