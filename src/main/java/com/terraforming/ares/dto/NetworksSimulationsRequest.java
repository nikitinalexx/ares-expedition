package com.terraforming.ares.dto;

import lombok.Data;

import java.util.List;

@Data
public class NetworksSimulationsRequest {
    int simulationsCount;
    List<String> networks;
}
