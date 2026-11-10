package com.terraforming.ares.services.policyai.action;

/**
 * Голова SELL_DISCARD — продать или сбросить карту из руки.
 * Локальный индекс = индекс карты из AiConstants.ALL_CARDS_INDEX_BY_CLASS.
 */
public final class SellDiscardAction {

    //cards + pass for mars university
    public static final int COUNT = ActionRegistry.CARD_COUNT + 1;

    private static final HeadAction[] CACHE = new HeadAction[ActionRegistry.CARD_COUNT];

    public static final HeadAction MARS_UNIVERSITY_PASS = new HeadAction() {
        @Override public ActionHead head()  { return ActionHead.SELL_DISCARD; }
        @Override public int localIndex()   { return ActionRegistry.CARD_COUNT; }
        @Override public String toString()  { return "MarsUniversityPass[" + ActionRegistry.CARD_COUNT + "]"; }
    };

    static {
        for (int i = 0; i < ActionRegistry.CARD_COUNT; i++) {
            final int localIndex = i;
            CACHE[i] = new HeadAction() {
                @Override public ActionHead head()  { return ActionHead.SELL_DISCARD; }
                @Override public int localIndex()   { return localIndex; }
                @Override public String toString()  { return "SellDiscardAction[" + localIndex + "]"; }
            };
        }
    }

    public static HeadAction forCardIndex(int cardIndex) {
        if (cardIndex < 0 || cardIndex >= ActionRegistry.CARD_COUNT)
            throw new IllegalArgumentException("SellDiscardAction: cardIndex " + cardIndex + " out of [0," + ActionRegistry.CARD_COUNT + ")");
        return CACHE[cardIndex];
    }

    private SellDiscardAction() {}
}
