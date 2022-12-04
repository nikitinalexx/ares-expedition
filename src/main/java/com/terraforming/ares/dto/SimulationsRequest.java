package com.terraforming.ares.dto;

import lombok.Data;

@Data
public class SimulationsRequest {
    int simulationsCount;
    boolean withParamCalibration;
}
