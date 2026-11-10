package com.terraforming.ares.services.policyai.action;

/**
 * Голова SYSTEM — системные действия.
 * Все действия фиксированные — используй константы напрямую.
 */
public enum SystemAction implements HeadAction {

    ENTER_SELL_MODE  (0),
    ENTER_HELION_MODE(1),
    HEAT_EXCHANGE    (2),
    UNMI_RT_FOR_6_MC (3),
    TAKE_BONUS       (4),
    EXIT_SELL_MODE(5),
    EXIT_HELION_MODE(6);

    public static final int COUNT = 7;

    private final int localIndex;

    SystemAction(int localIndex) {
        this.localIndex = localIndex;
    }

    @Override public ActionHead head()  { return ActionHead.SYSTEM; }
    @Override public int localIndex()   { return localIndex; }
}
