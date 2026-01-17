package com.terraforming.ares.services.ai.network2.dto;

import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StateWithVector {
    State state;
    float[] vector;
}
