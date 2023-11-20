package com.terraforming.ares.repositories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraforming.ares.dto.RecentGameDto;
import com.terraforming.ares.entity.GameEntity;
import com.terraforming.ares.entity.PlayerEntity;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.repositories.crudRepositories.GameEntityRepository;
import com.terraforming.ares.repositories.crudRepositories.PlayerEntityRepository;
import com.terraforming.ares.services.WinPointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
@Service
@RequiredArgsConstructor
public class GameRepositoryImpl implements GameRepository {
    private final ObjectMapper objectMapper;
    private final GameEntityRepository gameRepository;
    private final PlayerEntityRepository playerRepository;
    private final WinPointsService winPointsService;

    @Override
    @Transactional
    public long save(MarsGame game) {
        boolean newGame = (game.getId() == null);
        List<PlayerEntity> players = game.getPlayerUuidToPlayer().keySet()
                .stream()
                .map(PlayerEntity::new)
                .collect(Collectors.toList());

        GameEntity oldGame = new GameEntity(safeSerialize(game));
        oldGame.setId(game.getId());

        if (game.getStateType() == StateType.GAME_END) {
            oldGame.setFinished(true);
            oldGame.setFinishedDate(Instant.now());
        }

        GameEntity savedGame = gameRepository.save(oldGame);
        if (newGame) {
            game.setId(savedGame.getId());
            savedGame.setGameJson(safeSerialize(game));
            savedGame = gameRepository.save(savedGame);
        }

        if (newGame) {
            for (PlayerEntity player : players) {
                player.setGame(savedGame);
                playerRepository.save(player);
            }
        }

        return savedGame.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public MarsGame getGameById(long id) {
        GameEntity entity = gameRepository.findById(id);
        MarsGame marsGame = safeDeserialize(entity.getGameJson());
        marsGame.setId(entity.getId());
        return marsGame;
    }

    @Transactional(readOnly = true)
    public List<MarsGame> getAllGames() {
        return gameRepository.findAllBy()
                .stream()
                .map(game -> safeDeserialize(game.getGameJson()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getGameIdByPlayerUuid(String playerUuid) {
        return playerRepository.findByUuid(playerUuid).getGame().getId();
    }

    @Override
    public List<RecentGameDto> findRecentTwentyGames() {
        return gameRepository.findRecentTwentyGames()
                .stream()
                .map(gameEntity -> {
                    MarsGame game = safeDeserialize(gameEntity.getGameJson());
                    game.setId(gameEntity.getId());

                    List<Player> players = new ArrayList<>();
                    List<Integer> winPoints = new ArrayList<>();

                    for (Player player : game.getPlayerUuidToPlayer().values()) {
                        players.add(player);
                        winPoints.add(winPointsService.countWinPoints(player, game) + (game.isCrysis() ? player.getTerraformingRating() : 0));
                    }

                    int maxWinPoints = winPoints.get(0);
                    for (Integer winPoint : winPoints) {
                        if (winPoint > maxWinPoints) {
                            maxWinPoints = winPoint;
                        }
                    }

                    Player winner = players.get(0);
                    for (int i = 0; i < winPoints.size(); i++) {
                        if (winPoints.get(i) == maxWinPoints) {
                            winner = players.get(i);
                        }
                    }

                    return RecentGameDto.builder()
                            .uuid(winner.getUuid())
                            .playerName(winner.getName())
                            .playerCount(players.size())
                            .victoryPoints(maxWinPoints)
                            .turns(game.getTurns())
                            .date(gameEntity.getFinishedDate() != null ? DateTimeFormatter.ISO_INSTANT.format(gameEntity.getFinishedDate()) : null)
                            .isCrisis(game.isCrysis())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String safeSerialize(MarsGame marsGame) {
        try {
            return objectMapper.writeValueAsString(marsGame);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializing the game");
        }
    }

    private MarsGame safeDeserialize(String gameJson) {
        try {
            return objectMapper.readValue(gameJson, MarsGame.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error deserializing the game");
        }
    }


}
