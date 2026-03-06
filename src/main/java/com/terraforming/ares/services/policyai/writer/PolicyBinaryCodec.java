package com.terraforming.ares.services.policyai.writer;

import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class PolicyBinaryCodec {

    private PolicyBinaryCodec() {
    }

    // =========================================================
    // WRITE
    // =========================================================

    public static void write(DataOutput out, PolicyRecord r) throws IOException {

        out.writeByte(r.oxygenLeft);
        out.writeByte(r.temperatureLeft);
        out.writeShort(r.openOceans);

        out.writeByte(r.lastOceanCards);
        out.writeByte(r.lastOceanPlants);
        out.writeByte(r.lastOceanMc);

        out.writeBoolean(r.stillCanDoOxygen);
        out.writeBoolean(r.stillCanDoTemperature);

        out.writeByte(r.dummyStateMask);

        writeByteArray(out, r.previousPhaseMask);
        writeByteArray(out, r.chosenPhaseMask);

        writeByteArray(out, r.milestoneInGame);
        writeFloatArray(out, r.milestoneMyProgress);
        writeFloatArray(out, r.milestoneOpponentProgress);
        writeByteArray(out, r.austellarMilestoneOwner);

        writeByteArray(out, r.awardInGame);
        writeFloatArray(out, r.awardMyProgress);
        writeFloatArray(out, r.awardOpponentProgress);

        out.writeLong(r.blueActionFirstMask);
        out.writeLong(r.blueActionSecondMask);
        out.writeInt(r.blueActionSingleMask);

        out.writeByte(r.blueExtraActivationsLeft);

        out.writeBoolean(r.builtSpecialDesignLastTurn);
        out.writeBoolean(r.unmiTurnAvailable);

        out.writeBoolean(r.initialSetup);
        out.writeBoolean(r.doingMulligan);
        out.writeBoolean(r.pickingCorporation);
        out.writeBoolean(r.payingForTheBuild);
        out.writeByte(r.mulliganToDraw);

        out.writeByte(r.currentPhaseMask);
        out.writeBoolean(r.choosingPhase1Upgrade);
        out.writeBoolean(r.choosingPhase2Upgrade);
        out.writeBoolean(r.choosingPhase3Upgrade);
        out.writeBoolean(r.choosingPhase4Upgrade);
        out.writeBoolean(r.choosingPhase5Upgrade);

        out.writeBoolean(r.choosingTag);
        out.writeByte(r.universalPhaseUpgradeCount);
        out.writeBoolean(r.choosingPhase);
        out.writeBoolean(r.sellingCards);
        out.writeBoolean(r.helionExchanging);

        out.writeByte(r.cardsLeftToDiscard);
        out.writeByte(r.marsUniversityDiscardsLeft);
        out.writeByte(r.decomposersActivationsLeft);
        out.writeByte(r.viralEnhancersActivationsLeft);
        out.writeByte(r.animalPutCount);
        out.writeByte(r.microbePutCount);

        out.writeBoolean(r.postBuildDiscarding);
        out.writeBoolean(r.discardingFromSelected);
        out.writeBoolean(r.discardingLastTurn);
        out.writeBoolean(r.ceosFavoriteProjectEffect);
        out.writeBoolean(r.syntheticCatastropheEffect);
        out.writeBoolean(r.importedHydrogenEffect);
        out.writeBoolean(r.largeConvoyEffect);
        out.writeBoolean(r.localHeatTrappingEffect);
        out.writeBoolean(r.cryogenicShipmentEffect);
        out.writeBoolean(r.biomedicalImportsEffect);
        out.writeBoolean(r.austellarChoosing);
        out.writeBoolean(r.conservedBiomeAction);
        out.writeBoolean(r.decomposingFungus);
        out.writeBoolean(r.extremeColdFungus);
        out.writeBoolean(r.farmingCoops);
        out.writeBoolean(r.ghgProduction);
        out.writeBoolean(r.regolithEaters);
        out.writeBoolean(r.nitriteReductingBacteria);
        out.writeBoolean(r.selfReplicatingBacteria);
        out.writeBoolean(r.greenHouses);
        out.writeBoolean(r.matterGenerator);
        out.writeBoolean(r.powerInfrastructure);
        out.writeBoolean(r.redraftedContracts);
        out.writeBoolean(r.fibrousComposite);
        out.writeByte(r.redraftedDiscarded);

        out.writeShort(r.ownedBuildsMask);

        writeIntArray(out, r.phaseUpgrades);

        writeShortArray(out, r.winPointsNoForests);
        writeShortArray(out, r.forests);

        writeShortArray(out, r.mcIncomeTotal);
        writeByteArray(out, r.steelIncome);
        writeByteArray(out, r.titaniumIncome);
        writeByteArray(out, r.plantsIncome);
        writeByteArray(out, r.heatIncome);
        writeByteArray(out, r.cardIncome);

        writeShortArray(out, r.mc);
        writeShortArray(out, r.plants);
        writeShortArray(out, r.heat);

        writeShortArray(out, r.playedCards);
        writeShortArray(out, r.blueCards);
        writeShortArray(out, r.greenCards);
        writeByteArray(out, r.handSize);

        writeByteArray(out, r.spaceTags);
        writeByteArray(out, r.earthTags);
        writeByteArray(out, r.eventTags);
        writeByteArray(out, r.scienceTags);
        writeByteArray(out, r.plantTags);
        writeByteArray(out, r.energyTags);
        writeByteArray(out, r.buildingTags);
        writeByteArray(out, r.animalTags);
        writeByteArray(out, r.jupiterTags);
        writeByteArray(out, r.microbeTags);

        writeByteArray(out, r.engineActionCount);
        writeIntArray(out, r.corporationMask);
        writeByteArray(out, r.arclightResources);

        writeLongArray(out, r.playedBlueCards[0]);
        writeLongArray(out, r.playedBlueCards[1]);

        writeIntArray(out, r.playedGreenCards);
        out.writeLong(r.playedRedCardsMask);

        writeShortArray(out, r.maxMcIncome);
        writeShortArray(out, r.maxHeatIncome);
        writeByteArray(out, r.maxPlantsIncome);
        writeFloatArray(out, r.maxCardsIncome);

        writeByteArray(out, r.researchGrantDynamicTags);

        writeByteArray(out, r.animalResources[0]);
        writeByteArray(out, r.animalResources[1]);

        writeByteArray(out, r.microbeResources[0]);
        writeByteArray(out, r.microbeResources[1]);

        writeLongArray(out, r.handCardMask);
        writeLongArray(out, r.selectableCardMask);

        out.writeFloat(r.outcome);
        out.writeByte(r.chosenHead);
        out.writeShort(r.chosenLocalIndex);
    }

    private static void writeByteArray(DataOutput out, byte[] a) throws IOException {
        out.write(a);
    }


    private static void writeShortArray(DataOutput out, short[] a) throws IOException {
        for (short v : a) out.writeShort(v);
    }


    private static void writeFloatArray(DataOutput out, float[] a) throws IOException {
        for (float v : a) out.writeFloat(v);
    }

    private static void writeIntArray(DataOutput out, int[] a) throws IOException {
        for (int v : a) out.writeInt(v);
    }

    private static void writeLongArray(DataOutput out, long[] a) throws IOException {
        for (long v : a) out.writeLong(v);
    }
}

