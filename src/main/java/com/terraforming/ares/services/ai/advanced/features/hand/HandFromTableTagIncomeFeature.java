package com.terraforming.ares.services.ai.advanced.features.hand;

import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.Map;

public class HandFromTableTagIncomeFeature implements FeatureBlock {
    public static final List<String> FEATURE_NAMES = List.of(
            "HEAT_EARTH_INCOME_from_table",
            "MC_ANIMAL_PLANT_INCOME_from_table",
            "CARD_SCIENCE_INCOME_from_table",
            "MC_EARTH_INCOME_from_table",
            "PLANT_PLANT_INCOME_from_table",
            "MC_SCIENCE_INCOME_from_table",
            "MC_2_BUILDING_INCOME_from_table",
            "MC_ENERGY_INCOME_from_table",
            "MC_SPACE_INCOME_from_table",
            "HEAT_SPACE_INCOME_from_table",
            "MC_EVENT_INCOME_from_table",
            "HEAT_ENERGY_INCOME_from_table",
            "PLANT_MICROBE_INCOME_from_table",
            "MC_FOREST_INCOME_from_table"
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
        Map<CardAction, Long> playedCardActions = ctx.getPlayedCardActions();


        out.write(playedCardActions.getOrDefault(CardAction.HEAT_EARTH_INCOME, 0L));
        out.write(playedCardActions.getOrDefault(CardAction.MC_ANIMAL_PLANT_INCOME, 0L));
        out.write(playedCardActions.getOrDefault(CardAction.CARD_SCIENCE_INCOME, 0L));
        out.write(playedCardActions.getOrDefault(CardAction.MC_EARTH_INCOME, 0L));
        out.write(playedCardActions.getOrDefault(CardAction.PLANT_PLANT_INCOME, 0L));
        out.write(playedCardActions.getOrDefault(CardAction.MC_SCIENCE_INCOME, 0L));
        out.write(playedCardActions.getOrDefault(CardAction.MC_2_BUILDING_INCOME, 0L));
        out.write(playedCardActions.getOrDefault(CardAction.MC_ENERGY_INCOME, 0L));
        out.write(playedCardActions.getOrDefault(CardAction.MC_SPACE_INCOME, 0L));
        out.write(playedCardActions.getOrDefault(CardAction.HEAT_SPACE_INCOME, 0L));
        out.write(playedCardActions.getOrDefault(CardAction.MC_EVENT_INCOME, 0L));
        out.write(playedCardActions.getOrDefault(CardAction.HEAT_ENERGY_INCOME, 0L));
        out.write(playedCardActions.getOrDefault(CardAction.PLANT_MICROBE_INCOME, 0L));
        out.write(playedCardActions.getOrDefault(CardAction.MC_FOREST_INCOME, 0L));
    }
}
