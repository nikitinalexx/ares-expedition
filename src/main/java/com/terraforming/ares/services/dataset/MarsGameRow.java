package com.terraforming.ares.services.dataset;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.11.2022
 */
@Setter
@Getter
@Builder
public class MarsGameRow {
    //player
    private int turn;
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

    private long space;
    private long earth;
    private long event;
    private long science;
    private long plant;
    private long energy;
    private long building;
    private long animal;
    private long jupiter;
    private long microbe;

    private int extraCardsToTake;
    private int extraCardsToSee;

    private float resourceCount;

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

    private List<Integer> cards;

    private int winner;

    public float[] getAsInput() {
        final float[] basicData = {
                turn,
                winPoints,
                mcIncome,
                mc,
                steelIncome,
                titaniumIncome,
                plantsIncome,
                plants,
                heatIncome,
                heat,
                cardsIncome,
                cardsInHand,
                cardsBuilt,
                space,
                earth,
                event,
                science,
                plant,
                energy,
                building,
                animal,
                jupiter,
                microbe,
                extraCardsToTake,
                extraCardsToSee,
                resourceCount,
                oxygenLevel,
                temperatureLevel,
                oceansLevel,
                opponentWinPoints,
                opponentMcIncome,
                opponentMc,
                opponentSteelIncome,
                opponentTitaniumIncome,
                opponentPlantsIncome,
                opponentPlants,
                opponentHeatIncome,
                opponentHeat,
                opponentCardsIncome,
                opponentCardsBuilt,
        };

        int totalSize = basicData.length + cards.size();

        float[] result = new float[totalSize];

        int counter = 0;

        for (int i = 0; i < basicData.length; i++) {
            result[counter++] = basicData[i];
        }

        for (int i = 0; i < cards.size(); i++) {
            result[counter++] = cards.get(i);
        }

        return result;
    }


    //todo corporation??
}
