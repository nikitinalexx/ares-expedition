package com.terraforming.ares.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.06.2022
 */
@Builder
@Getter
@Setter
public class LobbyGame {
    private String name;
    private final boolean mulligan;
    private final Map<String, Boolean> playerToStartConfirm;
    private Map<String, String> playerToPlayerUuid;
    private Long confirmsReceivedTime;
}
