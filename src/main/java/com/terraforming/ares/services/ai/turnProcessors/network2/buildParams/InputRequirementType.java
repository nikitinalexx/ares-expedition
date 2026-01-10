package com.terraforming.ares.services.ai.turnProcessors.network2.buildParams;

public enum InputRequirementType {
    PHASE_UPGRADE,           // Апгрейд любой фазы
    PHASE_1_UPGRADE,         // Апгрейд фазы 1
    PHASE_2_UPGRADE,         // Апгрейд фазы 2
    PHASE_3_UPGRADE,         // Апгрейд фазы 3
    PHASE_4_UPGRADE,         // Апгрейд фазы 4

    TAG_CHOICE,              // Выбор тега

    MICROBE_INPUT,
    ANIMAL_INPUT,
    SCIENCE_RESOURCE_INPUT
}
