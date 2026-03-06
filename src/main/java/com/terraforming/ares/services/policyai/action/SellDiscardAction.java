package com.terraforming.ares.services.policyai.action;

/**
 * Голова SELL_DISCARD — продать или сбросить карту из руки.
 * Локальный индекс = индекс карты из AiConstants.ALL_CARDS_INDEX_BY_CLASS.
 */
public final class SellDiscardAction {

    public static final int COUNT = ActionRegistry.CARD_COUNT;

    private static final HeadAction[] CACHE = new HeadAction[COUNT];

    static {
        for (int i = 0; i < COUNT; i++) {
            final int localIndex = i;
            CACHE[i] = new HeadAction() {
                @Override public ActionHead head()  { return ActionHead.SELL_DISCARD; }
                @Override public int localIndex()   { return localIndex; }
                @Override public String toString()  { return "SellDiscardAction[" + localIndex + "]"; }
            };
        }
    }

    public static HeadAction forCardIndex(int cardIndex) {
        if (cardIndex < 0 || cardIndex >= COUNT)
            throw new IllegalArgumentException("SellDiscardAction: cardIndex " + cardIndex + " out of [0," + COUNT + ")");
        return CACHE[cardIndex];
    }

    private SellDiscardAction() {}
}
