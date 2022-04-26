package com.terraforming.ares.controllers;

import com.terraforming.ares.dto.GameDto;
import com.terraforming.ares.dto.PlayerContextDto;
import com.terraforming.ares.dto.PlayerUuidsDto;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.services.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@RestController
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;

    @PostMapping("/game/new")
    public PlayerUuidsDto startNewGame(GameParameters gameParameters) {
        MarsGame marsGame = gameService.startNewGame(gameParameters);

        return PlayerUuidsDto.builder()
                .players(new ArrayList<>(marsGame.getPlayerContexts().keySet()))
                .build();
    }

    @GetMapping("/game/player/{playerUuid}")
    public GameDto getGameByPlayerUuid(@PathVariable String playerUuid) {
        MarsGame game = gameService.getGame(playerUuid);

        return GameDto.builder()
                .player(buildCurrentPlayer(game.getPlayerByUuid(playerUuid)))
                .otherPlayers(Collections.emptyList())//TODO
                .build();
    }

    private PlayerContextDto buildCurrentPlayer(PlayerContext playerContext) {
        Deck corporations = playerContext.getCorporations();

        return PlayerContextDto.builder()
                .corporationsChoice(
                        Deck.builder()
                                .cards(corporations.getCards())
                                .build()
                )
                .build();
    }

}
