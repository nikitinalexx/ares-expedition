package com.terraforming.ares.mars;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.services.CardFactory;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

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
    //todo is it good?
    private int cardsBuilt;

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
