package com.terraforming.ares.services.ai.dl4j;

/**
 * Value network: [table | hand] feature vectors -> win probabilities.
 */
public interface ValueNetwork {
    float[] predictBatch(float[][] features);
}
