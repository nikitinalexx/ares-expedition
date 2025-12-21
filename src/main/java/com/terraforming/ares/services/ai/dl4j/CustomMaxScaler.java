package com.terraforming.ares.services.ai.dl4j;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.factory.Nd4j;

public class CustomMaxScaler {

    private final INDArray maxValues;

    public CustomMaxScaler(double[] maxes) {
        this.maxValues = Nd4j.create(maxes);
    }

    public void transform(INDArray features) {
        features.divi(maxValues);
    }


}
