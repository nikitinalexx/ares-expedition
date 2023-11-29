package com.terraforming.ares.services.ai.network.range;

import java.util.concurrent.Callable;

public class RangeCallable implements Callable<Void> {
    private final int from;
    private final int to;
    private final RangeFunction func;

    public RangeCallable(int from, int to, RangeFunction func) {
        this.from = from;
        this.to = to;
        this.func = func;
    }

    public Void call() {
        this.func.accept(this.from, this.to);
        return null;
    }
}
