package com.terraforming.ares.controllers;

import com.terraforming.ares.dto.PlayerReference;
import com.terraforming.ares.model.request.LobbyGameRequest;
import com.terraforming.ares.dto.LobbyDto;
import com.terraforming.ares.model.request.NewLobbyGameRequest;
import com.terraforming.ares.model.request.JoinLobbyRequest;
import com.terraforming.ares.model.request.PlayerRequest;
import com.terraforming.ares.services.lobby.LobbyService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Created by oleksii.nikitin
 * Creation date 07.06.2022
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/lobby")
public class LobbyController {
    private final LobbyService lobbyService;

    @GetMapping("/{player}")
    public LobbyDto getLobby(@PathVariable String player) {
        if (!StringUtils.hasLength(player) || player.length() > 10) {
            throw new IllegalArgumentException("Max nickname length is 10");
        }
        return lobbyService.getLobby(player);
    }

    @PostMapping("/game/new")
    public void createGame(@RequestBody NewLobbyGameRequest newGameRequest) {
        lobbyService.createGame(newGameRequest);
    }

    @PostMapping("/game/join")
    public void joinGame(@RequestBody LobbyGameRequest joinGameRequest) {
        lobbyService.joinGame(joinGameRequest.getPlayer(), joinGameRequest.getGameId());
    }

    @PostMapping("/game/leave")
    public void leaveGame(@RequestBody LobbyGameRequest leaveGameRequest) {
        lobbyService.leaveGame(leaveGameRequest.getPlayer(), leaveGameRequest.getGameId());
    }

    @PostMapping("/game/confirm")
    public void confirmGameStart(@RequestBody LobbyGameRequest confirmGameStartRequest) {
        lobbyService.confirmGameStart(confirmGameStartRequest.getPlayer(), confirmGameStartRequest.getGameId());
    }

    @PostMapping("/game/uuid")
    public PlayerReference getPlayerUuid(@RequestBody LobbyGameRequest joinGameRequest) {
        return lobbyService.getGamePlayerUuid(joinGameRequest.getPlayer(), joinGameRequest.getGameId());
    }

}
