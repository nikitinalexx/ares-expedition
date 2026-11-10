package com.terraforming.ares.services.agent;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class UniformRandomAgent implements DecisionAgent {
    @Override
    public int choose(Decision decision) {
        return ThreadLocalRandom.current().nextInt(decision.candidates().size());
    }
}
