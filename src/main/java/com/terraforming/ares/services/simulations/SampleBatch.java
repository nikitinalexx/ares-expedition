package com.terraforming.ares.services.simulations;

import java.util.List;

public record SampleBatch(
        List<float[]> features,
        List<Float> labels
) {}

