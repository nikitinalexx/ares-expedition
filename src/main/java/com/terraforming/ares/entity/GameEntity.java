package com.terraforming.ares.entity;

import org.springframework.beans.factory.annotation.Value;

import javax.persistence.*;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 17.05.2022
 */
@Entity
public class GameEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(columnDefinition="TEXT")
    private String gameJson;

    @OneToMany(mappedBy="game")
    private Set<PlayerEntity> items;

    protected GameEntity() {
    }

    public GameEntity(String gameJson) {
        this.gameJson = gameJson;
    }

    public Long getId() {
        return id;
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
