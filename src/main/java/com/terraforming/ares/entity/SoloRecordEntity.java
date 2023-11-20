package com.terraforming.ares.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

/**
 * Created by oleksii.nikitin
 * Creation date 17.05.2022
 */
@Entity
@Getter
@Setter
public class SoloRecordEntity {

    @Id
    private String uuid;
    private String playerName;
    private int victoryPoints;
    private int turns;
    @JsonIgnore
    private LocalDateTime date;

    public SoloRecordEntity() {
    }

}
