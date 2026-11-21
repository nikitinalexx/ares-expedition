package com.terraforming.ares.services.ai.dl4j;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Why: a sweep over the `advanced_ai` weight files must not depend on copying files over the committed model - the exact mistake that hid the wrong weights in
 * the working tree (same size, same timestamp, git reported it clean). The property makes the choice explicit and testable.
 */
class NNServiceModelTest {

    @Test
    void theCommittedModelIsTheDefault() {
        System.clearProperty("ares.nn.model");
        assertEquals(NNService.DEFAULT_MODEL, NNService.modelName());
    }

    @Test
    void thePropertySelectsAnotherModel() {
        System.setProperty("ares.nn.model", "D:/ares/models/advanced_ai/iter3_epoch_4_30.zip");
        try {
            assertEquals("D:/ares/models/advanced_ai/iter3_epoch_4_30.zip", NNService.modelName());
        } finally {
            System.clearProperty("ares.nn.model");
        }
    }
}
