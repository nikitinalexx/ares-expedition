package com.terraforming.ares.services.ai.advanced.features.table;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.Map;

public class PhaseUpgradeFeature implements FeatureBlock {

    private static final List<String> FEATURE_NAMES = List.of(
            "table_phase_1_app_1",
            "table_phase_1_app_2",

            "table_phase_2_app_1",
            "table_phase_2_app_2",

            "table_phase_3_app_1",
            "table_phase_3_app_2",

            "table_phase_4_app_1",
            "table_phase_4_app_2",

            "table_phase_5_app_1",
            "table_phase_5_app_2",

            "max_mc_per_card",
            "max_heat_per_card",
            "max_plants_per_card",
            "max_cards_per_card"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public int size() {
        return 14;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        Player player = ctx.getPlayer();
        Map<CardAction, Long> played = ctx.getPlayedCardActions();


        for (int i = 0; i < 5; i++) {
            out.write(player.getPhaseCards().get(i) == 1 ? 1 : 0);
            out.write(player.getPhaseCards().get(i) == 2 ? 1 : 0);
        }

        float maxMc = 0;
        float maxHeat = 0;
        float maxPlants = 0;
        float maxCards = 0;

        for (Card playedCard : ctx.getPlayedCards()) {
            List<Gain> incomes = playedCard.getCardMetadata().getIncomes();
            for (Gain income : incomes) {
                switch (income.getType()) {
                    case MC:
                        maxMc = Math.max(maxMc, income.getValue());
                        break;
                    case HEAT:
                        maxHeat = Math.max(maxHeat, income.getValue());
                        break;
                    case PLANT:
                        maxPlants = Math.max(maxPlants, income.getValue());
                        break;
                    case CARD:
                        maxCards = Math.max(maxCards, income.getValue());
                        break;
                }
            }
        }

        int[] tagCounts = ctx.getPlayedTagToCount();

        int earth = tagCounts[Tag.EARTH.ordinal()];
        int animal = tagCounts[Tag.ANIMAL.ordinal()];
        int plant = tagCounts[Tag.PLANT.ordinal()];
        int science = tagCounts[Tag.SCIENCE.ordinal()];
        int building = tagCounts[Tag.BUILDING.ordinal()];
        int energy = tagCounts[Tag.ENERGY.ordinal()];
        int space = tagCounts[Tag.SPACE.ordinal()];
        int event = tagCounts[Tag.EVENT.ordinal()];
        int microbe = tagCounts[Tag.MICROBE.ordinal()];
        int forests = player.getForests();

        maxMc = Math.max(maxMc, engine(played, CardAction.MC_ANIMAL_PLANT_INCOME, animal + plant));
        maxMc = Math.max(maxMc, engine(played, CardAction.MC_SCIENCE_INCOME, science));
        maxMc = Math.max(maxMc, engine(played, CardAction.MC_2_BUILDING_INCOME, building * 0.5f));
        maxMc = Math.max(maxMc, engine(played, CardAction.MC_ENERGY_INCOME, energy));
        maxMc = Math.max(maxMc, engine(played, CardAction.MC_SPACE_INCOME, space));
        maxMc = Math.max(maxMc, engine(played, CardAction.MC_EVENT_INCOME, event));
        maxMc = Math.max(maxMc, engine(played, CardAction.MC_FOREST_INCOME, forests));
        maxMc = Math.max(maxMc, engine(played, CardAction.MC_EARTH_INCOME, earth));


        maxHeat = Math.max(maxHeat, engine(played, CardAction.HEAT_EARTH_INCOME, earth));
        maxHeat = Math.max(maxHeat, engine(played, CardAction.HEAT_SPACE_INCOME, space));
        maxHeat = Math.max(maxHeat, engine(played, CardAction.HEAT_ENERGY_INCOME, energy));

        maxPlants = Math.max(maxPlants, engine(played, CardAction.PLANT_PLANT_INCOME, plant));
        maxPlants = Math.max(maxPlants, engine(played, CardAction.PLANT_MICROBE_INCOME, microbe));

        maxCards = Math.max(maxCards, engine(played, CardAction.CARD_SCIENCE_INCOME, science * (1f / 3f)));

        out.write(maxMc);
        out.write(maxHeat);
        out.write(maxPlants);
        out.write(maxCards);
    }

    private static float engine(
            Map<CardAction, Long> played,
            CardAction action,
            float value
    ) {
        boolean isPlayed = played.get(action) != null;

        if (isPlayed) {
            return played.get(action) * value;
        }
        return 0f;
    }

}
