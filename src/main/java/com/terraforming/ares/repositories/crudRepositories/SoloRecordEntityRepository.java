package com.terraforming.ares.repositories.crudRepositories;

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
public interface SoloRecordEntityRepository extends CrudRepository<SoloRecordEntity, Long> {

    SoloRecordEntity findByUuid(String uuid);

    @Query(value = "SELECT * FROM solo_record_entity m " +
            "ORDER BY turns ASC, m.victory_points DESC, date LIMIT 20", nativeQuery = true)
    List<SoloRecordEntity> findTopTwentyRecordsByTurns();

    @Modifying
    @Query(value = "DELETE FROM solo_record_entity m " +
            "WHERE uuid NOT IN (" +
            "    SELECT uuid" +
            "    FROM solo_record_entity" +
            "    ORDER BY turns ASC, m.victory_points DESC, date LIMIT 20)",nativeQuery = true)
    void clearSoloRecordMemory();
}
