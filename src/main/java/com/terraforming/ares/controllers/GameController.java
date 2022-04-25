package com.terraforming.ares.controllers;

import com.terraforming.ares.dto.NewGameDto;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.services.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@RestController
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;

    @PostMapping("/game/new")
    public NewGameDto startNewGame(GameParameters gameParameters) {
        MarsGame marsGame = gameService.startNewGame(gameParameters);

        return NewGameDto.builder()
                .players(new ArrayList<>(marsGame.getPlayerContexts().keySet()))
                .build();
    }

}
