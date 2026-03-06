package com.terraforming.ares.services.policyai.action;

import com.terraforming.ares.model.StandardProjectType;

/**
 * Голова SMALL — мелкие выборы.
 *
 * <p>Структура:
 * <pre>
 *   [0..10]  Phase upgrade / Austellar milestone (11) / Phase turn
 *   [11..20] Tag choice (10)
 *   [21]     Biomedical imports — oxygen
 *   [22]     Biomedical imports — phase upgrade
 *   [23]     Decomposers — pick microbe
 *   [24]     Decomposers — pick card
 *   [25]     Action: take microbe
 *   [26]     Action: use microbe
 *   [27..30] Count 1-4 (4)
 *   [31..34] Pay for the build (4)
 *   [35..37] Standard projects (3)
 *   [38..39] Heat/Plants conversion (2)
 * </pre>
 */
public final class SmallAction {

    public static final int OFFSET_PHASE_UPGRADE          = 0;
    public static final int OFFSET_TAG                    = OFFSET_PHASE_UPGRADE + ActionRegistry.SMALL_CHOICE_PHASE_OR_MILESTONE_CHOICE;
    public static final int OFFSET_BIOMEDICAL_OXYGEN       = OFFSET_TAG           + ActionRegistry.SMALL_CHOICE_TAG_COUNT;
    public static final int OFFSET_BIOMEDICAL_PHASE        = OFFSET_BIOMEDICAL_OXYGEN + 1;
    public static final int OFFSET_DECOMPOSERS_MICROBE     = OFFSET_BIOMEDICAL_PHASE + 1;
    public static final int OFFSET_DECOMPOSERS_CARD        = OFFSET_DECOMPOSERS_MICROBE + 1;
    public static final int OFFSET_TAKE_MICROBE            = OFFSET_DECOMPOSERS_CARD + 1;
    public static final int OFFSET_USE_MICROBE             = OFFSET_TAKE_MICROBE + 1;
    public static final int OFFSET_COUNT_1_4               = OFFSET_USE_MICROBE + 1;
    public static final int OFFSET_PAY_FOR_BUILD           = OFFSET_COUNT_1_4   + ActionRegistry.SMALL_ACTION_COUNT_1_4_COUNT;
    public static final int OFFSET_STANDARD_PROJECT        = OFFSET_PAY_FOR_BUILD + ActionRegistry.SMALL_PAY_FOR_THE_BUILD_COUNT;
    public static final int OFFSET_HEAT_PLANTS_CONVERSION  = OFFSET_STANDARD_PROJECT + ActionRegistry.SMALL_ACTION_STANDARD_PROJECT_COUNT;

    public static final int COUNT =
            ActionRegistry.SMALL_CHOICE_PHASE_OR_MILESTONE_CHOICE +
            ActionRegistry.SMALL_CHOICE_TAG_COUNT +
            2 + // biomedical imports
            2 + // decomposers
            2 + // microbe actions
            ActionRegistry.SMALL_ACTION_COUNT_1_4_COUNT +
            ActionRegistry.SMALL_PAY_FOR_THE_BUILD_COUNT +
            ActionRegistry.SMALL_ACTION_STANDARD_PROJECT_COUNT +
            ActionRegistry.SMALL_ACTION_HEAT_PLANTS_CONVERSION_COUNT;

    private static final HeadAction[] CACHE = new HeadAction[COUNT];

    static {
        for (int i = 0; i < COUNT; i++) {
            final int localIndex = i;
            CACHE[i] = new HeadAction() {
                @Override public ActionHead head()  { return ActionHead.SMALL; }
                @Override public int localIndex()   { return localIndex; }
                @Override public String toString()  { return "SmallAction[" + localIndex + "]"; }
            };
        }
    }

    // ---- Именованные константы ----

    public static final HeadAction BIOMEDICAL_IMPORTS_OXYGEN  = CACHE[OFFSET_BIOMEDICAL_OXYGEN];
    public static final HeadAction BIOMEDICAL_IMPORTS_PHASE   = CACHE[OFFSET_BIOMEDICAL_PHASE];
    public static final HeadAction DECOMPOSERS_PICK_MICROBE   = CACHE[OFFSET_DECOMPOSERS_MICROBE];
    public static final HeadAction DECOMPOSERS_PICK_CARD      = CACHE[OFFSET_DECOMPOSERS_CARD];
    public static final HeadAction ACTION_TAKE_MICROBE        = CACHE[OFFSET_TAKE_MICROBE];
    public static final HeadAction ACTION_USE_MICROBE         = CACHE[OFFSET_USE_MICROBE];
    public static final HeadAction CONVERT_HEAT_TO_TEMPERATURE = CACHE[OFFSET_HEAT_PLANTS_CONVERSION];
    public static final HeadAction CONVERT_PLANTS_TO_FOREST   = CACHE[OFFSET_HEAT_PLANTS_CONVERSION + 1];

    // Standard projects — именованные т.к. StandardProjectType.ordinal() используем
    public static final HeadAction STANDARD_PROJECT_POWER_PLANT = CACHE[OFFSET_STANDARD_PROJECT + 0];
    public static final HeadAction STANDARD_PROJECT_ASTEROID    = CACHE[OFFSET_STANDARD_PROJECT + 1];
    public static final HeadAction STANDARD_PROJECT_AQUIFER     = CACHE[OFFSET_STANDARD_PROJECT + 2];

    // ---- Фабрики ----

    /** upgradeId 0-9 (или 0-10 для Austellar milestone — reuses same space). */
    public static HeadAction forPhaseUpgrade(int upgradeId) {
        if (upgradeId < 0 || upgradeId >= ActionRegistry.SMALL_CHOICE_PHASE_OR_MILESTONE_CHOICE)
            throw new IllegalArgumentException("SmallAction.phaseUpgrade: upgradeId " + upgradeId + " out of range");
        return CACHE[OFFSET_PHASE_UPGRADE + upgradeId];
    }

    public static HeadAction forAustellarMilestone(int milestoneIndex) {
        if (milestoneIndex < 0 || milestoneIndex >= ActionRegistry.SMALL_CHOICE_PHASE_OR_MILESTONE_CHOICE)
            throw new IllegalArgumentException("SmallAction.austellarMilestone: index " + milestoneIndex + " out of range");
        return CACHE[OFFSET_PHASE_UPGRADE + milestoneIndex];
    }

    public static HeadAction forTag(int index) {
        if (index < 0 || index >= ActionRegistry.SMALL_CHOICE_TAG_COUNT)
            throw new IllegalArgumentException("SmallAction.tag: index " + index + " out of [0," + ActionRegistry.SMALL_CHOICE_TAG_COUNT + ")");
        return CACHE[OFFSET_TAG + index];
    }

    public static HeadAction forCount(int count) {
        if (count < 0 || count >= ActionRegistry.SMALL_ACTION_COUNT_1_4_COUNT)
            throw new IllegalArgumentException("SmallAction.count: " + count + " out of [0," + ActionRegistry.SMALL_ACTION_COUNT_1_4_COUNT + ")");
        return CACHE[OFFSET_COUNT_1_4 + count];
    }

    public static HeadAction forPayForBuild(int type) {
        if (type < 0 || type >= ActionRegistry.SMALL_PAY_FOR_THE_BUILD_COUNT)
            throw new IllegalArgumentException("SmallAction.payForBuild: type " + type + " out of [0," + ActionRegistry.SMALL_PAY_FOR_THE_BUILD_COUNT + ")");
        return CACHE[OFFSET_PAY_FOR_BUILD + type];
    }

    public static HeadAction forStandardProject(StandardProjectType type) {
        if (type == StandardProjectType.INFRASTRUCTURE)
            throw new IllegalArgumentException("SmallAction: INFRASTRUCTURE not available for policy");
        return CACHE[OFFSET_STANDARD_PROJECT + type.ordinal()];
    }

    private SmallAction() {}
}
