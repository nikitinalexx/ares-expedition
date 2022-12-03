package com.terraforming.ares.services.ai;

import org.springframework.stereotype.Service;

@Service
public class AiBalanceService {
    public static final double VALUE_WORTH_TO_DISCARD = 0.45;

    public boolean isCardWorthToDiscard(double value) {
        return value <= VALUE_WORTH_TO_DISCARD;
    }
}
