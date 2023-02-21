package com.terraforming.ares.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class LobbyGameRequest {
    private String player;
    private long gameId;
}
