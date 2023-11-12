package com.terraforming.ares.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Id;
import java.time.Instant;

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
