package com.terraforming.ares.dto;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by oleksii.nikitin
 * Creation date 17.05.2022
 */
@Getter
@Setter
@Builder
public class RecentGameDto {

    @Id
    private String uuid;
    private String playerName;
    private int playerCount;
    private int victoryPoints;
    private int turns;
    private String date;
    private boolean isCrisis;

}
