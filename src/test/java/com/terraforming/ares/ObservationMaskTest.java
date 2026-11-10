package com.terraforming.ares;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.agent.ObservationBuilder;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The columns the v3 observation drops must be EXACT copies of columns it keeps - checked on the states of real games, not on a sample file.
 * If the V2 encoder changes (a column stops being a copy) this test fails and the mask must be regenerated with ml/audit_features.py.
 */
class ObservationMaskTest extends EngineTestBase {
    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;
    @Autowired
    AdvancedAiDataCollectionService baseEncoder;

    private JsonNode mask() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/observation/v3_mask.json")) {
            return new ObjectMapper().readTree(in);
        }
    }

    @Test
    void theMaskIsConsistent() throws IOException {
        JsonNode mask = mask();
        int base = AiConstants.TOTAL_SIZE;
        assertEquals(base, mask.get("baseSize").asInt());
        Set<Integer> keep = new HashSet<>();
        mask.get("keep").forEach(n -> keep.add(n.asInt()));
        Set<Integer> dropped = new HashSet<>();
        for (JsonNode pair : mask.get("drop")) {
            assertTrue(dropped.add(pair.get(0).asInt()), "dropped twice: " + pair);
            assertTrue(keep.contains(pair.get(1).asInt()), "a column is replaced by a column that is dropped itself: " + pair);
        }
        assertEquals(base, keep.size() + dropped.size(), "every column is either kept or dropped");
        keep.retainAll(dropped);
        assertTrue(keep.isEmpty());
        assertEquals(mask.get("keep").size() + ObservationBuilder.CONTEXT_SIZE + ObservationBuilder.CARDS_SIZE + ObservationBuilder.EXTRAS_SIZE, ObservationBuilder.size());
    }

    @Test
    void everyValueHasAUniqueName() {
        List<String> names = ObservationBuilder.names();
        assertEquals(ObservationBuilder.size(), names.size());
        Set<String> seen = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (String name : names) {
            if (!seen.add(name)) {
                duplicates.add(name);
            }
        }
        assertTrue(duplicates.isEmpty(), "two values with the same name: " + duplicates);
    }

    @Test
    void droppedColumnsAreExactCopiesOnRealStates() throws IOException {
        JsonNode mask = mask();
        int[] checked = {0};
        List<String> broken = new ArrayList<>();
        for (int g = 0; g < 25; g++) {
            MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                    .playerNames(new ArrayList<>(List.of("A", "B")))
                    .computers(List.of(PlayerDifficulty.SMART, g % 2 == 0 ? PlayerDifficulty.SMART : PlayerDifficulty.RANDOM))
                    .mulligan(true)
                    .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                    .dummyHand(true).build());
            int[] step = {0};
            simulation.processSimulation(game, current -> {
                if (step[0]++ % 4 != 0) {
                    return;
                }
                for (Player player : current.getPlayerUuidToPlayer().values()) {
                    float[] raw = baseEncoder.collectData(current, player.getUuid());
                    for (JsonNode pair : mask.get("drop")) {
                        int dropped = pair.get(0).asInt();
                        int kept = pair.get(1).asInt();
                        if (raw[dropped] != raw[kept] && broken.size() < 10) {
                            broken.add("column " + dropped + " = " + raw[dropped] + " but its original " + kept + " = " + raw[kept]);
                        }
                    }
                    checked[0]++;
                }
            });
        }
        assertTrue(checked[0] > 500, "states checked: " + checked[0]);
        assertTrue(broken.isEmpty(), "columns that are not copies any more (regenerate the mask): " + broken);
    }
}
