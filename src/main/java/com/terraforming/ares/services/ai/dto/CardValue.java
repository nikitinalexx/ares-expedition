package com.terraforming.ares.services.ai.dto;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class CardValue {
    boolean isNan;
    double value;
}
