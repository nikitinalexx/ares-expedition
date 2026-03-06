package com.terraforming.ares.services.policyai.action;

import com.terraforming.ares.services.ai.AiConstants;

/**
 * Голова TARGET — выбор цели действия.
 *
 * <p>Структура (смещения внутри головы):
 * <pre>
 *   [0..8]   Animals (9)
 *   [9..17]  Microbes (9)
 *   [18..19] Science (2)
 *   [20..67] Red card targets (48)
 *   [68]     Take plant (1)
 *   [69..72] Max incomes (4): MC, Heat, Plants, Cards
 *   [73..]   Spec green card income (dynamic size)
 * </pre>
 */
public final class TargetAction {

    // ---- Смещения (пакетный доступ через константы) ----
    public static final int OFFSET_ANIMAL     = 0;
    public static final int OFFSET_MICROBE    = OFFSET_ANIMAL   + ActionRegistry.TARGET_ANIMAL_COUNT;
    public static final int OFFSET_SCIENCE    = OFFSET_MICROBE  + ActionRegistry.TARGET_MICROBE_COUNT;
    public static final int OFFSET_RED_CARD   = OFFSET_SCIENCE  + ActionRegistry.TARGET_SCIENCE_COUNT;
    public static final int OFFSET_TAKE_PLANT = OFFSET_RED_CARD + ActionRegistry.RED_CARD_TARGET_COUNT;
    public static final int OFFSET_MAX_INCOME = OFFSET_TAKE_PLANT + ActionRegistry.TARGET_TAKE_PLANT_COUNT;
    public static final int OFFSET_SPEC_GREEN = OFFSET_MAX_INCOME + 4;

    public static final int COUNT =
            ActionRegistry.TARGET_ANIMAL_COUNT +
            ActionRegistry.TARGET_MICROBE_COUNT +
            ActionRegistry.TARGET_SCIENCE_COUNT +
            ActionRegistry.RED_CARD_TARGET_COUNT +
            ActionRegistry.TARGET_TAKE_PLANT_COUNT +
            4 +
            ActionRegistry.TARGET_SPEC_GREEN_CARD_INCOME_COUNT;

    private static final HeadAction[] CACHE = new HeadAction[COUNT];

    static {
        for (int i = 0; i < COUNT; i++) {
            final int localIndex = i;
            CACHE[i] = new HeadAction() {
                @Override public ActionHead head()  { return ActionHead.TARGET; }
                @Override public int localIndex()   { return localIndex; }
                @Override public String toString()  { return "TargetAction[" + localIndex + "]"; }
            };
        }
    }

    // ---- Именованные константы (ссылки на кэш, не новые объекты) ----

    // Animals
    public static final HeadAction ANIMAL_BIRDS              = CACHE[OFFSET_ANIMAL + 0];
    public static final HeadAction ANIMAL_FILTER_FEEDERS     = CACHE[OFFSET_ANIMAL + 1];
    public static final HeadAction ANIMAL_FISH               = CACHE[OFFSET_ANIMAL + 2];
    public static final HeadAction ANIMAL_LIVESTOCK          = CACHE[OFFSET_ANIMAL + 3];
    public static final HeadAction ANIMAL_ZOOS               = CACHE[OFFSET_ANIMAL + 4];
    public static final HeadAction ANIMAL_ECOLOGICAL_ZONE    = CACHE[OFFSET_ANIMAL + 5];
    public static final HeadAction ANIMAL_HERBIVORES         = CACHE[OFFSET_ANIMAL + 6];
    public static final HeadAction ANIMAL_SMALL_ANIMALS      = CACHE[OFFSET_ANIMAL + 7];
    public static final HeadAction ANIMAL_ARCLIGHT           = CACHE[OFFSET_ANIMAL + 8];

    // Microbes
    public static final HeadAction MICROBE_ANAEROBIC              = CACHE[OFFSET_MICROBE + 0];
    public static final HeadAction MICROBE_DECOMPOSERS            = CACHE[OFFSET_MICROBE + 1];
    public static final HeadAction MICROBE_DECOMPOSING_FUNGUS     = CACHE[OFFSET_MICROBE + 2];
    public static final HeadAction MICROBE_GHG_PRODUCTION         = CACHE[OFFSET_MICROBE + 3];
    public static final HeadAction MICROBE_NITRITE_REDUCTING      = CACHE[OFFSET_MICROBE + 4];
    public static final HeadAction MICROBE_REGOLITH_EATERS        = CACHE[OFFSET_MICROBE + 5];
    public static final HeadAction MICROBE_SELF_REPLICATING       = CACHE[OFFSET_MICROBE + 6];
    public static final HeadAction MICROBE_TARDIGRADES            = CACHE[OFFSET_MICROBE + 7];
    public static final HeadAction MICROBE_BACTERIAL_AGGREGATES   = CACHE[OFFSET_MICROBE + 8];

    // Science
    public static final HeadAction SCIENCE_PHYSICS_COMPLEX    = CACHE[OFFSET_SCIENCE + 0];
    public static final HeadAction SCIENCE_FIBROUS_COMPOSITE   = CACHE[OFFSET_SCIENCE + 1];

    // Take plant
    public static final HeadAction TAKE_PLANT = CACHE[OFFSET_TAKE_PLANT];

    // Max incomes
    public static final HeadAction MAX_MC_INCOME    = CACHE[OFFSET_MAX_INCOME + 0];
    public static final HeadAction MAX_HEAT_INCOME  = CACHE[OFFSET_MAX_INCOME + 1];
    public static final HeadAction MAX_PLANTS_INCOME = CACHE[OFFSET_MAX_INCOME + 2];
    public static final HeadAction MAX_CARDS_INCOME = CACHE[OFFSET_MAX_INCOME + 3];

    // ---- Фабрики для динамических целей (возвращают из кэша) ----

    public static HeadAction forAnimalOffset(int offset) {
        return CACHE[OFFSET_ANIMAL + offset];
    }

    public static HeadAction forMicrobeOffset(int offset) {
        return CACHE[OFFSET_MICROBE + offset];
    }

    public static HeadAction forRedCardIndex(int redCardOffset) {
        if (redCardOffset < 0 || redCardOffset >= ActionRegistry.RED_CARD_TARGET_COUNT)
            throw new IllegalArgumentException("TargetAction: redCardOffset " + redCardOffset + " out of range");
        return CACHE[OFFSET_RED_CARD + redCardOffset];
    }

    public static HeadAction forSpecGreenCardIndex(int cardIndex) {
        if (cardIndex < 0 || cardIndex >= ActionRegistry.TARGET_SPEC_GREEN_CARD_INCOME_COUNT)
            throw new IllegalArgumentException("TargetAction: specGreenCardIndex " + cardIndex + " out of range");
        return CACHE[OFFSET_SPEC_GREEN + cardIndex];
    }

    private TargetAction() {}
}
