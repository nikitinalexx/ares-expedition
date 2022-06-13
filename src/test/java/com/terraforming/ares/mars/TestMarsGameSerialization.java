package com.terraforming.ares.mars;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraforming.ares.TerraformingAresApplication;
import com.terraforming.ares.model.awards.*;
import com.terraforming.ares.model.milestones.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
class TestMarsGameSerialization {
    private final ObjectMapper objectMapper = new TerraformingAresApplication().objectMapper();

    @Test
    void testAwards() throws Exception {
        MarsGame marsGame = new MarsGame();

        marsGame.setAwards(List.of(
                new CelebrityAward(),
                new CollectorAward(),
                new GeneratorAward(),
                new IndustrialistAward(),
                new ProjectManagerAward(),
                new ResearcherAward()
        ));

        MarsGame result = serializeDeserialize(marsGame);

        assertEquals(AwardType.CELEBRITY, result.getAwards().get(0).getType());
        assertEquals(AwardType.COLLECTOR, result.getAwards().get(1).getType());
        assertEquals(AwardType.GENERATOR, result.getAwards().get(2).getType());
        assertEquals(AwardType.INDUSTRIALIST, result.getAwards().get(3).getType());
        assertEquals(AwardType.PROJECT_MANAGER, result.getAwards().get(4).getType());
        assertEquals(AwardType.RESEARCHER, result.getAwards().get(5).getType());
    }

    @Test
    void testMilestones() throws Exception {
        MarsGame marsGame = new MarsGame();

        marsGame.setMilestones(List.of(
                new BuilderMilestone(),
                new DiversifierMilestone(),
                new EnergizerMilestone(),
                new FarmerMilestone(),
                new LegendMilestone(),
                new MagnateMilestone(),
                new PlannerMilestone(),
                new SpaceBaronMilestone(),
                new TerraformerMilestone(),
                new TycoonMilestone()
        ));

        MarsGame result = serializeDeserialize(marsGame);

        assertEquals(MilestoneType.BUILDER, result.getMilestones().get(0).getType());
        assertEquals(MilestoneType.DIVERSIFIER, result.getMilestones().get(1).getType());
        assertEquals(MilestoneType.ENERGIZER, result.getMilestones().get(2).getType());
        assertEquals(MilestoneType.FARMER, result.getMilestones().get(3).getType());
        assertEquals(MilestoneType.LEGEND, result.getMilestones().get(4).getType());
        assertEquals(MilestoneType.MAGNATE, result.getMilestones().get(5).getType());
        assertEquals(MilestoneType.PLANNER, result.getMilestones().get(6).getType());
        assertEquals(MilestoneType.SPACE_BARON, result.getMilestones().get(7).getType());
        assertEquals(MilestoneType.TERRAFORMER, result.getMilestones().get(8).getType());
        assertEquals(MilestoneType.TYCOON, result.getMilestones().get(9).getType());
    }

    private MarsGame serializeDeserialize(MarsGame game) throws Exception {
        String json = objectMapper.writeValueAsString(game);
        return objectMapper.readValue(json, MarsGame.class);
    }
}
