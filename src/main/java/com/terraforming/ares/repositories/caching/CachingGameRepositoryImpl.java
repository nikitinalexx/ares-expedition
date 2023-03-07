package com.terraforming.ares.repositories.caching;

import com.terraforming.ares.entity.CrisisRecordEntity;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.GameUpdateResult;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.repositories.GameRepository;
import com.terraforming.ares.repositories.crudRepositories.CrisisRecordEntityRepository;
import com.terraforming.ares.services.WinPointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class CachingGameRepositoryImpl implements CachingGameRepository {
    private final GameRepository gameRepository;
    private final CrisisRecordEntityRepository crisisRecordEntityRepository;
    private final WinPointsService winPointsService;

    private final Map<Long, MarsGame> cache = new ConcurrentHashMap<>();
    //TODO eviction
    private Map<String, Long> playerToGameIdCache = new ConcurrentHashMap<>();

    @Override
    public long newGame(MarsGame game) {
        long id = gameRepository.save(game);
        cache.put(id, game);
        for (String playerUuid : game.getPlayerUuidToPlayer().keySet()) {
            playerToGameIdCache.put(playerUuid, id);
        }
        return id;
    }

    @Override
    public MarsGame getGameById(long id) {
        return cache.computeIfAbsent(id, gameRepository::getGameById);
    }

    @Override
    public long getGameIdByPlayerUuid(String playerUuid) {
        return playerToGameIdCache.computeIfAbsent(playerUuid, key -> gameRepository.getGameIdByPlayerUuid(playerUuid));
    }

    @Override
    public MarsGame getGameByPlayerUuid(String playerUuid) {
        long gameId = getGameIdByPlayerUuid(playerUuid);
        return getGameById(gameId);
    }

    @Override
    public <T> GameUpdateResult<T> updateMarsGame(long id, Function<MarsGame, String> stateChecker, Function<MarsGame, T> updater) {
        GameUpdateResult.GameUpdateResultBuilder<T> resultBuilder = GameUpdateResult.builder();

        cache.compute(id, (key, game) -> {
            if (game == null) {
                game = gameRepository.getGameById(key);
            }

            String validationError = stateChecker.apply(game);

            resultBuilder.error(validationError);

            if (validationError == null) {
                T updateResult = updater.apply(game);
                resultBuilder.result(updateResult);

                game.iterateUpdateCounter();
                if (game.timeToSave()) {
                    gameRepository.save(game);
                    if (game.isCrysis() && game.getCrysisData().isWonGame()) {
                        crisisRecordEntityRepository.save(getCrisisRecordEntity(game));
                    }

                }
            }

            return game;
        });

        return resultBuilder.build();
    }

    private CrisisRecordEntity getCrisisRecordEntity(MarsGame game) {
        CrisisRecordEntity crisisRecordEntity = new CrisisRecordEntity();
        final Player player = game.getPlayerUuidToPlayer().values().iterator().next();
        crisisRecordEntity.setUuid(player.getUuid());
        crisisRecordEntity.setPlayerName(player.getName());
        crisisRecordEntity.setVictoryPoints(winPointsService.countCrysisWinPoints(game));
        crisisRecordEntity.setTerraformingPoints(
                game.getPlayerUuidToPlayer().values().stream().mapToInt(
                        Player::getTerraformingRating
                ).sum()
        );
        crisisRecordEntity.setPlayerCount(game.getPlayerUuidToPlayer().size());
        crisisRecordEntity.setTurnsLeft(game.getCrysisData().getCrysisCards().size());
        crisisRecordEntity.setDate(LocalDateTime.now(ZoneId.of("UTC")));
        crisisRecordEntity.setDifficulty(
                game.getCrysisData().isEasyMode()
                        ? Constants.CRISIS_BEGINNER_DIFFICULTY
                        : Constants.CRISIS_NORMAL_DIFFICULTY);
        return crisisRecordEntity;
    }

    @Override
    public int evictGameCache() {
        int sizeBeforeEviction = cache.size();
        cache.clear();
        return sizeBeforeEviction;
    }

}
