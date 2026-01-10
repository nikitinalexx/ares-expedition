package com.terraforming.ares.services.ai.turnProcessors.network2.buildParams;

import com.terraforming.ares.services.ai.dl4j.Prediction;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DecisionWithPrediction<T>{
    T value;
    Prediction prediction;
}
