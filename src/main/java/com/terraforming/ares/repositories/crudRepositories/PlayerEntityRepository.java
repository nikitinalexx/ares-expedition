package com.terraforming.ares.repositories.crudRepositories;

import com.terraforming.ares.entity.PlayerEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by oleksii.nikitin
 * Creation date 17.05.2022
 */
public interface PlayerEntityRepository extends CrudRepository<PlayerEntity, Long> {

    PlayerEntity findByUuid(String uuid);


    @Modifying
    @Query(value = "DELETE FROM player_entity " +
            "WHERE id NOT IN ((" +
            "        SELECT id FROM player_entity" +
            "        ORDER BY id DESC LIMIT 500)" +
            "    UNION ALL " +
            "    SELECT player_entity.id FROM player_entity" +
            "    WHERE player_entity.uuid IN (" +
            "        SELECT uuid FROM crisis_record_entity" +
            "        UNION ALL " +
            "        SELECT uuid FROM solo_record_entity" +
            "        UNION ALL " +
            "        SELECT uuid FROM player_entity WHERE game_id IN (SELECT id FROM game_entity WHERE finished = true ORDER BY finished_date DESC, id DESC LIMIT 20)" +
            "))",
            nativeQuery = true)
    void clearPlayerMemory();
}
