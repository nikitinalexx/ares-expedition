package com.terraforming.ares.services.ai.network2.dto;

import com.terraforming.ares.services.ai.turnProcessors.frontier.Node;
import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class BestFutureOptions {
    List<Map.Entry<State, Node>> bestFutureOptions;
    double bestChance;
}
