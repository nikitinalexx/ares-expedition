package com.terraforming.ares.selfplay;

import com.terraforming.ares.repositories.crudRepositories.CrisisRecordEntityRepository;
import com.terraforming.ares.repositories.crudRepositories.GameEntityRepository;
import com.terraforming.ares.repositories.crudRepositories.PlayerEntityRepository;
import com.terraforming.ares.repositories.crudRepositories.SoloRecordEntityRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.lang.reflect.Proxy;

/**
 * The headless self-play process does not need a database. The profile {@code selfplay} replaces the Spring Data repositories
 * with stubs that FAIL LOUDLY when used, so that nobody can accidentally depend on the database in a night run.
 */
@Configuration
@Profile("selfplay")
public class SelfPlayStubRepositories {
    @Bean
    GameEntityRepository gameEntityRepository() {
        return stub(GameEntityRepository.class);
    }

    @Bean
    PlayerEntityRepository playerEntityRepository() {
        return stub(PlayerEntityRepository.class);
    }

    @Bean
    CrisisRecordEntityRepository crisisRecordEntityRepository() {
        return stub(CrisisRecordEntityRepository.class);
    }

    @Bean
    SoloRecordEntityRepository soloRecordEntityRepository() {
        return stub(SoloRecordEntityRepository.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "toString":
                    return type.getSimpleName() + " (self-play stub)";
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                default:
                    if (method.getName().startsWith("clear") && method.getName().endsWith("Memory")
                            && method.getReturnType() == void.class) {
                        return null;  // the scheduled memory cleanup of GameProcessorService: nothing to clear without a database
                    }
                    throw new UnsupportedOperationException(type.getSimpleName() + "." + method.getName()
                            + " was called in the self-play process, which has no database");
            }
        });
    }
}
