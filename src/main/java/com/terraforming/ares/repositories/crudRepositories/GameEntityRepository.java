package com.terraforming.ares.repositories.crudRepositories;

import com.terraforming.ares.entity.GameEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 17.05.2022
 */
public interface GameEntityRepository extends CrudRepository<GameEntity, Long> {

    GameEntity findById(long id);

    List<GameEntity> findAllBy();
}
