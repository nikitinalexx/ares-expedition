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
public class MarsCardRow {
    //player
    private float turn;

    private float[] cards;

    private int winPoints;
    private int mcIncome;
    private int mc;
    private int steelIncome;
    private int titaniumIncome;
    private int plantsIncome;
    private int plants;
    private int heatIncome;
    private int heat;
    private int cardsIncome;
    private int cardsInHand;
    private int cardsBuilt;

    private float greenCards;
    private float redCards;

    private int heatEarthIncome;
    private int mcAnimalPlantIncome;
    private int cardScienceIncome;
    private int mcEarthIncome;
    private int plantPlantIncome;
    private int mcScienceIncome;
    private int mcTwoBuildingIncome;
    private int mcEnergyIncome;
    private int mcSpaceIncome;
    private int heatSpaceIncome;
    private int mcEventIncome;
    private int heatEnergyIncome;
    private int plantMicrobeIncome;
    private int mcForestIncome;

    private int oxygenLevel;
    private int temperatureLevel;
    private int oceansLevel;

    //his opponent
    private int opponentWinPoints;
    private int opponentMcIncome;
    private int opponentMc;
    private int opponentSteelIncome;
    private int opponentTitaniumIncome;
    private int opponentPlantsIncome;
    private int opponentPlants;
    private int opponentHeatIncome;
    private int opponentHeat;
    private int opponentCardsIncome;
    private int opponentCardsBuilt;

    private float winner;

    public float[] getAsOutput() {
        float[] result = new float[cards.length + 44];
        for (int i = 0; i < cards.length; i++) {
            result[i] = cards[i];
        }
        int counter = cards.length;

        result[counter++] = turn;

        result[counter++] = winPoints;
        result[counter++] = mcIncome;
        result[counter++] = mc;
        result[counter++] = steelIncome;
        result[counter++] = titaniumIncome;
        result[counter++] = plantsIncome;
        result[counter++] = plants;
        result[counter++] = heatIncome;
        result[counter++] = heat;
        result[counter++] = cardsIncome;
        result[counter++] = cardsInHand;
        result[counter++] = cardsBuilt;

        result[counter++] = greenCards;
        result[counter++] = redCards;

        result[counter++] = heatEarthIncome;
        result[counter++] = mcAnimalPlantIncome;
        result[counter++] = cardScienceIncome;
        result[counter++] = mcEarthIncome;
        result[counter++] = plantPlantIncome;
        result[counter++] = mcScienceIncome;
        result[counter++] = mcTwoBuildingIncome;
        result[counter++] = mcEnergyIncome;
        result[counter++] = mcSpaceIncome;
        result[counter++] = heatSpaceIncome;
        result[counter++] = mcEventIncome;
        result[counter++] = heatEnergyIncome;
        result[counter++] = plantMicrobeIncome;
        result[counter++] = mcForestIncome;

        result[counter++] = oxygenLevel;
        result[counter++] = temperatureLevel;
        result[counter++] = oceansLevel;


        result[counter++] = opponentWinPoints;
        result[counter++] = opponentMcIncome;
        result[counter++] = opponentMc;
        result[counter++] = opponentSteelIncome;
        result[counter++] = opponentTitaniumIncome;
        result[counter++] = opponentPlantsIncome;
        result[counter++] = opponentPlants;
        result[counter++] = opponentHeatIncome;
        result[counter++] = opponentHeat;
        result[counter++] = opponentCardsIncome;
        result[counter++] = opponentCardsBuilt;

        result[counter++] = winner;


        return result;
    }

    public float[] getAsInput() {
        float[] result = new float[cards.length + 43];
        for (int i = 0; i < cards.length; i++) {
            result[i] = cards[i];
        }
        int counter = cards.length;

        result[counter++] = turn;

        result[counter++] = winPoints;
        result[counter++] = mcIncome;
        result[counter++] = mc;
        result[counter++] = steelIncome;
        result[counter++] = titaniumIncome;
        result[counter++] = plantsIncome;
        result[counter++] = plants;
        result[counter++] = heatIncome;
        result[counter++] = heat;
        result[counter++] = cardsIncome;
        result[counter++] = cardsInHand;
        result[counter++] = cardsBuilt;

        result[counter++] = greenCards;
        result[counter++] = redCards;

        result[counter++] = heatEarthIncome;
        result[counter++] = mcAnimalPlantIncome;
        result[counter++] = cardScienceIncome;
        result[counter++] = mcEarthIncome;
        result[counter++] = plantPlantIncome;
        result[counter++] = mcScienceIncome;
        result[counter++] = mcTwoBuildingIncome;
        result[counter++] = mcEnergyIncome;
        result[counter++] = mcSpaceIncome;
        result[counter++] = heatSpaceIncome;
        result[counter++] = mcEventIncome;
        result[counter++] = heatEnergyIncome;
        result[counter++] = plantMicrobeIncome;
        result[counter++] = mcForestIncome;

        result[counter++] = oxygenLevel;
        result[counter++] = temperatureLevel;
        result[counter++] = oceansLevel;


        result[counter++] = opponentWinPoints;
        result[counter++] = opponentMcIncome;
        result[counter++] = opponentMc;
        result[counter++] = opponentSteelIncome;
        result[counter++] = opponentTitaniumIncome;
        result[counter++] = opponentPlantsIncome;
        result[counter++] = opponentPlants;
        result[counter++] = opponentHeatIncome;
        result[counter++] = opponentHeat;
        result[counter++] = opponentCardsIncome;
        result[counter++] = opponentCardsBuilt;


        return result;
    }

}
