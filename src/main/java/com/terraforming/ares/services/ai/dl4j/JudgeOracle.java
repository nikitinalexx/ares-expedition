package com.terraforming.ares.services.ai.dl4j;

import com.terraforming.ares.services.ai.AiConstants;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class JudgeOracle {

    private final MultiLayerNetwork baseNet;

    public JudgeOracle() throws Exception {
        this.baseNet = MultiLayerNetwork.load(new File("436_epoch_1_8495_5228.zip"), false);

    }


    public Prediction predict(float[] fullFeaturesArray) {
        if (fullFeaturesArray.length != AiConstants.TABLE_VECTOR_SIZE) {
            throw new IllegalArgumentException("Array size mismatch! Expected " + AiConstants.TABLE_VECTOR_SIZE + ", got " + fullFeaturesArray.length);
        }

        INDArray full = Nd4j.create(fullFeaturesArray, 1, AiConstants.TABLE_VECTOR_SIZE);

        return new Prediction(baseNet.output(full, false).getDouble(0));
    }

}
