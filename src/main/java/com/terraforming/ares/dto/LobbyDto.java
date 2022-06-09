package com.terraforming.ares.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.06.2022
 */
@Value
@Builder
public class LobbyDto {
    List<String> players;
    List<LobbyGameDto> games;
    LobbyGameDto playerGame;
}
