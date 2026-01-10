package com.terraforming.ares.services.ai.turnProcessors.frontier;

public final class Node {
    public final State state;
    public final long usedActionsMask;
    public final long usedRepeatMask;
    public int repeatsLeft;


    public final int firstActionId;
    public final Object firstActionContext;

    public Node(State state, long usedActionsMask, long usedRepeatMask, int repeatsLeft) {
        this(state, usedActionsMask, usedRepeatMask, repeatsLeft, -1, null);
    }

    public Node(
            State state,
            long usedActionsMask,
            long usedRepeatMask,
            int repeatsLeft,
            int firstActionId,
            Object firstActionContext
    ) {
        this.state = state;
        this.usedActionsMask = usedActionsMask;
        this.usedRepeatMask = usedRepeatMask;
        this.repeatsLeft = repeatsLeft;
        this.firstActionId = firstActionId;
        this.firstActionContext = firstActionContext;
    }
}
