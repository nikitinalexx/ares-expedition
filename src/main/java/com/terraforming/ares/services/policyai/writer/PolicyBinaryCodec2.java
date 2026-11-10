package com.terraforming.ares.services.policyai.writer;

import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class PolicyBinaryCodec2 {

    private PolicyBinaryCodec2() {
    }

    // =========================================================
    // WRITE v3 — с gameId и isFirstPlayer (промежуточный формат)
    // =========================================================

    public static void write(DataOutput out, PolicyRecord r) throws IOException {
        out.writeLong(r.gameId);
        out.writeBoolean(r.isFirstPlayer);
        writeBody(out, r);
    }

    // =========================================================
    // WRITE v2 — финальный формат, без gameId/isFirstPlayer
    // =========================================================

    public static void writeFinalized(DataOutput out, PolicyRecord r) throws IOException {
        writeBody(out, r);
    }

    // =========================================================
    // READ v3
    // =========================================================

    public static PolicyRecord read(DataInput in) throws IOException {
        PolicyRecord r = new PolicyRecord();
        r.gameId = in.readLong();
        r.isFirstPlayer = in.readBoolean();
        readBody(in, r);
        return r;
    }

    // =========================================================
    // SHARED BODY
    // =========================================================

    private static void writeBody(DataOutput out, PolicyRecord r) throws IOException {
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
        out.writeBoolean(r.building);
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
        out.writeByte(r.opponentLeftToDiscard);
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
        out.writeByte(r.paymentChoiceContext);

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
        out.writeInt(r.selectedTagMask);

        out.writeFloat(r.outcome);
        out.writeFloat(r.policyDelta);
        out.writeByte(r.chosenHead);
        out.writeShort(r.chosenLocalIndex);
        writeLongArray(out, r.legalActionMask);
        out.writeShort(r.legalHeadMask);

        out.writeBoolean(r.hasDistribution);
        if (r.hasDistribution) {
            out.writeByte(r.distIndices.length);
            for (int i = 0; i < r.distIndices.length; i++) {
                out.writeShort(r.distIndices[i]);
                out.writeFloat(r.distProbs[i]);
            }
        }
    }

    private static void readBody(DataInput in, PolicyRecord r) throws IOException {
        r.oxygenLeft = in.readByte();
        r.temperatureLeft = in.readByte();
        r.openOceans = in.readShort();

        r.lastOceanCards = in.readByte();
        r.lastOceanPlants = in.readByte();
        r.lastOceanMc = in.readByte();

        r.stillCanDoOxygen = in.readBoolean();
        r.stillCanDoTemperature = in.readBoolean();

        r.dummyStateMask = in.readByte();

        readByteArray(in, r.previousPhaseMask);
        readByteArray(in, r.chosenPhaseMask);

        readByteArray(in, r.milestoneInGame);
        readFloatArray(in, r.milestoneMyProgress);
        readFloatArray(in, r.milestoneOpponentProgress);
        readByteArray(in, r.austellarMilestoneOwner);

        readByteArray(in, r.awardInGame);
        readFloatArray(in, r.awardMyProgress);
        readFloatArray(in, r.awardOpponentProgress);

        r.blueActionFirstMask = in.readLong();
        r.blueActionSecondMask = in.readLong();
        r.blueActionSingleMask = in.readInt();

        r.blueExtraActivationsLeft = in.readByte();

        r.builtSpecialDesignLastTurn = in.readBoolean();
        r.unmiTurnAvailable = in.readBoolean();

        r.initialSetup = in.readBoolean();
        r.doingMulligan = in.readBoolean();
        r.pickingCorporation = in.readBoolean();
        r.payingForTheBuild = in.readBoolean();
        r.building = in.readBoolean();
        r.mulliganToDraw = in.readByte();

        r.currentPhaseMask = in.readByte();
        r.choosingPhase1Upgrade = in.readBoolean();
        r.choosingPhase2Upgrade = in.readBoolean();
        r.choosingPhase3Upgrade = in.readBoolean();
        r.choosingPhase4Upgrade = in.readBoolean();
        r.choosingPhase5Upgrade = in.readBoolean();

        r.choosingTag = in.readBoolean();
        r.universalPhaseUpgradeCount = in.readByte();
        r.choosingPhase = in.readBoolean();
        r.sellingCards = in.readBoolean();
        r.helionExchanging = in.readBoolean();

        r.cardsLeftToDiscard = in.readByte();
        r.opponentLeftToDiscard = in.readByte();
        r.marsUniversityDiscardsLeft = in.readByte();
        r.decomposersActivationsLeft = in.readByte();
        r.viralEnhancersActivationsLeft = in.readByte();
        r.animalPutCount = in.readByte();
        r.microbePutCount = in.readByte();

        r.postBuildDiscarding = in.readBoolean();
        r.discardingFromSelected = in.readBoolean();
        r.discardingLastTurn = in.readBoolean();
        r.ceosFavoriteProjectEffect = in.readBoolean();
        r.syntheticCatastropheEffect = in.readBoolean();
        r.importedHydrogenEffect = in.readBoolean();
        r.largeConvoyEffect = in.readBoolean();
        r.localHeatTrappingEffect = in.readBoolean();
        r.cryogenicShipmentEffect = in.readBoolean();
        r.biomedicalImportsEffect = in.readBoolean();
        r.austellarChoosing = in.readBoolean();
        r.conservedBiomeAction = in.readBoolean();
        r.decomposingFungus = in.readBoolean();
        r.extremeColdFungus = in.readBoolean();
        r.farmingCoops = in.readBoolean();
        r.ghgProduction = in.readBoolean();
        r.regolithEaters = in.readBoolean();
        r.nitriteReductingBacteria = in.readBoolean();
        r.selfReplicatingBacteria = in.readBoolean();
        r.greenHouses = in.readBoolean();
        r.matterGenerator = in.readBoolean();
        r.powerInfrastructure = in.readBoolean();
        r.redraftedContracts = in.readBoolean();
        r.fibrousComposite = in.readBoolean();
        r.redraftedDiscarded = in.readByte();

        r.ownedBuildsMask = in.readShort();
        r.paymentChoiceContext = in.readByte();

        readIntArray(in, r.phaseUpgrades);

        readShortArray(in, r.winPointsNoForests);
        readShortArray(in, r.forests);

        readShortArray(in, r.mcIncomeTotal);
        readByteArray(in, r.steelIncome);
        readByteArray(in, r.titaniumIncome);
        readByteArray(in, r.plantsIncome);
        readByteArray(in, r.heatIncome);
        readByteArray(in, r.cardIncome);

        readShortArray(in, r.mc);
        readShortArray(in, r.plants);
        readShortArray(in, r.heat);

        readShortArray(in, r.playedCards);
        readShortArray(in, r.blueCards);
        readShortArray(in, r.greenCards);
        readByteArray(in, r.handSize);

        readByteArray(in, r.spaceTags);
        readByteArray(in, r.earthTags);
        readByteArray(in, r.eventTags);
        readByteArray(in, r.scienceTags);
        readByteArray(in, r.plantTags);
        readByteArray(in, r.energyTags);
        readByteArray(in, r.buildingTags);
        readByteArray(in, r.animalTags);
        readByteArray(in, r.jupiterTags);
        readByteArray(in, r.microbeTags);

        readByteArray(in, r.engineActionCount);
        readIntArray(in, r.corporationMask);
        readByteArray(in, r.arclightResources);

        readLongArray(in, r.playedBlueCards[0]);
        readLongArray(in, r.playedBlueCards[1]);

        readIntArray(in, r.playedGreenCards);
        r.playedRedCardsMask = in.readLong();

        readShortArray(in, r.maxMcIncome);
        readShortArray(in, r.maxHeatIncome);
        readByteArray(in, r.maxPlantsIncome);
        readFloatArray(in, r.maxCardsIncome);

        readByteArray(in, r.researchGrantDynamicTags);

        readByteArray(in, r.animalResources[0]);
        readByteArray(in, r.animalResources[1]);

        readByteArray(in, r.microbeResources[0]);
        readByteArray(in, r.microbeResources[1]);

        readLongArray(in, r.handCardMask);
        readLongArray(in, r.selectableCardMask);
        r.selectedTagMask = in.readInt();

        r.outcome = in.readFloat();
        r.policyDelta = in.readFloat();
        r.chosenHead = in.readByte();
        r.chosenLocalIndex = in.readShort();
        readLongArray(in, r.legalActionMask);
        r.legalHeadMask = in.readShort();

        r.hasDistribution = in.readBoolean();
        if (r.hasDistribution) {
            int len = in.readByte() & 0xFF;
            r.distIndices = new short[len];
            r.distProbs = new float[len];
            for (int i = 0; i < len; i++) {
                r.distIndices[i] = in.readShort();
                r.distProbs[i] = in.readFloat();
            }
        }
    }

    // =========================================================
    // ARRAY HELPERS — WRITE
    // =========================================================

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

    // =========================================================
    // ARRAY HELPERS — READ
    // =========================================================

    private static void readByteArray(DataInput in, byte[] a) throws IOException {
        in.readFully(a);
    }

    private static void readShortArray(DataInput in, short[] a) throws IOException {
        for (int i = 0; i < a.length; i++) a[i] = in.readShort();
    }

    private static void readFloatArray(DataInput in, float[] a) throws IOException {
        for (int i = 0; i < a.length; i++) a[i] = in.readFloat();
    }

    private static void readIntArray(DataInput in, int[] a) throws IOException {
        for (int i = 0; i < a.length; i++) a[i] = in.readInt();
    }

    private static void readLongArray(DataInput in, long[] a) throws IOException {
        for (int i = 0; i < a.length; i++) a[i] = in.readLong();
    }
}