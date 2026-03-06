package com.terraforming.ares.services.policyai.action;

/**
 * Голова SYSTEM — системные действия.
 * Все действия фиксированные — используй константы напрямую.
 */
public enum SystemAction implements HeadAction {

    ENTER_SELL_MODE  (0),
    ENTER_HELION_MODE(1),
    HEAT_EXCHANGE    (2),
    PASS             (3),
    UNMI_RT_FOR_6_MC (4),
    TAKE_BONUS       (5);

    public static final int COUNT = 6;

    private final int localIndex;

    SystemAction(int localIndex) {
        this.localIndex = localIndex;
    }

    @Override public ActionHead head()  { return ActionHead.SYSTEM; }
    @Override public int localIndex()   { return localIndex; }
}
