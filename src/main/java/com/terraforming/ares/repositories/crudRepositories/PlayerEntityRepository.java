package com.terraforming.ares.repositories.crudRepositories;

import com.terraforming.ares.entity.PlayerEntity;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by oleksii.nikitin
 * Creation date 17.05.2022
 */
public interface PlayerEntityRepository extends CrudRepository<PlayerEntity, Long> {

    PlayerEntity findByUuid(String uuid);

}
