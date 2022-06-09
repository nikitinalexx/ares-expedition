package com.terraforming.ares.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 08.06.2022
 */
@Data
@Builder
public class LobbyGameDto {
    private final String name;
    private final long id;
    private final boolean mulligan;
    private final Map<String, Boolean> playerToStartConfirm;
}
