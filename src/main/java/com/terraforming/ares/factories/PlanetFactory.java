package com.terraforming.ares.factories;

import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.GlobalParameter;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.parameters.MeasurableGlobalParameter;
import com.terraforming.ares.model.parameters.Ocean;
import com.terraforming.ares.model.parameters.ParameterGradation;
import com.terraforming.ares.services.ShuffleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.terraforming.ares.model.parameters.ParameterColor.*;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class PlanetFactory {
    private final ShuffleService shuffleService;

    public Planet createMars(GameParameters gameParameters) {
        Planet.PlanetBuilder builder = Planet.builder();
        builder
                .measurableGlobalParameter(
                        GlobalParameter.TEMPERATURE, MeasurableGlobalParameter.builder()
                                .currentLevel(gameParameters.isCrysis() ? 19 : 0)
                                .level(ParameterGradation.of(-30, P))
                                .level(ParameterGradation.of(-28, P))
                                .level(ParameterGradation.of(-26, P))
                                .level(ParameterGradation.of(-24, P))
                                .level(ParameterGradation.of(-22, P))
                                .level(ParameterGradation.of(-20, P))
                                .level(ParameterGradation.of(-18, R))
                                .level(ParameterGradation.of(-16, R))
                                .level(ParameterGradation.of(-14, R))
                                .level(ParameterGradation.of(-12, R))
                                .level(ParameterGradation.of(-10, R))
                                .level(ParameterGradation.of(-8, Y))
                                .level(ParameterGradation.of(-6, Y))
                                .level(ParameterGradation.of(-4, Y))
                                .level(ParameterGradation.of(-2, Y))
                                .level(ParameterGradation.of(0, Y))
                                .level(ParameterGradation.of(2, W))
                                .level(ParameterGradation.of(4, W))
                                .level(ParameterGradation.of(6, W))
                                .level(ParameterGradation.of(8, W))
                                .build()
                )
                .measurableGlobalParameter(
                        GlobalParameter.OXYGEN, MeasurableGlobalParameter.builder()
                                .currentLevel(gameParameters.isCrysis() ? 14 : 0)
                                .level(ParameterGradation.of(0, P))
                                .level(ParameterGradation.of(1, P))
                                .level(ParameterGradation.of(2, P))
                                .level(ParameterGradation.of(3, R))
                                .level(ParameterGradation.of(4, R))
                                .level(ParameterGradation.of(5, R))
                                .level(ParameterGradation.of(6, R))
                                .level(ParameterGradation.of(7, Y))
                                .level(ParameterGradation.of(8, Y))
                                .level(ParameterGradation.of(9, Y))
                                .level(ParameterGradation.of(10, Y))
                                .level(ParameterGradation.of(11, Y))
                                .level(ParameterGradation.of(12, W))
                                .level(ParameterGradation.of(13, W))
                                .level(ParameterGradation.of(14, W))
                                .build()
                )
                .oceans(generateOceans(gameParameters));

        if (gameParameters.getExpansions().contains(Expansion.INFRASTRUCTURE)) {
            builder.measurableGlobalParameter(
                    GlobalParameter.INFRASTRUCTURE, MeasurableGlobalParameter.builder()
                            .currentLevel(0)
                            .level(ParameterGradation.of(0, P))
                            .level(ParameterGradation.of(7, P))
                            .level(ParameterGradation.of(14, P))

                            .level(ParameterGradation.of(21, R))
                            .level(ParameterGradation.of(28, R))
                            .level(ParameterGradation.of(35, R))
                            .level(ParameterGradation.of(42, R))
                            .level(ParameterGradation.of(49, R))

                            .level(ParameterGradation.of(56, Y))
                            .level(ParameterGradation.of(63, Y))
                            .level(ParameterGradation.of(70, Y))
                            .level(ParameterGradation.of(77, Y))

                            .level(ParameterGradation.of(85, W))
                            .level(ParameterGradation.of(92, W))
                            .level(ParameterGradation.of(100, W))
                            .build()
            );
        }

        return builder.build();
    }

    private List<Ocean> generateOceans(GameParameters gameParameters) {
        List<Ocean> oceans = new ArrayList<>(Arrays.asList(
                new Ocean(0, 0, 2),
                new Ocean(0, 4, 0),
                new Ocean(1, 1, 0),
                new Ocean(0, 2, 1),
                new Ocean(1, 0, 1),
                new Ocean(1, 0, 0),
                new Ocean(0, 1, 1),
                new Ocean(1, 0, 0),
                new Ocean(0, 0, 2)
        ));

        if (gameParameters.isCrysis()) {
            oceans.forEach(Ocean::reveal);
        }

        shuffleService.shuffle(oceans);

        return oceans;
    }

}
