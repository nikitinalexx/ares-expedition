package com.terraforming.ares.services.policyai.action;

/**
 * Голова CORPORATION — выбор корпорации.
 * Локальный индекс = индекс корпорации из AiConstants.CORP_INDEX.
 */
public final class CorporationAction {

    public static final int COUNT = ActionRegistry.CHOICE_CORPORATION_COUNT;

    private static final HeadAction[] CACHE = new HeadAction[COUNT];

    static {
        for (int i = 0; i < COUNT; i++) {
            final int localIndex = i;
            CACHE[i] = new HeadAction() {
                @Override public ActionHead head()  { return ActionHead.CORPORATION; }
                @Override public int localIndex()   { return localIndex; }
                @Override public String toString()  { return "CorporationAction[" + localIndex + "]"; }
            };
        }
    }

    public static HeadAction forCorpIndex(int corpIndex) {
        if (corpIndex < 0 || corpIndex >= COUNT)
            throw new IllegalArgumentException("CorporationAction: corpIndex " + corpIndex + " out of [0," + COUNT + ")");
        return CACHE[corpIndex];
    }

    private CorporationAction() {}
}
