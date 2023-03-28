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
public class MarsGameRow {

    public MarsGameRow(MarsGameRow copy) {
        this.turn = copy.turn;
        this.winPoints = copy.winPoints;
        this.mcIncome = copy.mcIncome;
        this.mc = copy.mc;
        this.steelIncome = copy.steelIncome;
        this.titaniumIncome = copy.titaniumIncome;
        this.plantsIncome = copy.plantsIncome;
        this.plants = copy.plants;
        this.heatIncome = copy.heatIncome;
        this.heat = copy.heat;
        this.cardsIncome = copy.cardsIncome;
        this.cardsBuilt = copy.cardsBuilt;
        this.extraSeeCards = copy.extraSeeCards;
        this.extraTakeCards = copy.extraTakeCards;

        this.greenCards = copy.greenCards;
        this.redCards = copy.redCards;
        this.blueCards = copy.blueCards;

        this.anaerobicMicroorganisms = copy.anaerobicMicroorganisms;
        this.decomposers = copy.decomposers;
        this.decomposingFungus = copy.decomposingFungus;
        this.ghgProduction = copy.ghgProduction;
        this.nitriteReducting = copy.nitriteReducting;
        this.regolithEaters = copy.regolithEaters;
        this.selfReplicatingBacteria = copy.selfReplicatingBacteria;

        this.scienceTagsCount = copy.scienceTagsCount;

        this.blueCardsList = copy.blueCardsList;

        this.heatEarthIncome = copy.heatEarthIncome;
        this.mcAnimalPlantIncome = copy.mcAnimalPlantIncome;
        this.cardScienceIncome = copy.cardScienceIncome;
        this.mcEarthIncome = copy.mcEarthIncome;
        this.plantPlantIncome = copy.plantPlantIncome;
        this.mcScienceIncome = copy.mcScienceIncome;
        this.mcTwoBuildingIncome = copy.mcTwoBuildingIncome;
        this.mcEnergyIncome = copy.mcEnergyIncome;
        this.mcSpaceIncome = copy.mcSpaceIncome;
        this.heatSpaceIncome = copy.heatSpaceIncome;
        this.mcEventIncome = copy.mcEventIncome;
        this.heatEnergyIncome = copy.heatEnergyIncome;
        this.plantMicrobeIncome = copy.plantMicrobeIncome;
        this.mcForestIncome = copy.mcForestIncome;

        this.oxygenLevel = copy.oxygenLevel;
        this.temperatureLevel = copy.temperatureLevel;
        this.oceansLevel = copy.oceansLevel;

//his opponent                                              
        this.opponentWinPoints = copy.opponentWinPoints;
        this.opponentMcIncome = copy.opponentMcIncome;
        this.opponentMc = copy.opponentMc;
        this.opponentSteelIncome = copy.opponentSteelIncome;
        this.opponentTitaniumIncome = copy.opponentTitaniumIncome;
        this.opponentPlantsIncome = copy.opponentPlantsIncome;
        this.opponentPlants = copy.opponentPlants;
        this.opponentHeatIncome = copy.opponentHeatIncome;
        this.opponentHeat = copy.opponentHeat;
        this.opponentCardsIncome = copy.opponentCardsIncome;
        this.opponentCardsBuilt = copy.opponentCardsBuilt;
        this.opponentExtraSeeCards = copy.opponentExtraSeeCards;
        this.opponentExtraTakeCards = copy.opponentExtraTakeCards;
        this.corporationId = copy.corporationId;
        this.winner = copy.winner;
    }

    //player
    int turn;
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

    int scienceTagsCount;

    //todo add tags?

    //todo add microbes that do not provide VPs directly

    //todo how to depict the current hand?

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

    int corporationId;

    int winner;

    public MarsGameRow applyDifference(MarsGameRowDifference another) {
        this.greenCards += another.greenCards;
        this.redCards += another.redCards;
        this.blueCards += another.blueCards;
        this.winPoints += another.winPoints;
        this.mc += another.mc;
        this.mcIncome += another.mcIncome;

        return this;
    }


    //todo corporation??
}
