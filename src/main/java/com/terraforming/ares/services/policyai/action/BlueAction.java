package com.terraforming.ares.services.policyai.action;

import com.terraforming.ares.services.ai.AiConstants;

/**
 * Голова BLUE — активировать синюю карту.
 *
 * <p>Структура:
 * <pre>
 *   [0 .. NO_PAY_COUNT-1]                  — карты без оплаты
 *   [NO_PAY_COUNT .. COUNT-1]              — карты с оплатой
 * </pre>
 */
public final class BlueAction {

    public static final int NO_PAY_COUNT   = AiConstants.NO_PAYMENT_BLUE_ACTIONS.size();
    public static final int WITH_PAY_COUNT = AiConstants.WITH_PAY_BLUE_ACTIONS.size();
    public static final int COUNT          = NO_PAY_COUNT + WITH_PAY_COUNT;

    private static final int OFFSET_NO_PAY   = 0;
    private static final int OFFSET_WITH_PAY = NO_PAY_COUNT;

    private static final HeadAction[] CACHE = new HeadAction[COUNT];

    static {
        for (int i = 0; i < COUNT; i++) {
            final int localIndex = i;
            CACHE[i] = new HeadAction() {
                @Override public ActionHead head()  { return ActionHead.BLUE; }
                @Override public int localIndex()   { return localIndex; }
                @Override public String toString()  { return "BlueAction[" + localIndex + "]"; }
            };
        }
    }

    public static HeadAction forNoPayIndex(int index) {
        if (index < 0 || index >= NO_PAY_COUNT)
            throw new IllegalArgumentException("BlueAction.noPay: index " + index + " out of [0," + NO_PAY_COUNT + ")");
        return CACHE[OFFSET_NO_PAY + index];
    }

    public static HeadAction forWithPayIndex(int index) {
        if (index < 0 || index >= WITH_PAY_COUNT)
            throw new IllegalArgumentException("BlueAction.withPay: index " + index + " out of [0," + WITH_PAY_COUNT + ")");
        return CACHE[OFFSET_WITH_PAY + index];
    }

    private BlueAction() {}
}
