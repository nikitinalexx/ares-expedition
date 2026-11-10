package com.terraforming.ares.services.ai.dl4j;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.AiConstants;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Value-network inference (win probability) for NETWORK_V2 computers.
 * <p>
 * Runs on plain CPU in pure Java ({@link PureJavaValueNetwork}) - no CUDA, ND4J or DL4J at runtime, so it works the same
 * locally, on a machine without GPU and on Heroku. The model files are the ordinary DL4J zips produced by the
 * {@code deep} training project; they are only read, never trained here.
 */
@Service
public class NNService {
    // Перечисление для выбора модели
    public enum ModelType {
        FIRST,
        SECOND
    }

    private static final String FIRST_MODEL = "iter4_epoch_2_93.zip";
    private static final String SECOND_MODEL = "iter4_epoch_2_93.zip";

    // Architecture of the trained graph (see configuration.json in the model zip)
    private static final int DENSE_TABLE = 512;
    private static final int DENSE_HAND = 512;
    private static final int DENSE_HIDDEN = 1024;

    private final Map<ModelType, PureJavaValueNetwork> networks = new EnumMap<>(ModelType.class);

    public NNService() throws IOException {
        networks.put(ModelType.FIRST, load(FIRST_MODEL));
        // the same file twice -> share the instance (immutable, thread-safe)
        networks.put(ModelType.SECOND, FIRST_MODEL.equals(SECOND_MODEL) ? networks.get(ModelType.FIRST) : load(SECOND_MODEL));
    }

    private static PureJavaValueNetwork load(String fileName) throws IOException {
        return PureJavaValueNetwork.load(new File(fileName),
                AiConstants.TABLE_VECTOR_SIZE, AiConstants.HAND_VECTOR_SIZE,
                DENSE_TABLE, DENSE_HAND, DENSE_HIDDEN);
    }

    public List<Prediction> predictBatch(List<float[]> featuresBatch, ModelType modelType) {
        if (featuresBatch.isEmpty()) return List.of();

        float[] probs = networks.get(modelType).predictBatch(featuresBatch.toArray(new float[0][]));
        List<Prediction> result = new ArrayList<>(probs.length);
        for (float p : probs) {
            result.add(new Prediction(p));
        }
        return result;
    }

    public List<Prediction> predictBatch(List<float[]> featuresBatch, Player player) {
        return predictBatch(featuresBatch, player.isFirstBot() ? ModelType.FIRST : ModelType.SECOND);
    }
}
