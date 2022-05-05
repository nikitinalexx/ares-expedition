package com.terraforming.ares.factories;

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
import java.util.Map;

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
        return Planet.builder()
                .measurableGlobalParameters(
                        Map.of(
                                GlobalParameter.TEMPERATURE, MeasurableGlobalParameter.builder()
                                        .currentLevel(0)
                                        .level(ParameterGradation.of(-30, PURPLE))
                                        .level(ParameterGradation.of(-28, PURPLE))
                                        .level(ParameterGradation.of(-26, PURPLE))
                                        .level(ParameterGradation.of(-24, PURPLE))
                                        .level(ParameterGradation.of(-22, PURPLE))
                                        .level(ParameterGradation.of(-20, PURPLE))
                                        .level(ParameterGradation.of(-18, RED))
                                        .level(ParameterGradation.of(-16, RED))
                                        .level(ParameterGradation.of(-14, RED))
                                        .level(ParameterGradation.of(-12, RED))
                                        .level(ParameterGradation.of(-10, RED))
                                        .level(ParameterGradation.of(-8, YELLOW))
                                        .level(ParameterGradation.of(-6, YELLOW))
                                        .level(ParameterGradation.of(-4, YELLOW))
                                        .level(ParameterGradation.of(-2, YELLOW))
                                        .level(ParameterGradation.of(0, YELLOW))
                                        .level(ParameterGradation.of(2, WHITE))
                                        .level(ParameterGradation.of(4, WHITE))
                                        .level(ParameterGradation.of(6, WHITE))
                                        .level(ParameterGradation.of(8, WHITE))
                                        .build(),
                                GlobalParameter.OXYGEN, MeasurableGlobalParameter.builder()
                                        .currentLevel(0)
                                        .level(ParameterGradation.of(0, PURPLE))
                                        .level(ParameterGradation.of(1, PURPLE))
                                        .level(ParameterGradation.of(2, PURPLE))
                                        .level(ParameterGradation.of(3, RED))
                                        .level(ParameterGradation.of(4, RED))
                                        .level(ParameterGradation.of(5, RED))
                                        .level(ParameterGradation.of(6, RED))
                                        .level(ParameterGradation.of(7, YELLOW))
                                        .level(ParameterGradation.of(8, YELLOW))
                                        .level(ParameterGradation.of(9, YELLOW))
                                        .level(ParameterGradation.of(10, YELLOW))
                                        .level(ParameterGradation.of(11, YELLOW))
                                        .level(ParameterGradation.of(12, WHITE))
                                        .level(ParameterGradation.of(13, WHITE))
                                        .level(ParameterGradation.of(14, WHITE))
                                        .build()
                        )
                )
                .oceans(generateOceans())
                .build();
    }

    private List<Ocean> generateOceans() {
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

        shuffleService.shuffle(oceans);

        return oceans;
    }

}
