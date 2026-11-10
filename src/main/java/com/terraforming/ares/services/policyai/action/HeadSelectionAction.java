package com.terraforming.ares.services.policyai.action;

/**
 * Голова SYSTEM — системные действия.
 * Все действия фиксированные — используй константы напрямую.
 */
public enum HeadSelectionAction implements HeadAction {

    BUILD_HEAD(0),
    SELL_DISCARD_HEAD(1),
    TARGET_HEAD(2),
    BLUE_HEAD(3),
    CORPORATION_HEAD(4),
    SMALL_HEAD(5),
    SYSTEM_HEAD(6),
    PASS_PHASE_1(7),
    PASS_PHASE_2(8),
    PASS_PHASE_3(9);

    public static final int COUNT = 10;

    private final int localIndex;

    HeadSelectionAction(int localIndex) {
        this.localIndex = localIndex;
    }

    @Override
    public ActionHead head() {
        return ActionHead.HEAD_SELECTION;
    }

    @Override
    public int localIndex() {
        return localIndex;
    }
}
