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
            "WHERE id NOT IN (" +
            "    SELECT id" +
            "    FROM (" +
            "        SELECT id FROM player_entity" +
            "        ORDER BY id DESC LIMIT 100" +
            "    ) AS latest_players" +
            "    UNION" +
            "    SELECT player_entity.id FROM player_entity" +
            "    WHERE player_entity.uuid IN (" +
            "        SELECT uuid FROM crisis_record_entity" +
            "        UNION" +
            "        SELECT uuid FROM solo_record_entity))",
            nativeQuery = true)
    void clearPlayerMemory();
}
