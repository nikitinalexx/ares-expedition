
package com.terraforming.ares.services.ai.network.layers;

import com.terraforming.ares.services.ai.network.DataColumn;
import com.terraforming.ares.services.ai.network.activationFunctions.RelSignal;
import com.terraforming.ares.services.ai.network.range.RangeCallable;
import com.terraforming.ares.services.ai.network.threads.CustomNetworkThreadPool;

import java.util.ArrayList;
import java.util.concurrent.Callable;

public final class MiddleLayer extends BaseLayer {
    private transient ArrayList<Callable<Void>> callables;

    public MiddleLayer(int width) {
        super(new RelSignal());
        if (width <= 0) {
            throw new IllegalArgumentException("Invalid data " + width);
        }
        this.width = width;
        this.height = 1;
        this.depth = 1;
    }

    public void init(DataColumn weights, float[] biases) {
        this.input = this.prev.result;
        this.result = new DataColumn(this.width);
        this.weights = weights;
        this.biases = biases;
        this.initCallables();
    }

    private void initCallables() {
        int threads = Math.min(CustomNetworkThreadPool.getInstance().getThreadCount(), this.width);
        if (threads > 1) {
            int[] splitPerThread = CustomNetworkThreadPool.calculateCellsPerThread(this.width, threads);
            this.callables = new ArrayList<>();
            int from;
            int to = 0;

            for (int i = 0; i < threads; ++i) {
                from = to;
                to += splitPerThread[i];
                this.callables.add(new RangeCallable(from, to, this::push));
            }
        }
    }

    public void push() {
        this.start();
    }

    private void start() {
        this.result.copyFrom(this.biases);

        try {
            CustomNetworkThreadPool.getInstance().run(this.callables);
        } catch (InterruptedException e) {
            System.out.println("InterruptedException " + e);
        }
    }

    private void push(int from, int to) {
        for (int i = from; i < to; i++) {
            float sum = 0.0F;

            for (int j = 0; j < this.input.size(); j++) {
                sum += this.input.get(j) * this.weights.get(j, i);
            }

            this.result.add(sum, i);
        }

        this.signal.apply(this.result, from, to);
    }

}
