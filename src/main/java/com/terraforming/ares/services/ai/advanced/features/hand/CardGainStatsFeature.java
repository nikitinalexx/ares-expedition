package com.terraforming.ares.services.ai.advanced.features.hand;

import com.terraforming.ares.cards.blue.MatterGenerator;
import com.terraforming.ares.cards.green.BuildingIndustries;
import com.terraforming.ares.cards.green.FuelFactory;
import com.terraforming.ares.cards.green.TropicalResort;
import com.terraforming.ares.cards.red.LocalHeatTrapping;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CardGainStatsFeature implements FeatureBlock {
    @Override
    public int size() {
        return 34;
    }

    public static final List<String> FEATURE_NAMES = List.of(
            // forests
            "forest_sum",
            "forest_max",
            "forest_non_zero",
            "forest_sum_weighted_by_oxygen_left",

            // temperature
            "temperature_sum",
            "temperature_max",
            "temperature_non_zero",
            "temperature_sum_weighted_by_temp_left",
            "temperature_max_weighted_by_temp_left",

            // oxygen
            "oxygen_sum",
            "oxygen_sum_weighted_by_oxygen_left",

            // ocean
            "ocean_sum",
            "ocean_max",
            "ocean_non_zero",
            "ocean_sum_weighted_by_oceans_left",
            "ocean_max_weighted_by_oceans_left",

            // plants positive
            "plant_pos_sum",
            "plant_pos_max",
            "plant_pos_non_zero",

            // plants negative
            "plant_neg_sum",
            "plant_neg_max",
            "plant_neg_non_zero",

            // heat positive
            "heat_pos_sum",
            "heat_pos_max",
            "heat_pos_non_zero",

            // negative heat on build
            "heat_neg_build_sum",
            "heat_neg_build_count",

            // cards
            "card_draw_sum",
            "card_draw_max",
            "card_draw_non_zero",

            // TR positive
            "tr_pos_sum",
            "tr_pos_max",
            "tr_pos_non_zero",

            // TR negative
            "tr_neg_sum"
    );


    private static final Map<Class<?>, Float> NEGATIVE_GAIN_CARDS = Map.of(
            TropicalResort.class, 5f,
            LocalHeatTrapping.class, 3f,
            BuildingIndustries.class, 4f,
            FuelFactory.class, 3f
    );

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        List<Card> hand = ctx.getHand();

        MarsGame game = ctx.getGame();
        Set<Class<?>> handCardClasses = ctx.getHandCardClasses();
        Map<CardAction, Long> handCardActions = ctx.getHandCardActions();

        Stats forest = new Stats();
        Stats temperature = new Stats();
        Stats oxygen = new Stats();
        Stats ocean = new Stats();
        Stats plantPos = new Stats();
        Stats plantNeg = new Stats();
        Stats heatPos = new Stats();
        Stats cardPos = new Stats();
        Stats trPos = new Stats();
        Stats trNeg = new Stats();

        for (Card card : hand) {
            List<Gain> bonuses = card.getCardMetadata().getBonuses();
            if (bonuses == null || bonuses.isEmpty()) continue;

            for (Gain g : bonuses) {
                int v = g.getValue();
                if (v == 0) continue;

                switch (g.getType()) {
                    case FOREST -> {
                        if (v > 0) forest.add(v);
                    }
                    case TEMPERATURE -> {
                        if (v > 0) temperature.add(v);
                    }
                    case OXYGEN -> {
                        if (v > 0) oxygen.add(v);
                    }
                    case OCEAN -> {
                        if (v > 0) ocean.add(v);
                    }
                    case PLANT -> {
                        if (v > 0) plantPos.add(v);
                        else plantNeg.add(-v);
                    }
                    case HEAT -> {
                        if (v > 0) heatPos.add(v);
                    }
                    case CARD -> {
                        if (v > 0) cardPos.add(v);
                    }
                    case TERRAFORMING_RATING -> {
                        if (v > 0) trPos.add(v);
                        else trNeg.add(-v);
                    }
                }
            }
        }

        // bugfixes
        if (handCardActions.containsKey(CardAction.FARMING_COOPS)) {
            plantPos.add(3f);
        }
        if (handCardActions.containsKey(CardAction.LOCAL_HEAT_TRAPPING)) {
            plantPos.add(4f);
        }
        if (handCardClasses.contains(MatterGenerator.class)) {
            cardPos.add(2f);
        }

        float oxygenLeftRatio =
                (float) game.getPlanet().oxygenLeft() / game.getPlanet().oxygenMax();
        float temperatureLeftRatio =
                (float) game.getPlanet().temperatureLeft() / game.getPlanet().temperatureMax();
        float oceansLeftRatio =
                (float) game.getPlanet().oceansLeft() / game.getPlanet().oceansMaxCount();

        // forests
        out.write(forest.sum);
        out.write(forest.max);
        out.write(forest.nonZero);
        out.write(forest.sum * oxygenLeftRatio);

        // temperature
        out.write(temperature.sum);
        out.write(temperature.max);
        out.write(temperature.nonZero);
        out.write(temperature.sum * temperatureLeftRatio);
        out.write(temperature.max * temperatureLeftRatio);

        // oxygen
        out.write(oxygen.sum);
        out.write(oxygen.sum * oxygenLeftRatio);

        // ocean
        out.write(ocean.sum);
        out.write(ocean.max);
        out.write(ocean.nonZero);
        out.write(ocean.sum * oceansLeftRatio);
        out.write(ocean.max * oceansLeftRatio);

        // plants
        out.write(plantPos.sum);
        out.write(plantPos.max);
        out.write(plantPos.nonZero);

        out.write(plantNeg.sum);
        out.write(plantNeg.max);
        out.write(plantNeg.nonZero);

        // heat
        out.write(heatPos.sum);
        out.write(heatPos.max);
        out.write(heatPos.nonZero);

        // negative heat on build
        float negHeatSum = 0f;
        int negHeatCount = 0;
        for (Map.Entry<Class<?>, Float> e : NEGATIVE_GAIN_CARDS.entrySet()) {
            if (handCardClasses.contains(e.getKey())) {
                negHeatSum += e.getValue();
                negHeatCount++;
            }
        }
        out.write(negHeatSum);
        out.write(negHeatCount);

        // cards
        out.write(cardPos.sum);
        out.write(cardPos.max);
        out.write(cardPos.nonZero);

        // TR
        out.write(trPos.sum);
        out.write(trPos.max);
        out.write(trPos.nonZero);

        out.write(trNeg.sum);
    }

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

}
