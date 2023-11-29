
package com.terraforming.ares.services.ai.network.layers;

import com.terraforming.ares.services.ai.network.DataColumn;
import com.terraforming.ares.services.ai.network.activationFunctions.SigSignal;

public class ResultLayer extends BaseLayer {

    public ResultLayer(int width) {
        super(new SigSignal());
        this.width = width;
        this.height = 1;
        this.depth = 1;
    }

    public void init(DataColumn weights, float[] biases) {
        this.input = this.prev.result;
        this.result = new DataColumn(this.width);

        this.weights = weights;
        this.biases = biases;
    }

    public void push() {
        this.result.copyFrom(this.biases);
        float sum;

        for (int i = 0; i < this.result.size(); i++) {
            sum = 0.0F;

            for (int j = 0; j < this.input.size(); j++) {
                sum += this.input.get(j) * this.weights.get(j, i);
            }

            this.result.add(sum, i);
        }

        this.result.apply(this.signal::getValue);
    }

}
