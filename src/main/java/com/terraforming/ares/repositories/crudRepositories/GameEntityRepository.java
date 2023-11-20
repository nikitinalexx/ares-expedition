package com.terraforming.ares.repositories.crudRepositories;

import com.terraforming.ares.entity.GameEntity;
import com.terraforming.ares.entity.SoloRecordEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 17.05.2022
 */
public interface GameEntityRepository extends CrudRepository<GameEntity, Long> {

    GameEntity findById(long id);

    List<GameEntity> findAllBy();


    @Modifying
    @Query(value = "DELETE FROM game_entity g WHERE NOT EXISTS " +
            "(SELECT 1 FROM player_entity p WHERE p.game_id = g.id)",
            nativeQuery = true)
    void clearGameMemory();

    @Query(value = "SELECT * FROM game_entity g WHERE finished = true " +
            "ORDER BY finished_date DESC, id DESC LIMIT 20", nativeQuery = true)
    List<GameEntity> findRecentTwentyGames();


}
