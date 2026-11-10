package com.terraforming.ares;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Starts the application context WITHOUT a database: the engine and the AI work on in-memory games
 * (the same path as /simulations), so tests run anywhere, including CI and the GPU machine.
 */
@SpringBootTest(
        classes = TerraformingAresApplication.class,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration",
                "spring.main.web-application-type=none"
        })
public abstract class EngineTestBase {
    @MockBean
    com.terraforming.ares.repositories.crudRepositories.CrisisRecordEntityRepository mockCrisisRecordEntityRepository;
    @MockBean
    com.terraforming.ares.repositories.crudRepositories.GameEntityRepository mockGameEntityRepository;
    @MockBean
    com.terraforming.ares.repositories.crudRepositories.PlayerEntityRepository mockPlayerEntityRepository;
    @MockBean
    com.terraforming.ares.repositories.crudRepositories.SoloRecordEntityRepository mockSoloRecordEntityRepository;
}
