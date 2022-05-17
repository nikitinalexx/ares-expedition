package com.terraforming.ares.entity;

import javax.persistence.*;

/**
 * Created by oleksii.nikitin
 * Creation date 17.05.2022
 */
@Entity
public class PlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String uuid;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    private GameEntity game;

    protected PlayerEntity() {

    }

    public PlayerEntity(String uuid) {
        this.uuid = uuid;
    }

    public Long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setGame(GameEntity game) {
        this.game = game;
    }

    public GameEntity getGame() {
        return game;
    }
}
