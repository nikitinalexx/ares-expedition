package com.terraforming.ares.controllers;

import com.terraforming.ares.cards.green.AcquiredCompany;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.GameDto;
import com.terraforming.ares.dto.PlayerDto;
import com.terraforming.ares.dto.PlayerUuidsDto;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin
public class GameController {
    private final GameService gameService;

    @PostMapping("/game/new")
    public PlayerUuidsDto startNewGame(GameParameters gameParameters) {
        MarsGame marsGame = gameService.startNewGame(gameParameters);

        return PlayerUuidsDto.builder()
                .players(new ArrayList<>(marsGame.getPlayerUuidToPlayer().keySet()))
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

    @GetMapping("/projects")
    public List<CardDto> getAllProjectCards() {
        return IntStream.rangeClosed(1, 10)
                .mapToObj(AcquiredCompany::new)
                .map(CardDto::from)
                .collect(Collectors.toList());
    }

    private PlayerDto buildCurrentPlayer(Player player) {
        Deck corporations = player.getCorporations();

        return PlayerDto.builder()
                .corporationsChoice(
                        Deck.builder()
                                .cards(corporations.getCards())
                                .build()
                )
                .corporationId(player.getSelectedCorporationCard())
                .phase(player.getChosenPhase())
                .hand(player.getHand())
                .build();
    }

}
