package com.terraforming.ares.services.policyai.encoder;

import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import org.springframework.stereotype.Service;

import static com.terraforming.ares.services.policyai.encoder.PolicyTableTagIncomeFeature.ENGINE_ACTIONS;

/**
 * Конвертирует PolicyRecord в float[1559]:
 *   [0..698]    = table  (699 элементов)
 *   [699..1290] = hand   (592 элемента)
 *   [1291..1558]= selectableCards (268 элементов)
 *
 * Порядок полей строго соответствует PolicyBatchLoader (чтение в INDArray).
 */
@Service
public class PolicyRecordFeatureConverter {

    public static final int TABLE_SIZE    = 699;
    public static final int HAND_SIZE     = 592;
    public static final int SEL_SIZE      = 268;
    public static final int TOTAL_SIZE    = TABLE_SIZE + HAND_SIZE + SEL_SIZE; // 1559

    private static final int MILESTONE_COUNT = AiConstants.MILESTONE_TYPES.size();
    private static final int AWARD_COUNT     = AiConstants.AWARD_TYPES.size();
    private static final int ENGINE_COUNT    = 2 * ENGINE_ACTIONS;

    // Нормализационные константы — должны совпадать с PolicyBatchLoader
    private static final float[] TAG_MAX = NormalizerConstants.tagMax; // float[10]

    // -------------------------------------------------------------------------

    public float[] convert(PolicyRecord r) {
        float[] out = new float[TOTAL_SIZE];
        int tc = 0; // курсор table
        int hc = TABLE_SIZE; // курсор hand (смещён в общем массиве)
        int sc = TABLE_SIZE + HAND_SIZE; // курсор selectable

        // =====================================================================
        // TABLE_AND_HAND — глобальные параметры планеты
        // =====================================================================

        float oxygenLeft = (r.oxygenLeft & 0xFF) / NormalizerConstants.oxygenLeft;
        out[tc++] = oxygenLeft;
        out[hc++] = oxygenLeft;

        float temperatureLeft = (r.temperatureLeft & 0xFF) / NormalizerConstants.temperatureLeft;
        out[tc++] = temperatureLeft;
        out[hc++] = temperatureLeft;

        // openOceans — 9 бит
        tc = putBits(out, tc, r.openOceans, 9);
        hc = putBits(out, hc, r.openOceans, 9);

        float lOceanCards = r.lastOceanCards & 0xFF;
        out[tc++] = lOceanCards;
        out[hc++] = lOceanCards;

        float lOceanPlants = (r.lastOceanPlants & 0xFF) / NormalizerConstants.lastOceanPlants;
        out[tc++] = lOceanPlants;
        out[hc++] = lOceanPlants;

        float lOceanMc = (r.lastOceanMc & 0xFF) / NormalizerConstants.lastOceanMc;
        out[tc++] = lOceanMc;
        out[hc++] = lOceanMc;

        float stillOxygen = r.stillCanDoOxygen ? 1f : 0f;
        out[tc++] = stillOxygen;
        out[hc++] = stillOxygen;

        float stillTemp = r.stillCanDoTemperature ? 1f : 0f;
        out[tc++] = stillTemp;
        out[hc++] = stillTemp;

        // =====================================================================
        // TABLE-only — phase masks
        // =====================================================================

        // dummyStateMask — 5 бит
        tc = putBits(out, tc, r.dummyStateMask, 5);

        // previousPhaseMask[2] — 5 бит каждый
        for (int i = 0; i < 2; i++) tc = putBits(out, tc, r.previousPhaseMask[i], 5);

        // chosenPhaseMask[2] — 5 бит каждый
        for (int i = 0; i < 2; i++) tc = putBits(out, tc, r.chosenPhaseMask[i], 5);

        // =====================================================================
        // TABLE_AND_HAND — milestones
        // =====================================================================

        for (int i = 0; i < MILESTONE_COUNT; i++) {
            float v = r.milestoneInGame[i] & 0xFF;
            out[tc++] = v;
            out[hc++] = v;
        }

        for (int i = 0; i < MILESTONE_COUNT; i++) {
            float v = r.milestoneMyProgress[i];
            out[tc++] = v;
            out[hc++] = v;
        }

        // milestoneOpponentProgress — только TABLE
        for (int i = 0; i < MILESTONE_COUNT; i++) out[tc++] = r.milestoneOpponentProgress[i];

        // austellarMilestoneOwner — TABLE_AND_HAND
        for (int i = 0; i < MILESTONE_COUNT; i++) {
            float v = r.austellarMilestoneOwner[i];
            out[tc++] = v;
            out[hc++] = v;
        }

        // =====================================================================
        // TABLE_AND_HAND — awards
        // =====================================================================

        for (int i = 0; i < AWARD_COUNT; i++) {
            float v = r.awardInGame[i] & 0xFF;
            out[tc++] = v;
            out[hc++] = v;
        }

        for (int i = 0; i < AWARD_COUNT; i++) {
            float v = r.awardMyProgress[i] / NormalizerConstants.awardMyProgress;
            out[tc++] = v;
            out[hc++] = v;
        }

        // awardOpponentProgress — только TABLE
        for (int i = 0; i < AWARD_COUNT; i++) {
            out[tc++] = r.awardOpponentProgress[i] / NormalizerConstants.awardOpponentProgress;
        }

        // =====================================================================
        // TABLE-only — blue action masks
        // =====================================================================

        tc = putBits(out, tc, r.blueActionFirstMask,  37);
        tc = putBits(out, tc, r.blueActionSecondMask, 37);
        tc = putBits(out, tc, r.blueActionSingleMask, 12);

        out[tc++] = (r.blueExtraActivationsLeft & 0xFF) / NormalizerConstants.blueExtraActivationsLeft;
        out[tc++] = r.builtSpecialDesignLastTurn ? 1f : 0f;
        out[tc++] = r.unmiTurnAvailable ? 1f : 0f;

        // =====================================================================
        // TABLE-only — action context
        // =====================================================================

        out[tc++] = r.initialSetup       ? 1f : 0f;
        out[tc++] = r.doingMulligan      ? 1f : 0f;
        out[tc++] = r.pickingCorporation ? 1f : 0f;
        out[tc++] = r.payingForTheBuild  ? 1f : 0f;
        out[tc++] = (r.mulliganToDraw & 0xFF) / NormalizerConstants.mulliganToDraw;

        tc = putBits(out, tc, r.currentPhaseMask, 5);

        out[tc++] = r.choosingPhase1Upgrade ? 1f : 0f;
        out[tc++] = r.choosingPhase2Upgrade ? 1f : 0f;
        out[tc++] = r.choosingPhase3Upgrade ? 1f : 0f;
        out[tc++] = r.choosingPhase4Upgrade ? 1f : 0f;
        out[tc++] = r.choosingPhase5Upgrade ? 1f : 0f;
        out[tc++] = r.choosingTag                     ? 1f : 0f;
        out[tc++] = (r.universalPhaseUpgradeCount & 0xFF) / NormalizerConstants.universalPhaseUpgradeCount;
        out[tc++] = r.choosingPhase                   ? 1f : 0f;
        out[tc++] = r.sellingCards                    ? 1f : 0f;
        out[tc++] = r.helionExchanging                ? 1f : 0f;
        out[tc++] = (r.cardsLeftToDiscard & 0xFF)           / NormalizerConstants.cardsLeftToDiscard;
        out[tc++] = (r.marsUniversityDiscardsLeft & 0xFF)   / NormalizerConstants.marsUniversityDiscardsLeft;
        out[tc++] = (r.decomposersActivationsLeft & 0xFF)   / NormalizerConstants.decomposersActivationsLeft;
        out[tc++] = (r.viralEnhancersActivationsLeft & 0xFF)/ NormalizerConstants.viralEnhancersActivationsLeft;
        out[tc++] = (r.animalPutCount & 0xFF)               / NormalizerConstants.animalPutCount;
        out[tc++] = (r.microbePutCount & 0xFF)              / NormalizerConstants.microbePutCount;
        out[tc++] = r.postBuildDiscarding       ? 1f : 0f;
        out[tc++] = r.discardingFromSelected    ? 1f : 0f;
        out[tc++] = r.discardingLastTurn        ? 1f : 0f;
        out[tc++] = r.ceosFavoriteProjectEffect ? 1f : 0f;
        out[tc++] = r.syntheticCatastropheEffect? 1f : 0f;
        out[tc++] = r.importedHydrogenEffect    ? 1f : 0f;
        out[tc++] = r.largeConvoyEffect         ? 1f : 0f;
        out[tc++] = r.localHeatTrappingEffect   ? 1f : 0f;
        out[tc++] = r.cryogenicShipmentEffect   ? 1f : 0f;
        out[tc++] = r.biomedicalImportsEffect   ? 1f : 0f;
        out[tc++] = r.austellarChoosing         ? 1f : 0f;
        out[tc++] = r.conservedBiomeAction      ? 1f : 0f;
        out[tc++] = r.decomposingFungus         ? 1f : 0f;
        out[tc++] = r.extremeColdFungus         ? 1f : 0f;
        out[tc++] = r.farmingCoops              ? 1f : 0f;
        out[tc++] = r.ghgProduction             ? 1f : 0f;
        out[tc++] = r.regolithEaters            ? 1f : 0f;
        out[tc++] = r.nitriteReductingBacteria  ? 1f : 0f;
        out[tc++] = r.selfReplicatingBacteria   ? 1f : 0f;
        out[tc++] = r.greenHouses               ? 1f : 0f;
        out[tc++] = r.matterGenerator           ? 1f : 0f;
        out[tc++] = r.powerInfrastructure       ? 1f : 0f;
        out[tc++] = r.redraftedContracts        ? 1f : 0f;
        out[tc++] = r.fibrousComposite          ? 1f : 0f;
        out[tc++] = (r.redraftedDiscarded & 0xFF) / NormalizerConstants.redraftedDiscarded;

        // ownedBuildsMask — 13 бит
        tc = putBits(out, tc, r.ownedBuildsMask, 13);

        // =====================================================================
        // TABLE_AND_HAND — phaseUpgrades[2] — 10 бит каждый
        // =====================================================================

        for (int i = 0; i < 2; i++) {
            tc = putBits(out, tc, r.phaseUpgrades[i], 10);
            hc = putBits(out, hc, r.phaseUpgrades[i], 10);
        }

        // TABLE-only — winPointsNoForests[2]
        for (int i = 0; i < 2; i++) out[tc++] = r.winPointsNoForests[i] / NormalizerConstants.winPointsNoForests;

        // TABLE_AND_HAND — forests[2]
        for (int i = 0; i < 2; i++) {
            float v = r.forests[i] / NormalizerConstants.forests;
            out[tc++] = v;
            out[hc++] = v;
        }

        // =====================================================================
        // TABLE_AND_HAND — incomes
        // =====================================================================

        for (int i = 0; i < 2; i++) { float v = r.mcIncomeTotal[i]  / NormalizerConstants.mcIncomeTotal;  out[tc++] = v; out[hc++] = v; }
        for (int i = 0; i < 2; i++) { float v = (r.steelIncome[i]    & 0xFF) / NormalizerConstants.steelIncome;    out[tc++] = v; out[hc++] = v; }
        for (int i = 0; i < 2; i++) { float v = (r.titaniumIncome[i] & 0xFF) / NormalizerConstants.titaniumIncome; out[tc++] = v; out[hc++] = v; }
        for (int i = 0; i < 2; i++) { float v = (r.plantsIncome[i]   & 0xFF) / NormalizerConstants.plantsIncome;   out[tc++] = v; out[hc++] = v; }
        for (int i = 0; i < 2; i++) { float v = (r.heatIncome[i]     & 0xFF) / NormalizerConstants.heatIncome;     out[tc++] = v; out[hc++] = v; }
        for (int i = 0; i < 2; i++) { float v = (r.cardIncome[i]     & 0xFF) / NormalizerConstants.cardIncome;     out[tc++] = v; out[hc++] = v; }

        // =====================================================================
        // TABLE_AND_HAND — resources
        // =====================================================================

        for (int i = 0; i < 2; i++) { short v = (short) (r.mc[i]     / NormalizerConstants.mc);     out[tc++] = v; out[hc++] = v; }
        for (int i = 0; i < 2; i++) { float v = r.plants[i] / NormalizerConstants.plants;  out[tc++] = v; out[hc++] = v; }
        for (int i = 0; i < 2; i++) { float v = r.heat[i]   / NormalizerConstants.heat;    out[tc++] = v; out[hc++] = v; }

        // =====================================================================
        // TABLE_AND_HAND — card counts
        // =====================================================================

        for (int i = 0; i < 2; i++) { float v = r.playedCards[i] / NormalizerConstants.playedCards; out[tc++] = v; out[hc++] = v; }
        for (int i = 0; i < 2; i++) { float v = r.blueCards[i]   / NormalizerConstants.blueCards;   out[tc++] = v; out[hc++] = v; }
        for (int i = 0; i < 2; i++) { float v = r.greenCards[i]  / NormalizerConstants.greenCards;  out[tc++] = v; out[hc++] = v; }
        for (int i = 0; i < 2; i++) { float v = (r.handSize[i] & 0xFF) / NormalizerConstants.handSize; out[tc++] = v; out[hc++] = v; }

        // =====================================================================
        // TABLE_AND_HAND — tags (10 типов * 2 игрока)
        // порядок: space, earth, event, science, plant, energy, building, animal, jupiter, microbe
        // =====================================================================

        byte[][] allTags = {
                r.spaceTags, r.earthTags, r.eventTags, r.scienceTags, r.plantTags,
                r.energyTags, r.buildingTags, r.animalTags, r.jupiterTags, r.microbeTags
        };

        for (int tag = 0; tag < 10; tag++) {
            float maxVal = TAG_MAX[tag];
            for (int i = 0; i < 2; i++) {
                float v = (allTags[tag][i] & 0xFF) / maxVal;
                out[tc++] = v;
                out[hc++] = v;
            }
        }

        // =====================================================================
        // TABLE_AND_HAND — engineActionCount[ENGINE_COUNT]
        // =====================================================================

        for (int i = 0; i < ENGINE_COUNT; i++) {
            float v = r.engineActionCount[i] & 0xFF;
            if (i % ENGINE_COUNT == 3) {
                v /= NormalizerConstants.engineActionCount;
            }
            out[tc++] = v;
            out[hc++] = v;
        }

        // =====================================================================
        // TABLE_AND_HAND — corporationMask[2] — 25 бит каждый
        // player 0 — и TABLE и HAND; player 1 — только TABLE
        // =====================================================================

        for (int i = 0; i < 2; i++) {
            tc = putBits(out, tc, r.corporationMask[i], 25);
            if (i == 0) {
                hc = putBits(out, hc, r.corporationMask[i], 25);
            }
        }

        // TABLE-only — arclightResources[2]
        for (int i = 0; i < 2; i++) {
            out[tc++] = (r.arclightResources[i] & 0xFF) / NormalizerConstants.arclightResources;
        }

        // =====================================================================
        // TABLE_AND_HAND — playedBlueCards[2] — [64+19] бит на игрока
        // player 0 — TABLE и HAND; player 1 — только TABLE
        // =====================================================================

        for (int player = 0; player < 2; player++) {
            long lo = r.playedBlueCards[player][0];
            long hi = r.playedBlueCards[player][1];
            tc = putBits(out, tc, lo, 64);
            tc = putBits(out, tc, hi, 19);
            if (player == 0) {
                hc = putBits(out, hc, lo, 64);
                hc = putBits(out, hc, hi, 19);
            }
        }

        // TABLE_AND_HAND — playedGreenCards[2] — 17 бит
        // player 0 — TABLE и HAND; player 1 — только TABLE
        for (int i = 0; i < 2; i++) {
            tc = putBits(out, tc, r.playedGreenCards[i], 17);
            if (i == 0) {
                hc = putBits(out, hc, r.playedGreenCards[i], 17);
            }
        }

        // TABLE-only — playedRedCardsMask — 49 бит
        tc = putBits(out, tc, r.playedRedCardsMask, 49);

        // =====================================================================
        // TABLE_AND_HAND — max incomes
        // =====================================================================

        for (int i = 0; i < 2; i++) { float v = r.maxMcIncome[i]    / NormalizerConstants.maxMcIncome;    out[tc++] = v; out[hc++] = v; }
        for (int i = 0; i < 2; i++) { float v = r.maxHeatIncome[i]  / NormalizerConstants.maxHeatIncome;  out[tc++] = v; out[hc++] = v; }
        for (int i = 0; i < 2; i++) { float v = (r.maxPlantsIncome[i] & 0xFF) / NormalizerConstants.maxPlantsIncome; out[tc++] = v; out[hc++] = v; }
        for (int i = 0; i < 2; i++) { float v = r.maxCardsIncome[i] / NormalizerConstants.maxCardsIncome; out[tc++] = v; out[hc++] = v; }

        // TABLE-only — researchGrantDynamicTags[2]
        for (int i = 0; i < 2; i++) {
            out[tc++] = (r.researchGrantDynamicTags[i] & 0xFF) / NormalizerConstants.researchGrantDynamicTags;
        }

        // =====================================================================
        // TABLE_AND_HAND — animalResources[2][8]
        // =====================================================================

        for (int player = 0; player < 2; player++) {
            for (int slot = 0; slot < 8; slot++) {
                float v = (r.animalResources[player][slot] & 0xFF) / NormalizerConstants.animalResources;
                out[tc++] = v;
                out[hc++] = v;
            }
        }

        // =====================================================================
        // TABLE_AND_HAND — microbeResources[2][9]
        // =====================================================================

        for (int player = 0; player < 2; player++) {
            for (int slot = 0; slot < 9; slot++) {
                float v = (r.microbeResources[player][slot] & 0xFF) / NormalizerConstants.microbeResources;
                out[tc++] = v;
                out[hc++] = v;
            }
        }

        // =====================================================================
        // handCardMask — только HAND (268 бит: 4*64 + 12)
        // =====================================================================

        for (int word = 0; word < 4; word++) hc = putBits(out, hc, r.handCardMask[word], 64);
        hc = putBits(out, hc, r.handCardMask[4], 12);

        // =====================================================================
        // selectableCardMask — в отдельный блок (268 бит)
        // =====================================================================

        for (int word = 0; word < 4; word++) sc = putBits(out, sc, r.selectableCardMask[word], 64);
        sc = putBits(out, sc, r.selectableCardMask[4], 12);

        // =====================================================================
        // Assertions (можно убрать в prod)
        // =====================================================================

        assert tc == TABLE_SIZE         : "TABLE cursor mismatch: " + tc;
        assert hc == TABLE_SIZE + HAND_SIZE : "HAND cursor mismatch: " + hc;
        assert sc == TOTAL_SIZE         : "SEL cursor mismatch: " + sc;

        return out;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Записывает `bits` бит из значения `val` в out[], начиная с позиции `pos`.
     *  Возвращает новую позицию. */
    private static int putBits(float[] out, int pos, long val, int bits) {
        for (int i = 0; i < bits; i++) {
            out[pos++] = (val >>> i) & 1L;
        }
        return pos;
    }

    private static int putBits(float[] out, int pos, int val, int bits) {
        return putBits(out, pos, (long) val & 0xFFFFFFFFL, bits);
    }

    private static int putBits(float[] out, int pos, short val, int bits) {
        return putBits(out, pos, (long) val & 0xFFFFL, bits);
    }

    private static int putBits(float[] out, int pos, byte val, int bits) {
        return putBits(out, pos, (long) val & 0xFFL, bits);
    }
}
