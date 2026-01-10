package com.terraforming.ares.services.ai.advanced.features.hand;

import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.Map;

public class PlayedTagsFeature implements FeatureBlock {
    @Override
    public int size() {
        return 66;
    }

    public static final List<String> FEATURE_NAMES = List.of(
            // --- tag counts ---
            "played_tag_space",
            "played_tag_earth",
            "played_tag_event",
            "played_tag_science",
            "played_tag_plant",
            "played_tag_energy",
            "played_tag_building",
            "played_tag_animal",
            "played_tag_jupiter",
            "played_tag_microbe",

            // --- HEAT_EARTH_INCOME ---
            "heat_earth_income_in_hand",
            "heat_earth_income_played",
            "heat_earth_income_hand_value",
            "heat_earth_income_played_value",

            // --- MC_ANIMAL_PLANT_INCOME ---
            "mc_animal_plant_income_in_hand",
            "mc_animal_plant_income_played",
            "mc_animal_plant_income_hand_value",
            "mc_animal_plant_income_played_value",

            // --- PLANT_PLANT_INCOME ---
            "plant_plant_income_in_hand",
            "plant_plant_income_played",
            "plant_plant_income_hand_value",
            "plant_plant_income_played_value",

            // --- CARD_SCIENCE_INCOME ---
            "card_science_income_in_hand",
            "card_science_income_played",
            "card_science_income_hand_value",
            "card_science_income_played_value",

            // --- MC_SCIENCE_INCOME ---
            "mc_science_income_in_hand",
            "mc_science_income_played",
            "mc_science_income_hand_value",
            "mc_science_income_played_value",

            // --- MC_2_BUILDING_INCOME ---
            "mc_building_income_in_hand",
            "mc_building_income_played",
            "mc_building_income_hand_value",
            "mc_building_income_played_value",

            // --- MC_ENERGY_INCOME ---
            "mc_energy_income_in_hand",
            "mc_energy_income_played",
            "mc_energy_income_hand_value",
            "mc_energy_income_played_value",

            // --- MC_SPACE_INCOME ---
            "mc_space_income_in_hand",
            "mc_space_income_played",
            "mc_space_income_hand_value",
            "mc_space_income_played_value",

            // --- HEAT_SPACE_INCOME ---
            "heat_space_income_in_hand",
            "heat_space_income_played",
            "heat_space_income_hand_value",
            "heat_space_income_played_value",

            // --- MC_EVENT_INCOME ---
            "mc_event_income_in_hand",
            "mc_event_income_played",
            "mc_event_income_hand_value",
            "mc_event_income_played_value",

            // --- HEAT_ENERGY_INCOME ---
            "heat_energy_income_in_hand",
            "heat_energy_income_played",
            "heat_energy_income_hand_value",
            "heat_energy_income_played_value",

            // --- PLANT_MICROBE_INCOME ---
            "plant_microbe_income_in_hand",
            "plant_microbe_income_played",
            "plant_microbe_income_hand_value",
            "plant_microbe_income_played_value",

            // --- MC_FOREST_INCOME ---
            "mc_forest_income_in_hand",
            "mc_forest_income_played",
            "mc_forest_income_hand_value",
            "mc_forest_income_played_value",

            // --- MC_EARTH_INCOME ---
            "mc_earth_income_in_hand",
            "mc_earth_income_played",
            "mc_earth_income_hand_value",
            "mc_earth_income_played_value"
    );


    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        Map<Tag, Long> tagCounts = ctx.getPlayedTagToCount();
        Map<CardAction, Long> hand = ctx.getHandCardActions();
        Map<CardAction, Long> played = ctx.getPlayedCardActions();

        Player player = ctx.getPlayer();

        int earth     = get(tagCounts, Tag.EARTH);
        int animal    = get(tagCounts, Tag.ANIMAL);
        int plant     = get(tagCounts, Tag.PLANT);
        int science   = get(tagCounts, Tag.SCIENCE);
        int building  = get(tagCounts, Tag.BUILDING);
        int energy    = get(tagCounts, Tag.ENERGY);
        int space     = get(tagCounts, Tag.SPACE);
        int event     = get(tagCounts, Tag.EVENT);
        int microbe   = get(tagCounts, Tag.MICROBE);
        int jupiter   = get(tagCounts, Tag.JUPITER);
        int forests   = player.getForests();

        out.write(space);
        out.write(earth);
        out.write(event);
        out.write(science);
        out.write(plant);
        out.write(energy);
        out.write(building);
        out.write(animal);
        out.write(jupiter);
        out.write(microbe);

        engine(hand, played, CardAction.HEAT_EARTH_INCOME, earth, out);
        engine(hand, played, CardAction.MC_ANIMAL_PLANT_INCOME, animal + plant, out);
        engine(hand, played, CardAction.PLANT_PLANT_INCOME, plant, out);
        engine(hand, played, CardAction.CARD_SCIENCE_INCOME, science * (1f / 3f), out);
        engine(hand, played, CardAction.MC_SCIENCE_INCOME, science, out);
        engine(hand, played, CardAction.MC_2_BUILDING_INCOME, building * 0.5f, out);
        engine(hand, played, CardAction.MC_ENERGY_INCOME, energy, out);
        engine(hand, played, CardAction.MC_SPACE_INCOME, space, out);
        engine(hand, played, CardAction.HEAT_SPACE_INCOME, space, out);
        engine(hand, played, CardAction.MC_EVENT_INCOME, event, out);
        engine(hand, played, CardAction.HEAT_ENERGY_INCOME, energy, out);
        engine(hand, played, CardAction.PLANT_MICROBE_INCOME, microbe, out);
        engine(hand, played, CardAction.MC_FOREST_INCOME, forests, out);
        engine(hand, played, CardAction.MC_EARTH_INCOME, earth, out);
    }

    private static int get(Map<Tag, Long> map, Tag tag) {
        Long v = map.get(tag);
        return v == null ? 0 : v.intValue();
    }

    private static void engine(
            Map<CardAction, Long> hand,
            Map<CardAction, Long> played,
            CardAction action,
            float value,
            FeatureWriter out
    ) {
        boolean inHand   = hand.get(action)   != null;
        boolean isPlayed = played.get(action) != null;

        out.write(inHand ? 1 : 0);
        out.write(isPlayed ? 1 : 0);
        out.write(inHand   ? hand.get(action) * value : 0f);
        out.write(isPlayed ? played.get(action) * value : 0f);
    }

}
