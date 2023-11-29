package com.terraforming.ares.services.ai.network.layers;


import com.terraforming.ares.services.ai.network.DataColumn;
import com.terraforming.ares.services.ai.network.activationFunctions.SimpleSignal;

public class InsertLayer extends BaseLayer {

    public InsertLayer(int width) {
        super(new SimpleSignal());
        this.width = width;
        this.height = 1;
        this.depth = 1;
        this.init();
    }

    public final void init() {
        this.input = new DataColumn(this.height, this.width, this.depth);
        this.result = this.input;
    }

    public void setInput(DataColumn in) {
        this.input.setValues(in.getValues());
    }


}
