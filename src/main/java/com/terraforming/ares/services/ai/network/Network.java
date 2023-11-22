package com.terraforming.ares.services.ai.network;


import com.terraforming.ares.services.ai.network.layers.BaseLayer;
import com.terraforming.ares.services.ai.network.layers.InsertLayer;
import com.terraforming.ares.services.ai.network.layers.MiddleLayer;
import com.terraforming.ares.services.ai.network.layers.ResultLayer;

import java.util.ArrayList;
import java.util.List;

public class Network {
    private final InsertLayer inputLayer;
    private final ResultLayer outputLayer;

    private final List<BaseLayer> layers;

    public Network(int inputSize, DataColumn middleWeights, float[] middleBiases, DataColumn outputLayerWeights, float[] outputLayerBiases) {
        inputLayer = new InsertLayer(inputSize);
        MiddleLayer middleLayer = new MiddleLayer(inputSize);
        outputLayer = new ResultLayer(1);

        inputLayer.setNext(middleLayer);
        middleLayer.setNext(outputLayer);

        middleLayer.setPrev(inputLayer);
        outputLayer.setPrev(middleLayer);

        layers = new ArrayList<>();
        layers.add(inputLayer);
        layers.add(middleLayer);
        layers.add(outputLayer);

        inputLayer.init();
        middleLayer.init(middleWeights, middleBiases);
        outputLayer.init(outputLayerWeights, outputLayerBiases);
    }

    public void setInput(DataColumn inputs) {
        this.inputLayer.setInput(inputs);
        this.forward();
    }

    public float[] getOutput() {
        return this.outputLayer.getResult().getValues();
    }

    protected void forward() {
        for (int i = 1; i < this.layers.size(); ++i) {
            this.layers.get(i).push();
        }
    }

}
