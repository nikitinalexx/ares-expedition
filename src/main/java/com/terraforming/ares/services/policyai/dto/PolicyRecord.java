package com.terraforming.ares.services.policyai.dto;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.awards.AwardType;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.policyai.annotations.BitMask;
import com.terraforming.ares.services.policyai.annotations.DataField;
import com.terraforming.ares.services.policyai.annotations.Feature;
import lombok.Data;

import static com.terraforming.ares.services.ai.AiConstants.BLUE_CARDS_INDEX_BY_CLASS;
import static com.terraforming.ares.services.policyai.annotations.Feature.FeatureScope.TABLE;
import static com.terraforming.ares.services.policyai.annotations.Feature.FeatureScope.TABLE_AND_HAND;
import static com.terraforming.ares.services.policyai.encoder.PolicyTableTagIncomeFeature.ENGINE_ACTIONS;

/**
 * Снимок состояния игры в момент принятия решения.
 *
 * <h3>Аннотации полей:</h3>
 * <ul>
 *   <li>{@code @BitMask(validBits=N)} — поле является битовой маской из N допустимых бит.
 *       Для массивов каждый элемент является маской.</li>
 *   <li>{@code @DataField} — числовое значение (не маска), нужен max для нормализации.</li>
 *   <li>{@code @Feature(scope=TABLE)} — попадает только в вектор стола.</li>
 *   <li>{@code @Feature(scope=TABLE_AND_HAND)} — попадает и в вектор стола, и в вектор руки.</li>
 * </ul>
 *
 * <p>Поля без {@code @Feature} / без {@code @DataField} / без {@code @BitMask}
 * являются служебными (контекст действия, целевые переменные) и не участвуют
 * в автоматическом анализе и трансформации.
 */
@Data
public class PolicyRecord {

    // =========================================================================
    // Global params — состояние планеты
    // =========================================================================

    @Feature(scope = TABLE) @DataField
    public byte oxygenLeft;

    @Feature(scope = TABLE) @DataField
    public byte temperatureLeft;

    /** Битовая маска 9 океанов. */
    @Feature(scope = TABLE) @BitMask(validBits = 9)
    public short openOceans;

    @Feature(scope = TABLE) @DataField
    public byte lastOceanCards;

    @Feature(scope = TABLE) @DataField
    public byte lastOceanPlants;

    @Feature(scope = TABLE) @DataField
    public byte lastOceanMc;

    @Feature(scope = TABLE) @DataField
    public boolean stillCanDoOxygen;

    @Feature(scope = TABLE) @DataField
    public boolean stillCanDoTemperature;

    // =========================================================================
    // Phase masks
    // =========================================================================

    /** 5 бит состояния dummy. */
    @Feature(scope = TABLE) @BitMask(validBits = 5)
    public byte dummyStateMask;

    /** 5 бит на игрока — фазы предыдущего хода. */
    @Feature(scope = TABLE) @BitMask(validBits = 5)
    public byte[] previousPhaseMask = new byte[2];

    /** 5 бит на игрока — выбранные фазы. */
    @Feature(scope = TABLE) @BitMask(validBits = 5)
    public byte[] chosenPhaseMask = new byte[2];

    @Feature(scope = TABLE) @DataField
    public boolean isFirstPlayer;

    // =========================================================================
    // Milestones — 10 штук, НЕ битовые маски
    // =========================================================================

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] milestoneInGame = new byte[AiConstants.MILESTONE_TYPES.size()];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public float[] milestoneMyProgress = new float[AiConstants.MILESTONE_TYPES.size()];

    @Feature(scope = TABLE) @DataField
    public float[] milestoneOpponentProgress = new float[AiConstants.MILESTONE_TYPES.size()];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] austellarMilestoneOwner = new byte[AiConstants.MILESTONE_TYPES.size()]; // 1, 0, -1

    // =========================================================================
    // Awards — 6 штук, НЕ битовые маски
    // =========================================================================

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] awardInGame = new byte[AiConstants.AWARD_TYPES.size()];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public float[] awardMyProgress = new float[AiConstants.AWARD_TYPES.size()];

    @Feature(scope = TABLE) @DataField
    public float[] awardOpponentProgress = new float[AiConstants.AWARD_TYPES.size()];

    // =========================================================================
    // Blue actions
    // =========================================================================

    /** Маска синих действий, биты 0–36 (37 карт). */
    @Feature(scope = TABLE) @BitMask(validBits = 37)
    public long blueActionFirstMask;

    /** Маска синих действий, биты 0–36 (37 карт). */
    @Feature(scope = TABLE) @BitMask(validBits = 37)
    public long blueActionSecondMask;

    /** Маска синих одиночных действий, биты 0–11 (12 карт). */
    @Feature(scope = TABLE) @BitMask(validBits = 12)
    public int blueActionSingleMask;

    @Feature(scope = TABLE) @DataField
    public byte blueExtraActivationsLeft;

    @Feature(scope = TABLE) @DataField
    public boolean builtSpecialDesignLastTurn;

    @Feature(scope = TABLE) @DataField
    public boolean unmiTurnAvailable;

    // =========================================================================
    // Action context — служебные флаги текущего действия.
    // Не участвуют в анализе нормализации, но идут в вектор стола.
    // =========================================================================

    @Feature(scope = TABLE) @DataField
    public boolean initialSetup;

    @Feature(scope = TABLE) @DataField
    public boolean doingMulligan;

    @Feature(scope = TABLE) @DataField
    public boolean pickingCorporation;

    @Feature(scope = TABLE) @DataField
    public boolean payingForTheBuild;

    @Feature(scope = TABLE) @DataField
    public byte mulliganToDraw;

    /** 5-битная маска текущей фазы. */
    @Feature(scope = TABLE) @BitMask(validBits = 5)
    public int currentPhaseMask;

    @Feature(scope = TABLE) @DataField
    public boolean choosingPhase1Upgrade;

    @Feature(scope = TABLE) @DataField
    public boolean choosingPhase2Upgrade;

    @Feature(scope = TABLE) @DataField
    public boolean choosingPhase3Upgrade;

    @Feature(scope = TABLE) @DataField
    public boolean choosingPhase4Upgrade;

    @Feature(scope = TABLE) @DataField
    public boolean choosingPhase5Upgrade;

    @Feature(scope = TABLE) @DataField
    public boolean choosingTag;

    @Feature(scope = TABLE) @DataField
    public byte universalPhaseUpgradeCount;

    @Feature(scope = TABLE) @DataField
    public boolean choosingPhase;

    @Feature(scope = TABLE) @DataField
    public boolean sellingCards;

    @Feature(scope = TABLE) @DataField
    public boolean helionExchanging;

    @Feature(scope = TABLE) @DataField
    public byte cardsLeftToDiscard;

    @Feature(scope = TABLE) @DataField
    public byte marsUniversityDiscardsLeft;

    @Feature(scope = TABLE) @DataField
    public byte decomposersActivationsLeft;

    @Feature(scope = TABLE) @DataField
    public byte viralEnhancersActivationsLeft;

    @Feature(scope = TABLE) @DataField
    public byte animalPutCount;

    @Feature(scope = TABLE) @DataField
    public byte microbePutCount;

    @Feature(scope = TABLE) @DataField
    public boolean postBuildDiscarding;

    @Feature(scope = TABLE) @DataField
    public boolean discardingFromSelected;

    @Feature(scope = TABLE) @DataField
    public boolean discardingLastTurn;

    @Feature(scope = TABLE) @DataField
    public boolean ceosFavoriteProjectEffect;

    @Feature(scope = TABLE) @DataField
    public boolean syntheticCatastropheEffect;

    @Feature(scope = TABLE) @DataField
    public boolean importedHydrogenEffect;

    @Feature(scope = TABLE) @DataField
    public boolean largeConvoyEffect;

    @Feature(scope = TABLE) @DataField
    public boolean localHeatTrappingEffect;

    @Feature(scope = TABLE) @DataField
    public boolean astrofarmEffect;

    @Feature(scope = TABLE) @DataField
    public boolean eosChasmaEffect;

    @Feature(scope = TABLE) @DataField
    public boolean cryogenicShipmentEffect;

    @Feature(scope = TABLE) @DataField
    public boolean biomedicalImportsEffect;

    @Feature(scope = TABLE) @DataField
    public boolean austellarChoosing;

    @Feature(scope = TABLE) @DataField
    public boolean conservedBiomeAction;

    @Feature(scope = TABLE) @DataField
    public boolean decomposingFungus;

    @Feature(scope = TABLE) @DataField
    public boolean extremeColdFungus;

    @Feature(scope = TABLE) @DataField
    public boolean farmingCoops;

    @Feature(scope = TABLE) @DataField
    public boolean ghgProduction;

    @Feature(scope = TABLE) @DataField
    public boolean regolithEaters;

    @Feature(scope = TABLE) @DataField
    public boolean nitriteReductingBacteria;

    @Feature(scope = TABLE) @DataField
    public boolean selfReplicatingBacteria;

    @Feature(scope = TABLE) @DataField
    public boolean greenHouses;

    @Feature(scope = TABLE) @DataField
    public boolean matterGenerator;

    @Feature(scope = TABLE) @DataField
    public boolean powerInfrastructure;

    @Feature(scope = TABLE) @DataField
    public boolean redraftedContracts;

    @Feature(scope = TABLE) @DataField
    public boolean fibrousComposite;

    @Feature(scope = TABLE) @DataField
    public byte redraftedDiscarded;

    // =========================================================================
    // Builds
    // =========================================================================

    /** 11 бит доступных построек. */
    @Feature(scope = TABLE) @BitMask(validBits = 13)
    public short ownedBuildsMask;

    // =========================================================================
    // Phase upgrades — 5 бит на игрока
    // =========================================================================

    /** 2 бита на каждую из 5 фаз => 10 бит на игрока. */
    @Feature(scope = TABLE_AND_HAND) @BitMask(validBits = 10)
    public int[] phaseUpgrades = new int[2];

    // =========================================================================
    // Victory / board
    // =========================================================================

    @Feature(scope = TABLE) @DataField
    public short[] winPointsNoForests = new short[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public short[] forests = new short[2];

    // =========================================================================
    // Incomes
    // =========================================================================

    @Feature(scope = TABLE_AND_HAND) @DataField
    public short[] mcIncomeTotal = new short[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] steelIncome = new byte[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] titaniumIncome = new byte[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] plantsIncome = new byte[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] heatIncome = new byte[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] cardIncome = new byte[2];

    // =========================================================================
    // Resources
    // =========================================================================

    @Feature(scope = TABLE_AND_HAND) @DataField
    public float[] mc = new float[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public short[] plants = new short[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public short[] heat = new short[2];

    // =========================================================================
    // Cards counts
    // =========================================================================

    @Feature(scope = TABLE_AND_HAND) @DataField
    public short[] playedCards = new short[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public short[] blueCards = new short[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public short[] greenCards = new short[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] handSize = new byte[2];

    // =========================================================================
    // Tags
    // =========================================================================

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] spaceTags = new byte[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] earthTags = new byte[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] eventTags = new byte[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] scienceTags = new byte[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] plantTags = new byte[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] energyTags = new byte[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] buildingTags = new byte[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] animalTags = new byte[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] jupiterTags = new byte[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] microbeTags = new byte[2];

    // =========================================================================
    // Engine actions — 2 * ENGINE_ACTIONS, НЕ маски
    // =========================================================================

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] engineActionCount = new byte[2 * ENGINE_ACTIONS];

    // =========================================================================
    // Corporation masks — 24 бита на игрока
    // =========================================================================

    @Feature(scope = TABLE_AND_HAND) @BitMask(validBits = 25)
    public int[] corporationMask = new int[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] arclightResources = new byte[2];

    // =========================================================================
    // Played cards — битовые маски карт
    // =========================================================================

    /** 82 флага синих карт, packed в 2 longs на игрока: [64, 18, 64, 18] бит. */
    @Feature(scope = TABLE_AND_HAND) @BitMask(bitsPerElement = {64, 18, 64, 18})
    public long[][] playedBlueCards = new long[2][2];

    /** 17 бит зелёных карт. */
    @Feature(scope = TABLE_AND_HAND) @BitMask(validBits = 17)
    public int[] playedGreenCards = new int[2];

    /** 49 бит красных карт. */
    @Feature(scope = TABLE) @BitMask(validBits = 49)
    public long playedRedCardsMask;

    // =========================================================================
    // Max incomes — лучшая карта на столе
    // =========================================================================

    @Feature(scope = TABLE_AND_HAND) @DataField
    public short[] maxMcIncome = new short[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public short[] maxHeatIncome = new short[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[] maxPlantsIncome = new byte[2];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public float[] maxCardsIncome = new float[2];

    // =========================================================================
    // Research grant
    // =========================================================================

    @Feature(scope = TABLE) @DataField
    public byte[] researchGrantDynamicTags = new byte[2];

    // =========================================================================
    // Animals / Microbes
    // =========================================================================

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[][] animalResources = new byte[2][8];

    @Feature(scope = TABLE_AND_HAND) @DataField
    public byte[][] microbeResources = new byte[2][9];

    // =========================================================================
    // Hand & selectable card masks — только рука (TABLE_AND_HAND, но это HAND-only данные)
    // =========================================================================

    /** 268 бит карт в руке: 4 * 64 + 12 = 268. */
    @Feature(scope = TABLE_AND_HAND) @BitMask(validBits = 64, lastElementBits = 12)
    public long[] handCardMask = new long[5];

    /** 268 бит выбираемых карт: 4 * 64 + 12 = 268. */
    @Feature(scope = TABLE_AND_HAND) @BitMask(validBits = 64, lastElementBits = 12)
    public long[] selectableCardMask = new long[5];

    // =========================================================================
    // Train targets — служебные поля, не участвуют в анализе и трансформации
    // =========================================================================

    public int chosenAction = -1;
    public float outcome;

    // =========================================================================
    // Methods (без изменений)
    // =========================================================================

    public void copyFrom(PolicyRecord s) {
        oxygenLeft = s.oxygenLeft;
        temperatureLeft = s.temperatureLeft;
        openOceans = s.openOceans;
        lastOceanCards = s.lastOceanCards;
        lastOceanPlants = s.lastOceanPlants;
        lastOceanMc = s.lastOceanMc;
        stillCanDoOxygen = s.stillCanDoOxygen;
        stillCanDoTemperature = s.stillCanDoTemperature;
        dummyStateMask = s.dummyStateMask;

        System.arraycopy(s.previousPhaseMask, 0, previousPhaseMask, 0, 2);
        System.arraycopy(s.chosenPhaseMask, 0, chosenPhaseMask, 0, 2);

        isFirstPlayer = s.isFirstPlayer;

        System.arraycopy(s.milestoneInGame, 0, milestoneInGame, 0, milestoneInGame.length);
        System.arraycopy(s.milestoneMyProgress, 0, milestoneMyProgress, 0, milestoneMyProgress.length);
        System.arraycopy(s.milestoneOpponentProgress, 0, milestoneOpponentProgress, 0, milestoneOpponentProgress.length);
        System.arraycopy(s.austellarMilestoneOwner, 0, austellarMilestoneOwner, 0, austellarMilestoneOwner.length);

        System.arraycopy(s.awardInGame, 0, awardInGame, 0, awardInGame.length);
        System.arraycopy(s.awardMyProgress, 0, awardMyProgress, 0, awardMyProgress.length);
        System.arraycopy(s.awardOpponentProgress, 0, awardOpponentProgress, 0, awardOpponentProgress.length);

        blueActionFirstMask = s.blueActionFirstMask;
        blueActionSecondMask = s.blueActionSecondMask;
        blueActionSingleMask = s.blueActionSingleMask;
        blueExtraActivationsLeft = s.blueExtraActivationsLeft;
        builtSpecialDesignLastTurn = s.builtSpecialDesignLastTurn;
        unmiTurnAvailable = s.unmiTurnAvailable;

        initialSetup = s.initialSetup;
        doingMulligan = s.doingMulligan;
        pickingCorporation = s.pickingCorporation;
        payingForTheBuild = s.payingForTheBuild;
        mulliganToDraw = s.mulliganToDraw;
        currentPhaseMask = s.currentPhaseMask;

        choosingPhase1Upgrade = s.choosingPhase1Upgrade;
        choosingPhase2Upgrade = s.choosingPhase2Upgrade;
        choosingPhase3Upgrade = s.choosingPhase3Upgrade;
        choosingPhase4Upgrade = s.choosingPhase4Upgrade;
        choosingPhase5Upgrade = s.choosingPhase5Upgrade;

        universalPhaseUpgradeCount = s.universalPhaseUpgradeCount;
        choosingTag = s.choosingTag;
        cardsLeftToDiscard = s.cardsLeftToDiscard;
        postBuildDiscarding = s.postBuildDiscarding;
        discardingFromSelected = s.discardingFromSelected;
        marsUniversityDiscardsLeft = s.marsUniversityDiscardsLeft;
        decomposersActivationsLeft = s.decomposersActivationsLeft;
        viralEnhancersActivationsLeft = s.viralEnhancersActivationsLeft;
        ceosFavoriteProjectEffect = s.ceosFavoriteProjectEffect;
        syntheticCatastropheEffect = s.syntheticCatastropheEffect;
        importedHydrogenEffect = s.importedHydrogenEffect;
        largeConvoyEffect = s.largeConvoyEffect;
        localHeatTrappingEffect = s.localHeatTrappingEffect;
        astrofarmEffect = s.astrofarmEffect;
        eosChasmaEffect = s.eosChasmaEffect;
        cryogenicShipmentEffect = s.cryogenicShipmentEffect;
        biomedicalImportsEffect = s.biomedicalImportsEffect;
        animalPutCount = s.animalPutCount;
        microbePutCount = s.microbePutCount;
        discardingLastTurn = s.discardingLastTurn;

        austellarChoosing = s.austellarChoosing;
        choosingPhase = s.choosingPhase;
        sellingCards = s.sellingCards;
        helionExchanging = s.helionExchanging;

        conservedBiomeAction = s.conservedBiomeAction;
        decomposingFungus = s.decomposingFungus;
        extremeColdFungus = s.extremeColdFungus;
        farmingCoops = s.farmingCoops;
        ghgProduction = s.ghgProduction;
        regolithEaters = s.regolithEaters;
        nitriteReductingBacteria = s.nitriteReductingBacteria;
        selfReplicatingBacteria = s.selfReplicatingBacteria;
        greenHouses = s.greenHouses;
        matterGenerator = s.matterGenerator;
        powerInfrastructure = s.powerInfrastructure;
        redraftedContracts = s.redraftedContracts;
        redraftedDiscarded = s.redraftedDiscarded;
        fibrousComposite = s.fibrousComposite;

        ownedBuildsMask = s.ownedBuildsMask;

        System.arraycopy(s.phaseUpgrades, 0, phaseUpgrades, 0, 2);
        System.arraycopy(s.maxMcIncome, 0, maxMcIncome, 0, 2);
        System.arraycopy(s.maxHeatIncome, 0, maxHeatIncome, 0, 2);
        System.arraycopy(s.maxPlantsIncome, 0, maxPlantsIncome, 0, 2);
        System.arraycopy(s.maxCardsIncome, 0, maxCardsIncome, 0, 2);
        System.arraycopy(s.winPointsNoForests, 0, winPointsNoForests, 0, 2);
        System.arraycopy(s.forests, 0, forests, 0, 2);
        System.arraycopy(s.mcIncomeTotal, 0, mcIncomeTotal, 0, 2);
        System.arraycopy(s.steelIncome, 0, steelIncome, 0, 2);
        System.arraycopy(s.titaniumIncome, 0, titaniumIncome, 0, 2);
        System.arraycopy(s.plantsIncome, 0, plantsIncome, 0, 2);
        System.arraycopy(s.heatIncome, 0, heatIncome, 0, 2);
        System.arraycopy(s.cardIncome, 0, cardIncome, 0, 2);
        System.arraycopy(s.mc, 0, mc, 0, 2);
        System.arraycopy(s.plants, 0, plants, 0, 2);
        System.arraycopy(s.heat, 0, heat, 0, 2);
        System.arraycopy(s.playedCards, 0, playedCards, 0, 2);
        System.arraycopy(s.blueCards, 0, blueCards, 0, 2);
        System.arraycopy(s.greenCards, 0, greenCards, 0, 2);
        System.arraycopy(s.handSize, 0, handSize, 0, 2);
        System.arraycopy(s.spaceTags, 0, spaceTags, 0, 2);
        System.arraycopy(s.earthTags, 0, earthTags, 0, 2);
        System.arraycopy(s.eventTags, 0, eventTags, 0, 2);
        System.arraycopy(s.scienceTags, 0, scienceTags, 0, 2);
        System.arraycopy(s.plantTags, 0, plantTags, 0, 2);
        System.arraycopy(s.energyTags, 0, energyTags, 0, 2);
        System.arraycopy(s.buildingTags, 0, buildingTags, 0, 2);
        System.arraycopy(s.animalTags, 0, animalTags, 0, 2);
        System.arraycopy(s.jupiterTags, 0, jupiterTags, 0, 2);
        System.arraycopy(s.microbeTags, 0, microbeTags, 0, 2);
        System.arraycopy(s.engineActionCount, 0, engineActionCount, 0, engineActionCount.length);
        System.arraycopy(s.corporationMask, 0, corporationMask, 0, 2);
        System.arraycopy(s.arclightResources, 0, arclightResources, 0, 2);
        System.arraycopy(s.playedBlueCards[0], 0, playedBlueCards[0], 0, 2);
        System.arraycopy(s.playedBlueCards[1], 0, playedBlueCards[1], 0, 2);
        System.arraycopy(s.researchGrantDynamicTags, 0, researchGrantDynamicTags, 0, 2);

        for (int i = 0; i < 2; i++) {
            System.arraycopy(s.animalResources[i], 0, animalResources[i], 0, 8);
            System.arraycopy(s.microbeResources[i], 0, microbeResources[i], 0, 9);
        }

        System.arraycopy(s.handCardMask, 0, handCardMask, 0, 5);
        System.arraycopy(s.selectableCardMask, 0, selectableCardMask, 0, 5);

        System.arraycopy(s.playedGreenCards, 0, playedGreenCards, 0, 2);

        playedRedCardsMask = s.playedRedCardsMask;
        chosenAction = s.chosenAction;
        outcome = s.outcome;
    }

    public void reset() {
        outcome = 0f;
        chosenAction = -1;
    }

    public void removeCardFromHand(Card card) {
        if (card == null) return;
        handSize[0]--;
        int index = AiConstants.ALL_CARDS_INDEX_BY_CLASS.get(card.getClass());
        int wordIndex = index >>> 6;
        long bitMask = 1L << (index & 63);
        handCardMask[wordIndex] &= ~bitMask;
    }

    public void addGenericCardToHand() {
        handSize[0]++;
    }

    public boolean hasPlayedBlueCard(Class<?> cardClass) {
        Integer index = BLUE_CARDS_INDEX_BY_CLASS.get(cardClass);
        if (index == null) return false;
        long mask;
        if (index < 64) {
            mask = playedBlueCards[0][0];
            return (mask & (1L << index)) != 0;
        } else {
            mask = playedBlueCards[0][1];
            return (mask & (1L << (index - 64))) != 0;
        }
    }

    public void addAwardProgress(AwardType type, int delta) {
        int index = AiConstants.AWARD_TYPES.indexOf(type);
        if (index < 0 || awardInGame[index] == 0) return;
        float stored = awardMyProgress[index];
        double rawValue = Math.expm1(stored);
        rawValue += delta;
        if (rawValue < 0) rawValue = 0;
        awardMyProgress[index] = (float) Math.log1p(rawValue);
    }

    public void selectCard(Card card) {
        if (card == null) return;
        int index = AiConstants.ALL_CARDS_INDEX_BY_CLASS.get(card.getClass());
        int wordIndex = index >>> 6;
        long bitMask = 1L << (index & 63);
        selectableCardMask[wordIndex] |= bitMask;
    }

    public void removeCardFromSelected(Card card) {
        if (card == null) return;
        int index = AiConstants.ALL_CARDS_INDEX_BY_CLASS.get(card.getClass());
        int wordIndex = index >>> 6;
        long bitMask = 1L << (index & 63);
        selectableCardMask[wordIndex] &= ~bitMask;
    }

    public void setPhaseUpgrade(int upgradeId) {
        int mask = phaseUpgrades[0];
        int phase = upgradeId / 2;
        int upgradeType = (upgradeId % 2) + 1;
        int shift = phase * 2;
        mask &= ~(0b11 << shift);
        mask |= (upgradeType << shift);
        phaseUpgrades[0] = mask;
    }
}