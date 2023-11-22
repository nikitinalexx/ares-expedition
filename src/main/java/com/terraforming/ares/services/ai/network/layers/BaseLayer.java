package com.terraforming.ares.services.ai.network.layers;


import com.terraforming.ares.services.ai.network.DataColumn;
import com.terraforming.ares.services.ai.network.activationFunctions.Signal;

public abstract class BaseLayer {
    protected BaseLayer prev;
    protected BaseLayer next;
    protected DataColumn weights;
    protected DataColumn input;
    protected DataColumn result;
    protected final Signal signal;
    protected int width;
    protected int height;
    protected int depth;
    protected float[] biases;

    public BaseLayer(Signal signal) {
        this.signal = signal;
    }

    public void push() {};

    public void setPrev(BaseLayer prev) {
        this.prev = prev;
    }

    public void setNext(BaseLayer next) {
        this.next = next;
    }

    public final DataColumn getResult() {
        return this.result;
    }

}
