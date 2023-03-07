package com.terraforming.ares.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.terraforming.ares.model.Constants;
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
public class CrisisRecordEntity {

    @Id
    private String uuid;
    private String playerName;
    private int victoryPoints;
    private int terraformingPoints;
    private int playerCount;
    private int turnsLeft;
    @JsonIgnore
    private LocalDateTime date;
    //0=default, -1=beginner
    private int difficulty = Constants.CRISIS_NORMAL_DIFFICULTY;


    public CrisisRecordEntity() {
    }


}
