package com.terraforming.ares.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 17.05.2022
 */
@Entity
public class GameEntity {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String gameJson;

    @Column
    @Getter
    @Setter
    private boolean finished = false;

    @Column
    @Getter
    @Setter
    private Instant finishedDate;

    @OneToMany(mappedBy = "game")
    private Set<PlayerEntity> items;

    protected GameEntity() {
    }

    public GameEntity(String gameJson) {
        this.gameJson = gameJson;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGameJson() {
        return gameJson;
    }

    public void setGameJson(String gameJson) {
        this.gameJson = gameJson;
    }
}
