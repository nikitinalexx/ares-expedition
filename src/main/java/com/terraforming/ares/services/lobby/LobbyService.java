package com.terraforming.ares.services.lobby;

import com.terraforming.ares.dto.LobbyDto;
import com.terraforming.ares.dto.LobbyGameDto;
import com.terraforming.ares.dto.PlayerReference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.LobbyGame;
import com.terraforming.ares.model.request.NewLobbyGameRequest;
import com.terraforming.ares.services.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.terraforming.ares.model.Constants.REMOVE_GAMES_FROM_LOBBY_AFTER_SECONDS;
import static com.terraforming.ares.model.Constants.REMOVE_PLAYERS_FROM_LOBBY_AFTER_SECONDS;

/**
 * Created by oleksii.nikitin
 * Creation date 07.06.2022
 */
@Service
@RequiredArgsConstructor
public class LobbyService {
    public static final String GAME_NOT_FOUND = "Game not found";
    private final Map<String, Long> playerToLastAccessTime = new HashMap<>();
    private final Map<Long, LobbyGame> games = new HashMap<>();
    private final Map<String, Long> playerToLobbyGameId = new HashMap<>();
    private final GameService gameService;

    private long gameIdCounter = System.currentTimeMillis();

    @Scheduled(fixedDelay = 10000)
    public synchronized void cleanStartedGames() {
        long currentTime = System.currentTimeMillis();

        List<Long> gamesToRemove = games.entrySet().stream().filter(
                entry -> entry.getValue().getConfirmsReceivedTime() != null
                        && currentTime - entry.getValue().getConfirmsReceivedTime() >= REMOVE_GAMES_FROM_LOBBY_AFTER_SECONDS * 1000
        ).map(Map.Entry::getKey)
                .collect(Collectors.toList());

        for (Long gameId : gamesToRemove) {
            LobbyGame lobbyGame = games.get(gameId);

            for (String player : lobbyGame.getPlayerToStartConfirm().keySet()) {
                if (playerToLobbyGameId.containsKey(player) && playerToLobbyGameId.get(player).equals(gameId)) {
                    playerToLobbyGameId.remove(player);
                }
            }

            games.remove(gameId);
        }

        List<String> players = playerToLastAccessTime.entrySet().stream().filter(
                entry -> currentTime - entry.getValue() >= REMOVE_PLAYERS_FROM_LOBBY_AFTER_SECONDS * 1000
        ).map(Map.Entry::getKey).collect(Collectors.toList());

        for (String player : players) {
            playerToLastAccessTime.remove(player);

            Long lobbyGameId = playerToLobbyGameId.get(player);
            if (lobbyGameId != null) {
                playerToLobbyGameId.remove(player);
                LobbyGame lobbyGame = games.get(lobbyGameId);

                if (lobbyGame != null && lobbyGame.getPlayerToStartConfirm().containsKey(player) && !lobbyGame.getPlayerToStartConfirm().get(player)) {
                    lobbyGame.getPlayerToStartConfirm().remove(player);

                    if (lobbyGame.getPlayerToStartConfirm().isEmpty()) {
                        games.remove(lobbyGameId);
                    }
                }
            }
        }

    }

    public synchronized PlayerReference getGamePlayerUuid(String player, long gameId) {
        LobbyGame lobbyGame = games.get(gameId);

        if (lobbyGame == null) {
            throw new IllegalArgumentException(GAME_NOT_FOUND);
        }

        return PlayerReference.builder()
                .uuid(lobbyGame.getPlayerToPlayerUuid().get(player))
                .name(player)
                .build();
    }

    public synchronized LobbyDto getLobby(String player) {
        playerToLastAccessTime.put(player, System.currentTimeMillis());

        return LobbyDto.builder()
                .playerGame(
                        playerToLobbyGameId.containsKey(player)
                                ? fromLobbyGame(playerToLobbyGameId.get(player), games.get(playerToLobbyGameId.get(player)))
                                : null
                )
                .players(new ArrayList<>(playerToLastAccessTime.keySet()))
                .games(games.entrySet().stream().filter(
                        entry ->
                                !entry.getValue().getPlayerToStartConfirm().values().stream().allMatch(
                                        Boolean.TRUE::equals
                                )

                        )
                                .map(entry -> fromLobbyGame(entry.getKey(), entry.getValue()))
                                .collect(Collectors.toList())
                )
                .build();
    }

    private static LobbyGameDto fromLobbyGame(long id, LobbyGame lobbyGame) {
        return LobbyGameDto.builder()
                .id(id)
                .name(lobbyGame.getName())
                .playerToStartConfirm(new HashMap<>(lobbyGame.getPlayerToStartConfirm()))
                .mulligan(lobbyGame.isMulligan())
                .build();
    }


    //todo scheduler that deletes players if it considers them disconnected

    public synchronized void createGame(NewLobbyGameRequest lobbyGameRequest) {
        if (!playerToLastAccessTime.containsKey(lobbyGameRequest.getHost())) {
            throw new IllegalArgumentException("Can't create a game outside of lobby");
        }

        cleanOtherGames(lobbyGameRequest.getHost());

        long gameId = gameIdCounter++;

        Map<String, Boolean> playerToConfirmedStart = new HashMap<>();

        playerToConfirmedStart.put(lobbyGameRequest.getHost(), false);

        games.put(
                gameId,
                LobbyGame.builder()
                        .name(lobbyGameRequest.getHost() + " game")
                        .mulligan(lobbyGameRequest.isMulligan())
                        .playerToStartConfirm(playerToConfirmedStart)
                        .build()
        );

        playerToLobbyGameId.put(lobbyGameRequest.getHost(), gameId);

    }

    public synchronized void joinGame(String player, long gameId) {
        if (!playerToLastAccessTime.containsKey(player)) {
            throw new IllegalArgumentException("Can't join a game from outside of lobby");
        }

        if (!games.containsKey(gameId)) {
            throw new IllegalArgumentException(GAME_NOT_FOUND);
        }

        LobbyGame game = games.get(gameId);

        if (game.getPlayerToStartConfirm().size() == Constants.MAX_PLAYERS) {
            throw new IllegalArgumentException("Game is full");
        }

        if (game.getPlayerToStartConfirm().containsKey(player)) {
            throw new IllegalArgumentException("You already joined the game");
        }

        cleanOtherGames(player);

        game.getPlayerToStartConfirm().put(player, false);
        playerToLobbyGameId.put(player, gameId);
    }

    public synchronized void leaveGame(String player, long gameId) {
        if (!playerToLastAccessTime.containsKey(player)) {
            throw new IllegalArgumentException("Can't leave a game from outside of lobby");
        }

        if (!games.containsKey(gameId)) {
            throw new IllegalArgumentException(GAME_NOT_FOUND);
        }

        LobbyGame game = games.get(gameId);


        if (!game.getPlayerToStartConfirm().containsKey(player)) {
            throw new IllegalArgumentException("Can't leave a game which you didn't join");
        }

        if (!game.getPlayerToStartConfirm().values().stream().allMatch(Boolean.TRUE::equals)) {
            //if game not started yet, remove player from game
            game.getPlayerToStartConfirm().remove(player);
            game.getPlayerToStartConfirm().replaceAll((key, value) -> false);

            if (game.getPlayerToStartConfirm().isEmpty()) {
                games.remove(gameId);
            }
        }

        playerToLobbyGameId.remove(player);
    }

    public synchronized void confirmGameStart(String player, long gameId) {
        if (!playerToLastAccessTime.containsKey(player)) {
            throw new IllegalArgumentException("Can't join a game from outside of lobby");
        }

        if (!games.containsKey(gameId)) {
            throw new IllegalArgumentException(GAME_NOT_FOUND);
        }

        LobbyGame game = games.get(gameId);

        if (!game.getPlayerToStartConfirm().containsKey(player)) {
            throw new IllegalArgumentException("You didn't join the game");
        }

        game.getPlayerToStartConfirm().put(player, true);

        if (game.getPlayerToStartConfirm().values().stream().allMatch(Boolean.TRUE::equals)) {
            boolean mulligan = game.isMulligan();

            MarsGame marsGame = gameService.startNewGame(
                    GameParameters.builder()
                            .mulligan(mulligan)
                            .expansions(List.of(Expansion.BASE))
                            .playerNames(new ArrayList<>(game.getPlayerToStartConfirm().keySet()))
                            .build()
            );

            game.setPlayerToPlayerUuid(
                    marsGame.getPlayerUuidToPlayer()
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(p -> p.getValue().getName(), Map.Entry::getKey))
            );

            game.setConfirmsReceivedTime(System.currentTimeMillis());
        }
    }

    private void cleanOtherGames(String player) {
        if (!playerToLobbyGameId.containsKey(player)) {
            return;
        }

        long gameId = playerToLobbyGameId.get(player);

        LobbyGame lobbyGame = games.get(gameId);

        if (lobbyGame == null) {
            return;
        }

        lobbyGame.getPlayerToStartConfirm().remove(player);

        if (lobbyGame.getPlayerToStartConfirm().isEmpty()) {
            games.remove(gameId);
        }
    }

}
