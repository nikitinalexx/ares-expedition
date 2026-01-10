package com.terraforming.ares.services.ai.advanced.features.hand;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.buffedCorporations.BuffedArclightCorporation;
import com.terraforming.ares.cards.corporations.ArclightCorporation;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.terraforming.ares.model.parameters.ParameterColor.*;

public class MicrobeFeature implements FeatureBlock {
    @Override
    public int size() {
        return 51;
    }

    public static final List<String> FEATURE_NAMES = List.of(
            // --- Plant action output ---
            "table_plant_action_output",
            "hand_plant_action_output",

            // --- VP per resource sensitivity ---
            "vp_resource_sensitivity_hand",
            "vp_resource_sensitivity_table",

            // --- Producing / Consuming microbes (synergy) ---
            "played_producing_microbe_cards",
            "played_consuming_microbe_cards",
            "hand_producing_microbe_cards",
            "hand_consuming_microbe_cards",

            // --- Direct microbe producers ---
            "played_direct_microbe_producers",
            "hand_direct_microbe_producers",

            // --- Filter Feeders ---
            "filter_feeders_played",
            "filter_feeders_hand",
            "filter_feeders_hand_can_build",
            "filter_feeders_consuming_microbes_hand",
            "filter_feeders_consuming_microbes_played",

            // --- Extreme Cold Fungus ---
            "ecf_hand",
            "ecf_played",
            "ecf_temperature_utility_window",
            "ecf_consuming_microbes_hand",
            "ecf_consuming_microbes_played",

            // --- Decomposing Fungus ---
            "df_hand",
            "df_played",
            "df_producing_microbes_hand",
            "df_producing_microbes_played",

            // --- Conserved Biome ---
            "cb_hand",
            "cb_played",
            "cb_consuming_microbes_hand",
            "cb_consuming_microbes_played",
            "cb_vp_animal_cards_hand",
            "cb_best_animal_vp_played",
            "cb_forests_if_in_hand",

            // --- Symbiotic Fungus ---
            "sf_hand",
            "sf_played",
            "sf_temperature_availability",
            "sf_consuming_microbes_hand",
            "sf_consuming_microbes_played",

            // --- Viral Enhancers ---
            "viral_enhancers_hand",
            "viral_enhancers_played",
            "viral_enhancers_card_power",
            "viral_enhancers_has_consuming_microbes",
            "viral_enhancers_has_animal_vp",

            // --- Orbital Outpost ---
            "orbital_outpost_hand",
            "orbital_outpost_played",
            "orbital_outpost_low_tag_cards_in_hand",

            // --- Ecological Zone ---
            "ecological_zone_hand",
            "ecological_zone_played",
            "ecological_zone_plant_plus_animal_tags_hand",

            // --- Bacterial Aggregates ---
            "bacterial_aggregates_hand",
            "bacterial_aggregates_played",
            "bacterial_aggregates_resources_on_table",
            "bacterial_aggregates_earth_tags_hand"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    private static final Map<Class<?>, Float> CARDS_WITH_VP_PER_RESOURCE =
            Map.ofEntries(
                    Map.entry(ArclightCorporation.class, 0.5f),
                    Map.entry(BuffedArclightCorporation.class, 0.5f),
                    Map.entry(Birds.class, 1f),
                    Map.entry(EcologicalZone.class, 0.5f),
                    Map.entry(FilterFeeders.class, 0.33f),
                    Map.entry(BuffedFilterFeeders.class, 0.5f),
                    Map.entry(Fish.class, 1f),
                    Map.entry(Herbivores.class, 0.5f),
                    Map.entry(Livestock.class, 1f),
                    Map.entry(SmallAnimals.class, 0.5f),
                    Map.entry(Tardigrades.class, 0.33f),
                    Map.entry(PhysicsComplex.class, 0.5f),
                    Map.entry(Zoos.class, 1f)
            );

    private static final Map<Class<?>, Float> ANIMAL_CARDS_VP_PER_RESOURCE =
            Map.ofEntries(
                    Map.entry(ArclightCorporation.class, 0.5f),
                    Map.entry(BuffedArclightCorporation.class, 0.5f),
                    Map.entry(Birds.class, 1f),
                    Map.entry(EcologicalZone.class, 0.5f),
                    Map.entry(FilterFeeders.class, 0.33f),
                    Map.entry(BuffedFilterFeeders.class, 0.5f),
                    Map.entry(Fish.class, 1f),
                    Map.entry(Herbivores.class, 0.5f),
                    Map.entry(Livestock.class, 1f),
                    Map.entry(SmallAnimals.class, 0.5f),
                    Map.entry(Zoos.class, 1f)
            );

    private float vpFromBestAnimalCard(Set<Class<?>> cardClasses) {//TODO IS THIS STRATEGY?
        if (cardClasses.contains(Birds.class) || cardClasses.contains(Fish.class) || cardClasses.contains(Livestock.class) || cardClasses.contains(Zoos.class)) {
            return 1f;
        }

        if (cardClasses.contains(ArclightCorporation.class) || cardClasses.contains(BuffedArclightCorporation.class)
                || cardClasses.contains(EcologicalZone.class)
                || cardClasses.contains(Herbivores.class)
                || cardClasses.contains(SmallAnimals.class)) {
            return 0.5f;
        }

        if (cardClasses.contains(FilterFeeders.class)) {
            return 0.33f;
        }
        return 0;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        List<Card> hand = ctx.getHand();

        Map<CardAction, Long> playedCardActions = ctx.getPlayedCardActions();
        Map<CardAction, Long> handCardActions = ctx.getHandCardActions();
        Set<Class<?>> handCardClasses = ctx.getHandCardClasses();
        Set<Class<?>> playedCardClasses = ctx.getPlayedCardClasses();

        Player player = ctx.getPlayer();
        Planet planet = ctx.getGame().getPlanet();
        boolean canAmplifyOxygenOrTemperature = ctx.isCanAmplifyOxygenOrTemperature();
        int[] handTagCounts = ctx.getHandTagCounts();


        float tablePlantActionOutputRough = getTotalPlantActionOutput(playedCardActions);
        float handPlantActionOutputRough = getTotalPlantActionOutput(handCardActions);

        out.write(tablePlantActionOutputRough);
        out.write(handPlantActionOutputRough);

        //summarize all cards that give VP per microbe/animal entity
        float vpResourceSensitivityInHand = (float) CARDS_WITH_VP_PER_RESOURCE.entrySet().stream()
                .filter(e -> handCardClasses.contains(e.getKey()))
                .mapToDouble(Map.Entry::getValue)
                .sum();

        float vpResourceSensitivityOnTable = (float) CARDS_WITH_VP_PER_RESOURCE.entrySet().stream()
                .filter(e -> playedCardClasses.contains(e.getKey()))
                .mapToDouble(Map.Entry::getValue)
                .sum();

        out.write(vpResourceSensitivityInHand);
        out.write(vpResourceSensitivityOnTable);

        float[] producingConsumingMicrobes = handleProducingConsumingMicrobeSynergy(ctx);//playedProducingMicrobeCards, playedConsumingMicrobeCards, handProducingMicrobeCards, handConsumingMicrobeCards
        float playedProducingMicrobeCards = producingConsumingMicrobes[0];
        float playedConsumingMicrobeCards = producingConsumingMicrobes[1];
        float handProducingMicrobeCards = producingConsumingMicrobes[2];
        float handConsumingMicrobeCards = producingConsumingMicrobes[3];

        out.write(playedProducingMicrobeCards);
        out.write(playedConsumingMicrobeCards);
        out.write(handProducingMicrobeCards);
        out.write(handConsumingMicrobeCards);

        out.write(directlyProducingMicrobeCardsCount(playedCardActions));
        out.write(directlyProducingMicrobeCardsCount(handCardActions));

        {
            boolean filterFeedersInHand = handCardActions.containsKey(CardAction.FILTER_FEEDERS);
            boolean filterFeedersPlayed = playedCardActions.containsKey(CardAction.FILTER_FEEDERS);

            out.write((filterFeedersPlayed ? 1 : 0));
            out.write((filterFeedersInHand ? 1 : 0));

            boolean canBuild = planet.getRevealedOceans().size() >= 2;
            out.write((filterFeedersInHand && canBuild ? 1 : 0));
            out.write((filterFeedersPlayed || filterFeedersInHand) ? handConsumingMicrobeCards : 0);
            out.write((filterFeedersPlayed || filterFeedersInHand) ? playedConsumingMicrobeCards : 0);
        }

        {
            boolean handECF = handCardClasses.contains(ExtremeColdFungus.class);
            boolean playedECF = playedCardClasses.contains(ExtremeColdFungus.class);

            out.write(handECF ? 1 : 0);
            out.write(playedECF ? 1 : 0);

            int currentTemp = planet.getCurrentLevel(GlobalParameter.TEMPERATURE);
            int maxTemp = ctx.getTemperatureStepFromColor(canAmplifyOxygenOrTemperature ? ParameterColor.R : ParameterColor.P, true);

            float utilityWindow = currentTemp <= maxTemp ? (float) (maxTemp - currentTemp + 1) : 0;
            out.write((handECF || playedECF) ? utilityWindow : 0);

            out.write((handECF || playedECF) ? handConsumingMicrobeCards : 0);
            out.write((handECF || playedECF) ? playedConsumingMicrobeCards : 0);
        }

        {
            boolean handDF = handCardClasses.contains(DecomposingFungus.class);
            boolean playedDF = playedCardClasses.contains(ExtremeColdFungus.class);

            out.write(handDF ? 1 : 0);
            out.write(playedDF ? 1 : 0);

            out.write((handDF || playedDF) ? handProducingMicrobeCards : 0);
            out.write((handDF || playedDF) ? playedProducingMicrobeCards : 0);
        }

        {
            boolean handCB = handCardClasses.contains(ConservedBiome.class);
            boolean playedCB = playedCardClasses.contains(ConservedBiome.class);

            out.write(handCB ? 1 : 0);
            out.write(playedCB ? 1 : 0);

            out.write((handCB || playedCB) ? handConsumingMicrobeCards : 0);
            out.write((handCB || playedCB) ? playedConsumingMicrobeCards : 0);

            out.write((handCB || playedCB) ? countVpPerAnimalCards(handCardClasses) : 0);
            out.write((handCB || playedCB) ? vpFromBestAnimalCard(playedCardClasses) : 0);

            out.write(handCB ? player.getForests() : 0);
        }

        {
            boolean handSF = handCardClasses.contains(SymbioticFungus.class);
            boolean playedSF = playedCardClasses.contains(SymbioticFungus.class);

            out.write(handSF ? 1 : 0);
            out.write(playedSF ? 1 : 0);

            int currentTemp = planet.getCurrentLevel(GlobalParameter.TEMPERATURE);
            int minTemp = ctx.getTemperatureStepFromColor(canAmplifyOxygenOrTemperature ? ParameterColor.P : ParameterColor.R, false); // Минимум для старта
            float availability = currentTemp >= minTemp ? 1.0f : (float) currentTemp / minTemp;
            out.write(handSF ? availability : 0);

            out.write((handSF || playedSF) ? handConsumingMicrobeCards : 0);
            out.write((handSF || playedSF) ? playedConsumingMicrobeCards : 0);
        }

        {
            boolean handViralEnhancers = handCardClasses.contains(ViralEnhancers.class);
            boolean playedViralEnhancers = playedCardClasses.contains(ViralEnhancers.class);

            out.write(handViralEnhancers ? 1 : 0);
            out.write(playedViralEnhancers ? 1 : 0);

            int cardPower = handTagCounts[Tag.PLANT.ordinal()] + handTagCounts[Tag.MICROBE.ordinal()] + handTagCounts[Tag.ANIMAL.ordinal()];

            out.write(handViralEnhancers || playedViralEnhancers ? cardPower : 0);
            out.write(handViralEnhancers || playedViralEnhancers ? ((playedConsumingMicrobeCards + handConsumingMicrobeCards) > 0 ? 1 : 0) : 0);
            out.write(handViralEnhancers || playedViralEnhancers ? ((vpFromBestAnimalCard(playedCardClasses) > 0 || vpFromBestAnimalCard(handCardClasses) > 0) ? 1 : 0) : 0 );
        }


        {
            boolean handOutpost = handCardActions.containsKey(CardAction.ORBITAL_OUTPOST);
            boolean playedOO = playedCardActions.containsKey(CardAction.ORBITAL_OUTPOST);

            out.write(handOutpost ? 1 : 0);
            out.write(playedOO ? 1 : 0);

            out.write((handOutpost || playedOO) ? hand.stream().filter(card -> card.getTags().size() <= 1).count() : 0);
        }


        {
            boolean handEZ = handCardActions.containsKey(CardAction.ECOLOGICAL_ZONE);
            boolean tableEZ = playedCardActions.containsKey(CardAction.ECOLOGICAL_ZONE);

            out.write(handEZ ? 1 : 0);
            out.write(tableEZ ? 1 : 0);

            out.write((handEZ || tableEZ) ? handTagCounts[Tag.ANIMAL.ordinal()] + handTagCounts[Tag.PLANT.ordinal()] : 0);
        }

        {
            boolean handBA = handCardActions.containsKey(CardAction.BACTERIAL_AGGREGATES);
            boolean tableBA = playedCardActions.containsKey(CardAction.BACTERIAL_AGGREGATES);

            out.write(handBA ? 1 : 0);
            out.write(tableBA ? 1 : 0);

            out.write(tableBA ? player.getCardResourcesCount().get(BacterialAggregates.class) : 0);
            out.write((handBA || tableBA) ? handTagCounts[Tag.EARTH.ordinal()] : 0);
        }
    }

    private float[] handleProducingConsumingMicrobeSynergy(TableContext ctx) {
        Planet planet = ctx.getGame().getPlanet();
        Player player = ctx.getPlayer();

        List<Card> hand = ctx.getHand();
        List<Card> playedCards = ctx.getPlayedCards();
        
        Set<Class<?>> playedCardClasses = ctx.getPlayedCardClasses();
        Set<Class<?>> handCardClasses = ctx.getHandCardClasses();
        
        boolean canAmplifyOxygenOrTemperature = ctx.isCanAmplifyOxygenOrTemperature();

        boolean temperatureMax = planet.isTemperatureMax();
        boolean oxygenMax = planet.isOxygenMax();
        boolean oceansMax = planet.isOceansMax();

        long playedProducingMicrobeCards = playedCards.stream().filter(Card::producesMicrobe).count();
        long playedConsumingMicrobeCards = playedCards.stream().filter(Card::consumesMicrobe).count();

        long handProducingMicrobeCards = hand.stream().filter(Card::producesMicrobe).count();
        long handConsumingMicrobeCards = hand.stream().filter(Card::consumesMicrobe).count();

        if (playedCardClasses.contains(BacterialAggregates.class) && player.getCardResourcesCount().getOrDefault(BacterialAggregates.class, 0) >= 5) {
            playedConsumingMicrobeCards--;
        }

        if (handCardClasses.contains(ExtremeColdFungus.class) || handCardClasses.contains(BuffedExtremeColdFungus.class)) {
            ParameterColor temperatureColor = planet.getTemperatureColor();
            if (temperatureColor == Y || temperatureColor == W || temperatureColor == R && !canAmplifyOxygenOrTemperature) {
                handProducingMicrobeCards--;//we can never ever build these cards
            }
        }

        if (temperatureMax) {//cards can never consume
            if (handCardClasses.contains(GhgProductionBacteria.class) || handCardClasses.contains(BuffedGhgProductionBacteria.class)) {
                handConsumingMicrobeCards--;
            }
            if (playedCardClasses.contains(GhgProductionBacteria.class) || playedCardClasses.contains(BuffedGhgProductionBacteria.class)) {
                playedConsumingMicrobeCards--;
            }
        }

        if (oxygenMax) {//cards can never consume
            if (handCardClasses.contains(RegolithEaters.class)) {
                handConsumingMicrobeCards--;
            }
            if (playedCardClasses.contains(RegolithEaters.class)) {
                playedConsumingMicrobeCards--;
            }
        }

        if (oceansMax) {//cards can never consume
            if (handCardClasses.contains(NitriteReductingBacteria.class)) {
                handConsumingMicrobeCards--;
            }
            if (playedCardClasses.contains(NitriteReductingBacteria.class)) {
                playedConsumingMicrobeCards--;
            }
        }

        return new float[]{playedProducingMicrobeCards, Math.max(0, playedConsumingMicrobeCards), Math.max(0, handProducingMicrobeCards), Math.max(0, handConsumingMicrobeCards)};
    }

    private float countVpPerAnimalCards(Set<Class<?>> cardClasses) {
        return ANIMAL_CARDS_VP_PER_RESOURCE.keySet().stream().filter(cardClasses::contains).count();
    }

    private float directlyProducingMicrobeCardsCount(Map<CardAction, Long> cardActions) {
        float result = 0;
        if (cardActions.containsKey(CardAction.CONSERVED_BIOME)) {
            result++;
        }
        if (cardActions.containsKey(CardAction.EXTREME_COLD_FUNGUS)) {
            result++;
        }
        if (cardActions.containsKey(CardAction.SYMBIOTIC_FUNGUD)) {
            result++;
        }
        return result / 3f;
    }

    private int getTotalPlantActionOutput(Map<CardAction, Long> cardActions) {
        return (cardActions.containsKey(CardAction.COMMUNITY_GARDENS) ? 1 : 0) +
                (cardActions.containsKey(CardAction.DECOMPOSING_FUNGUS) ? 3 : 0) +
                (cardActions.containsKey(CardAction.EXTREME_COLD_FUNGUS) ? 1 : 0) +
                (cardActions.containsKey(CardAction.FARMERS_MARKET) ? 2 : 0) +
                (cardActions.containsKey(CardAction.FARMING_COOPS) ? 3 : 0) +
                (cardActions.containsKey(CardAction.GREEN_HOUSES) ? 4 : 0);
    }
}
