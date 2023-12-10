package com.terraforming.ares.factories;

import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.awards.*;
import com.terraforming.ares.model.milestones.*;
import com.terraforming.ares.services.ShuffleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
@Service
@RequiredArgsConstructor
public class AchievementsFactory {
    private final ShuffleService shuffleService;


    public List<BaseAward> createAwards(List<Expansion> expansions, int count) {
        List<BaseAward> awards = new ArrayList<>(List.of(
                new CelebrityAward(),
                new CollectorAward(),
                new GeneratorAward(),
                new IndustrialistAward(),
                new ProjectManagerAward(),
                new ResearcherAward()
        ));

        if (expansions.contains(Expansion.EXPERIMENTAL)) {
            awards.add(new BotanistAward());
            awards.add(new FloraHarvestAward());
            awards.add(new BuilderAward());
            awards.add(new GardenerAward());
            awards.add(new CriterionAward());
        }

        shuffleService.shuffle(awards);

        return awards.subList(0, count);
    }

    public List<Milestone> createMilestones(List<Expansion> expansions, int count) {
        List<Milestone> milestones = new ArrayList<>(List.of(
                new BuilderMilestone(),
                new DiversifierMilestone(),
                new EnergizerMilestone(),
                new FarmerMilestone(),
                new LegendMilestone(),
                new MagnateMilestone(),
                new PlannerMilestone(),
                new SpaceBaronMilestone(),
                new TerraformerMilestone(),
                new TycoonMilestone(),
                new GardenerMilestone()
        ));

        if (expansions.contains(Expansion.EXPERIMENTAL)) {
            milestones.add(new EcologistMilestone());
            milestones.add(new MinimalistMilestone());
            milestones.add(new TerranMilestone());
            milestones.add(new TriadMasteryMilestone());
        }

        shuffleService.shuffle(milestones);

        return milestones.subList(0, count);
    }

}
