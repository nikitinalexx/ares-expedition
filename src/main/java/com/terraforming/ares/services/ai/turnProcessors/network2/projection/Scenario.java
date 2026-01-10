package com.terraforming.ares.services.ai.turnProcessors.network2.projection;

import com.terraforming.ares.services.ai.turnProcessors.network2.dto.CardWithChanceAndInput;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scenario {
    // Храним только ПЕРВОЕ действие цепочки, которое мы реально выполним
    private ActionType firstStepType;
    private CardWithChanceAndInput firstStepCardData;

    // Описание для логов (чтобы понимать, что бот "задумал")
    private final List<String> stepsDescription = new ArrayList<>();

    private float[] finalStateData;
    private double finalChance;

    public enum ActionType { BUILD, UNMI, EXTRA_BONUS, SKIP }
}
