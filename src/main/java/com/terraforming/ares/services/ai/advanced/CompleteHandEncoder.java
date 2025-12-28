package com.terraforming.ares.services.ai.advanced;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.buffedCorporations.BuffedArclightCorporation;
import com.terraforming.ares.cards.corporations.ApolloIndustriesCorporation;
import com.terraforming.ares.cards.corporations.ArclightCorporation;
import com.terraforming.ares.cards.green.*;
import com.terraforming.ares.cards.red.*;
import com.terraforming.ares.dto.DraftCardsDto;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.awards.BaseAward;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.model.parameters.OceanRequirement;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.SpecialEffectsService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.terraforming.ares.model.parameters.ParameterColor.*;

@RequiredArgsConstructor
@Component
public class CompleteHandEncoder {
    //todo should have a vector with max values and divide during collection to avoid future normalization


    private static final Map<Class<?>, Float> CARDS_WITH_VP_PER_RESOURCE =//TODO CHECK WITH CHATGPT IF SUCH ROUGH PARAMETER IS OK
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

    private static final Map<Class<?>, Float> NEGATIVE_GAIN_CARDS = Map.of(
            TropicalResort.class, 5f,
            LocalHeatTrapping.class, 3f,
            BuildingIndustries.class, 4f,
            FuelFactory.class, 3f
    );

    private final CardService cardService;
    private final SpecialEffectsService specialEffectsService;
    private final DraftCardsService draftCardsService;
    private final HandEncoderHelperService handEncoderHelperService;

    public float[] evaluate(MarsGame game, Player player, Player anotherPlayer, float[] features, int idx) {
        return new EncoderCalculator(game, player, anotherPlayer, features, idx).encodeHand();
    }


    public class EncoderCalculator {
        private final MarsGame game;
        private final Player player;
        private final Player anotherPlayer;
        private final Set<Card> hand;
        private final Set<Card> playedCards;

        private final Set<Class<?>> handCardClasses;
        private final Set<Class<?>> playedCardClasses;

        private final Map<CardAction, Long> handCardActions;
        private final Map<CardAction, Long> playedCardActions;

        private final boolean canAmplifyOxygenOrTemperature;
        private final Map<Tag, Long> playedTagToCount;

        private final float[] features;
        private int idx;

        public EncoderCalculator(MarsGame game, Player player, Player anotherPlayer, float[] features, int idx) {
            this.game = game;
            this.player = player;
            this.anotherPlayer = anotherPlayer;
            this.hand = player.getHand().getCards().stream().map(cardService::getCard).collect(Collectors.toSet());
            this.playedCards = player.getPlayed().getCards().stream().map(cardService::getCard).collect(Collectors.toSet());

            this.handCardActions =
                    hand.stream()
                            .map(card -> card.getCardMetadata().getCardAction())
                            .filter(Objects::nonNull)
                            .collect(Collectors.groupingBy(
                                    Function.identity(),
                                    Collectors.counting()
                            ));

            playedCardActions =
                    playedCards.stream()
                            .map(card -> card.getCardMetadata().getCardAction())
                            .filter(Objects::nonNull)
                            .collect(Collectors.groupingBy(
                                    Function.identity(),
                                    Collectors.counting()
                            ));

            this.handCardClasses = hand.stream().map(Card::getClass).collect(Collectors.toSet());
            this.playedCardClasses = playedCards.stream().map(Card::getClass).collect(Collectors.toSet());

            this.canAmplifyOxygenOrTemperature = specialEffectsService.ownsSpecialEffect(player, SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT);

            this.playedTagToCount = cardService.countPlayedTagsAsMap(player);

            this.features = features;
            this.idx = idx;
        }

        public float[] encodeHand() {
            if (hand.isEmpty()) {
                return features; // Все нули
            }

            // ===============================================
            // БЛОК 1: ОБЩИЕ ПРИЗНАКИ (30 features) [0-29]
            // ===============================================

            // --- 1.1 Размер руки ---
            features[idx++] = hand.size();  // [0]
            features[idx++] = 1f / (hand.size() + 1);

            long blueCount = hand.stream().filter(card -> card.getColor() == CardColor.BLUE).count();
            long redCount = hand.stream().filter(card -> card.getColor() == CardColor.RED).count();
            long greenCount = hand.stream().filter(card -> card.getColor() == CardColor.GREEN).count();

            features[idx++] = blueCount;
            features[idx++] = redCount;
            features[idx++] = greenCount;

            features[idx++] = playedCards.stream().filter(Card::isActiveCard).count();
            features[idx++] = hand.stream().filter(Card::isActiveCard).count();

            int[] costBuckets = new int[5];

            for (Card card : hand) {
                if (card.getPrice() <= 5) costBuckets[0]++;
                else if (card.getPrice() <= 12) costBuckets[1]++;
                else if (card.getPrice() <= 20) costBuckets[2]++;
                else if (card.getPrice() <= 30) costBuckets[3]++;
                else costBuckets[4]++;
            }

            for (int i = 0; i < 5; i++) {
                features[idx++] = Math.min(1.0f, costBuckets[i] / 10.0f);
            }

            // --- 1.2 Cost статистики ---
            List<Float> costs = extract(c -> (float) c.getPrice());
            FeatureStats costStats = computeStats(costs);

            features[idx++] = costStats.sum;      // [1] Total cost
            features[idx++] = costStats.mean;      // [2] Average cost
            features[idx++] = costStats.max;       // [3] Most expensive
            features[idx++] = costStats.min;       // [4] Cheapest
            features[idx++] = costStats.std;       // [5] Разброс (ВАЖНО!)

            // --- 1.3 Victory Points статистики ---
            List<Float> vps = extract(c -> (float) ((float) c.getWinningPoints() > 0 ? c.getWinningPoints() : 0));
            FeatureStats vpStats = computeStats(vps);

            features[idx++] = vpStats.sum;        // [7] Total VP
            features[idx++] = vpStats.mean;        // [8] Average VP
            features[idx++] = vpStats.max;         // [9] Best card
            features[idx++] = vpStats.std;         // [10] Разброс VP (ВАЖНО!)
            features[idx++] = vpStats.nonZeroCount; // [11] Сколько дают VP

            //negative vp statistics, a couple of -1, and a single of -2
            features[idx++] = hand.stream()
                    .mapToInt(Card::getWinningPoints)
                    .filter(p -> p < 0)
                    .map(Math::abs)
                    .sum();

            // --- 1.4 Income статистики ---
            List<Float> mcIncome = extract((Card c) -> (float) getIncomeByType(c, GainType.MC));
            FeatureStats mcStats = computeStats(mcIncome);
            features[idx++] = mcStats.sum;         // [12] Total MC income
            features[idx++] = mcStats.max;         // [13] Best income
            features[idx++] = mcStats.nonZeroCount; // [14] Cards with income

            List<Float> heatIncome = extract((Card c) -> (float) getIncomeByType(c, GainType.HEAT));
            FeatureStats heatIncomeStats = computeStats(heatIncome);
            features[idx++] = heatIncomeStats.sum; // [15]
            features[idx++] = heatIncomeStats.max; // [16]
            features[idx++] = heatIncomeStats.nonZeroCount; // [17]

            List<Float> plantIncome = extract((Card c) -> (float) getIncomeByType(c, GainType.PLANT));
            FeatureStats plantIncomeStats = computeStats(plantIncome);
            features[idx++] = plantIncomeStats.sum; // [18]
            features[idx++] = plantIncomeStats.max; // [19]
            features[idx++] = plantIncomeStats.nonZeroCount; // [20]

            List<Float> cardDraw = extract((Card c) -> (float) getIncomeByType(c, GainType.CARD));
            FeatureStats cardStats = computeStats(cardDraw);
            features[idx++] = cardStats.sum; // [21]
            features[idx++] = cardStats.max; // [22]
            features[idx++] = cardStats.nonZeroCount; // [23]

            List<Float> steelDraw = extract((Card c) -> (float) getIncomeByType(c, GainType.STEEL));
            FeatureStats steelStats = computeStats(steelDraw);
            features[idx++] = steelStats.sum; // [24]
            features[idx++] = steelStats.max; // [25]
            features[idx++] = steelStats.nonZeroCount; // [26]

            List<Float> titaniumDraw = extract((Card c) -> (float) getIncomeByType(c, GainType.TITANIUM));
            FeatureStats titaniumStats = computeStats(titaniumDraw);
            features[idx++] = titaniumStats.sum; // [27]
            features[idx++] = titaniumStats.max; // [28]
            features[idx++] = titaniumStats.nonZeroCount; // [29]

            List<Float> forestsGain = extract((Card c) -> (float) getPositiveGainByType(c, GainType.FOREST));
            FeatureStats forestsGainStats = computeStats(forestsGain);
            features[idx++] = forestsGainStats.sum;
            features[idx++] = forestsGainStats.max;
            features[idx++] = forestsGainStats.nonZeroCount;
            float oxygenLeftRatio = (float) game.getPlanet().oxygenLeft() / game.getPlanet().oxygenMax();
            features[idx++] = forestsGainStats.sum * oxygenLeftRatio;


            List<Float> temperatureGain = extract((Card c) -> (float) getPositiveGainByType(c, GainType.TEMPERATURE));
            FeatureStats temperatureGainStats = computeStats(temperatureGain);
            features[idx++] = temperatureGainStats.sum;
            features[idx++] = temperatureGainStats.max;
            features[idx++] = temperatureGainStats.nonZeroCount;
            float temperatureLeftRatio = (float) game.getPlanet().temperatureLeft() / game.getPlanet().temperatureMax();
            features[idx++] = temperatureGainStats.sum * temperatureLeftRatio;
            features[idx++] = temperatureGainStats.max * temperatureLeftRatio;

            List<Float> oxygenGain = extract((Card c) -> (float) getPositiveGainByType(c, GainType.OXYGEN));
            FeatureStats oxygenGainStats = computeStats(oxygenGain);
            features[idx++] = oxygenGainStats.sum;
            features[idx++] = forestsGainStats.sum * oxygenLeftRatio;

            List<Float> oceanGain = extract((Card c) -> (float) getPositiveGainByType(c, GainType.OCEAN));
            FeatureStats oceanGainStats = computeStats(oceanGain);
            features[idx++] = oceanGainStats.sum;
            features[idx++] = oceanGainStats.max;
            features[idx++] = oceanGainStats.nonZeroCount;
            float oceansLeftRatio = (float) game.getPlanet().oceansLeft() / game.getPlanet().oceansMaxCount();
            features[idx++] = oceanGainStats.sum * oceansLeftRatio;
            features[idx++] = oceanGainStats.max * oceansLeftRatio;

            List<Float> positivePlantGain = extract((Card c) -> (float) getPositiveGainByType(c, GainType.PLANT));
            if (handCardActions.containsKey(CardAction.FARMING_COOPS)) {
                positivePlantGain.add(3f);//bugfix
            }
            if (handCardActions.containsKey(CardAction.LOCAL_HEAT_TRAPPING)) {
                positivePlantGain.add(4f);//bugfix
            }
            FeatureStats plantGainStats = computeStats(positivePlantGain);
            features[idx++] = plantGainStats.sum;
            features[idx++] = plantGainStats.max;
            features[idx++] = plantGainStats.nonZeroCount;

            List<Float> negativePlantGain = extract((Card c) -> (float) getNegatedGainByType(c, GainType.PLANT));
            FeatureStats negativePlantGainStats = computeStats(negativePlantGain);
            features[idx++] = negativePlantGainStats.sum;
            features[idx++] = negativePlantGainStats.max;
            features[idx++] = negativePlantGainStats.nonZeroCount;

            List<Float> positiveHeatGain = extract((Card c) -> (float) getPositiveGainByType(c, GainType.HEAT));
            FeatureStats positiveHeatStats = computeStats(positiveHeatGain);
            features[idx++] = positiveHeatStats.sum;
            features[idx++] = positiveHeatStats.max;
            features[idx++] = positiveHeatStats.nonZeroCount;

            List<Float> negativeHeatGain = getNegativeHeatGainValuesOnBuild();
            features[idx++] = negativeHeatGain.stream().reduce(0.0f, Float::sum);
            features[idx++] = negativeHeatGain.size();

            List<Float> cardPositiveGain = extract((Card c) -> (float) getPositiveGainByType(c, GainType.CARD));
            if (handCardClasses.contains(MatterGenerator.class)) {
                cardPositiveGain.add(2f);//bugfix
            }
            FeatureStats cardPositiveGainStats = computeStats(cardPositiveGain);
            features[idx++] = cardPositiveGainStats.sum;
            features[idx++] = cardPositiveGainStats.max;
            features[idx++] = cardPositiveGainStats.nonZeroCount;

            List<Float> terraformingRatingGain = extract((Card c) -> (float) getPositiveGainByType(c, GainType.TERRAFORMING_RATING));
            FeatureStats terraformingRatingGainStats = computeStats(terraformingRatingGain);
            features[idx++] = terraformingRatingGainStats.sum;
            features[idx++] = terraformingRatingGainStats.max;
            features[idx++] = terraformingRatingGainStats.nonZeroCount;

            List<Float> negativeTRgain = extract((Card c) -> (float) getNegatedGainByType(c, GainType.TERRAFORMING_RATING));
            FeatureStats negativeTRgainStats = computeStats(negativeTRgain);
            features[idx++] = negativeTRgainStats.sum;

            // --- 1.5 Теги (количество каждого тега в руке) ---
            int[] handTagCounts = new int[11];
            for (Card card : hand) {
                List<Tag> tags = card.getTags();
                for (Tag tag : tags) {
                    handTagCounts[tag.ordinal()]++;
                }
            }

            for (int i = 0; i < handTagCounts.length; i++) {
                features[idx++] = handTagCounts[i];
            }

            // --- 1.6 Тег diversity ---
            int uniqueTags = 0;
            for (int count : handTagCounts) {
                if (count > 0) uniqueTags++;
            }
            features[idx++] = uniqueTags;          //Разнообразие тегов

            // --- 1.7 Самый частый тег ---
            int maxTagCount = 0;
            for (int count : handTagCounts) {
                maxTagCount = Math.max(maxTagCount, count);
            }
            features[idx++] = maxTagCount;         //Max tag concentration

            //real incomes, lets artificial jungle to know if it has plants
            features[idx++] = player.getPlantsIncome() > 0 ? 1 : 0;
            features[idx++] = (float) Math.log1p(player.getPlants());
            features[idx++] = (float) Math.log1p(player.getPlantsIncome());

            //real incomes, lets development center to know if it has plants
            features[idx++] = (float) Math.log1p(player.getHeat());
            features[idx++] = (float) Math.log1p(player.getHeatIncome());

            features[idx++] = player.getCardIncome() > 0 ? 1 : 0;
            features[idx++] = (float) Math.log1p(player.getCardIncome());

            int earth = playedTagToCount.getOrDefault(Tag.EARTH, 0L).intValue();
            int animal = playedTagToCount.getOrDefault(Tag.ANIMAL, 0L).intValue();
            int plant = playedTagToCount.getOrDefault(Tag.PLANT, 0L).intValue();
            int science = playedTagToCount.getOrDefault(Tag.SCIENCE, 0L).intValue();
            int building = playedTagToCount.getOrDefault(Tag.BUILDING, 0L).intValue();
            int playedEnergy = playedTagToCount.getOrDefault(Tag.ENERGY, 0L).intValue();
            int space = playedTagToCount.getOrDefault(Tag.SPACE, 0L).intValue();
            int event = playedTagToCount.getOrDefault(Tag.EVENT, 0L).intValue();
            int microbe = playedTagToCount.getOrDefault(Tag.MICROBE, 0L).intValue();
            int playedJupiters = playedTagToCount.getOrDefault(Tag.JUPITER, 0L).intValue();
            int forests = player.getForests();

            features[idx++] = space;
            features[idx++] = earth;
            features[idx++] = event;
            features[idx++] = science;
            features[idx++] = plant;
            features[idx++] = playedEnergy;
            features[idx++] = building;
            features[idx++] = animal;
            features[idx++] = playedJupiters;
            features[idx++] = microbe;

            features[idx++] = getImmediateMicrobeGainSum();//cards that when built put microbes on any other chosen card
            features[idx++] = getImmediateAnimalGainSum();//cards that when built put animals on any other chose card
            features[idx++] = getImmediatePlantGainCapacity();
            features[idx++] = getImmediateMicrobeGainCapacity();
            features[idx++] = getImmediateAnimalGainCapacity();

            handleEngineIncome(CardAction.HEAT_EARTH_INCOME, earth, features, idx);
            idx += 4;

            handleEngineIncome(CardAction.MC_ANIMAL_PLANT_INCOME, animal + plant, features, idx);
            idx += 4;

            handleEngineIncome(CardAction.PLANT_PLANT_INCOME, plant, features, idx);
            idx += 4;

            handleEngineIncome(CardAction.CARD_SCIENCE_INCOME, science / 3f, features, idx);
            idx += 4;

            handleEngineIncome(CardAction.MC_SCIENCE_INCOME, science, features, idx);
            idx += 4;

            handleEngineIncome(CardAction.MC_2_BUILDING_INCOME, building / 2f, features, idx);
            idx += 4;

            handleEngineIncome(CardAction.MC_ENERGY_INCOME, playedEnergy, features, idx);
            idx += 4;

            handleEngineIncome(CardAction.MC_SPACE_INCOME, space, features, idx);
            idx += 4;

            handleEngineIncome(CardAction.HEAT_SPACE_INCOME, space, features, idx);
            idx += 4;

            handleEngineIncome(CardAction.MC_EVENT_INCOME, event, features, idx);
            idx += 4;

            handleEngineIncome(CardAction.HEAT_ENERGY_INCOME, playedEnergy, features, idx);
            idx += 4;

            handleEngineIncome(CardAction.PLANT_MICROBE_INCOME, microbe, features, idx);
            idx += 4;

            handleEngineIncome(CardAction.MC_FOREST_INCOME, forests, features, idx);
            idx += 4;

            features[idx++] = handCardActions.containsKey(CardAction.PROCESSED_METALS) ? 1 : 0;
            features[idx++] = handCardActions.containsKey(CardAction.PROCESSED_METALS) ? playedEnergy : 0;

            {
                boolean cardInHand = handCardActions.containsKey(CardAction.MC_EARTH_INCOME);
                boolean cardPlayed = playedCardActions.containsKey(CardAction.MC_EARTH_INCOME);
                features[idx++] = cardInHand ? 1 : 0;
                features[idx++] = cardPlayed ? 1 : 0;
                features[idx++] = cardInHand ? handCardActions.get(CardAction.MC_EARTH_INCOME) * earth : 0;
                features[idx++] = cardPlayed ? playedCardActions.get(CardAction.MC_EARTH_INCOME) * earth : 0;
            }


            if (handCardActions.containsKey(CardAction.AWARD_WINNING_REFLECTOR)) {
                features[idx++] = 1;
                features[idx++] = game.getMilestones().stream().anyMatch(milestone -> milestone.isAchieved(player)) ? 1 : 0;
            } else {
                features[idx++] = 0;
                features[idx++] = 0;
            }
            if (handCardActions.containsKey(CardAction.TOURISM)) {
                features[idx++] = 1;
                features[idx++] = game.getMilestones().stream().filter(milestone -> milestone.isAchieved(player)).count();
            } else {
                features[idx++] = 0;
                features[idx++] = 0;
            }


            boolean handAf = handCardActions.containsKey(CardAction.AUTOMATED_FACTORIES);
            boolean handTS = handCardActions.containsKey(CardAction.TALL_STATION);
            if (handAf || handTS) {
                features[idx++] = ((handAf ? 1 : 0) + (handTS ? 1 : 0)) / 2.0F;//build extra at cost <9
                features[idx++] = hand.stream().filter(card -> card.getColor() == CardColor.GREEN && card.getPrice() <= 9).count();
            } else {
                features[idx++] = 0;
                features[idx++] = 0;
            }

            if (handCardActions.containsKey(CardAction.ENERGY_STORAGE)) {
                features[idx++] = 1;
                features[idx++] = Math.min(1.0f, (player.getTerraformingRating() / 7f));
            } else {
                features[idx++] = 0;
                features[idx++] = 0;
            }
            features[idx++] = handCardClasses.contains(Microprocessors.class) ? 1 : 0;//draw 2 cards then discard a card
            features[idx++] = handCardClasses.contains(InventionContest.class) ? 1 : 0;//draw 3 cards then discard a card
            features[idx++] = handCardActions.containsKey(CardAction.PROCESSING_PLANT) ? 1 : 0;//gives card with building tag

            features[idx++] = handCardClasses.contains(AssortedEnterprises.class) ? 1 : 0;//lets u build any extra card with 2mc discount
            features[idx++] = handCardClasses.contains(BusinessContracts.class) ? 1 : 0;//take 4 cards and discard 2
            features[idx++] = handCardClasses.contains(CeosFavoriteProject.class) ? 1 : 0;//can put any 2 resources
            features[idx++] = handCardClasses.contains(SpecialDesign.class) ? 1 : 0;//lets you build 1 building with +- requirement
            features[idx++] = handCardClasses.contains(BiomedicalImports.class) ? 1 : 0;//raise oxygen or improve phase
            features[idx++] = handCardClasses.contains(InnovativeTechnologiesAward.class) ? player.getPhaseCards().stream().filter(phaseCard -> phaseCard != 0).count() : 0;

            if (handCardClasses.contains(LocalHeatTrapping.class)) {//-3 heat, +4 plants, +2 animals or + 2 microbes
                features[idx++] = 1;
                features[idx++] = (float) Math.min(1.0, player.getHeat() / 3f);
            } else {
                features[idx++] = 0;
                features[idx++] = 0;
            }

            if (handCardClasses.contains(TropicalResort.class)) {//-5 heat
                features[idx++] = 1;
                features[idx++] = (float) Math.min(1.0, player.getHeat() / 5f);
            } else {
                features[idx++] = 0;
                features[idx++] = 0;
            }

            if (handCardClasses.contains(BuildingIndustries.class)) {//-4 heat
                features[idx++] = 1;
                features[idx++] = (float) Math.min(1.0, player.getHeat() / 4f);
            } else {
                features[idx++] = 0;
                features[idx++] = 0;
            }

            if (handCardClasses.contains(FuelFactory.class)) {//-3 heat
                features[idx++] = 1;
                features[idx++] = (float) Math.min(1.0, player.getHeat() / 3f);
            } else {
                features[idx++] = 0;
                features[idx++] = 0;
            }

            if (handCardClasses.contains(Trees.class)) {
                features[idx++] = 1;
                features[idx++] = getMinYellowTemperatureAvailabilityProgress();
            } else {
                features[idx++] = 0;
                features[idx++] = 0;
            }

            if (handCardClasses.contains(TundraFarming.class)) {
                features[idx++] = 1;
                features[idx++] = getMinYellowTemperatureAvailabilityProgress();
            } else {
                features[idx++] = 0;
                features[idx++] = 0;
            }


            features[idx++] = handCardClasses.contains(AdaptationTechnology.class) && !canAmplifyOxygenOrTemperature ? 1 : 0;
            features[idx++] = specialEffectsService.ownsSpecialEffect(player, SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT) ? 1 : 0;


            features[idx++] = handCardClasses.contains(AdvancedScreeningTechnology.class) ? 1 : 0;


            {
                boolean handAiCentral = handCardClasses.contains(AiCentral.class);
                boolean handAntiGravity = handCardClasses.contains(AntiGravityTechnology.class);
                boolean playedAiCentral = playedCardClasses.contains(AiCentral.class);
                boolean playedAntiGravity = playedCardClasses.contains(AntiGravityTechnology.class);
                boolean handCircuitBoard = handCardClasses.contains(CircuitBoardFactory.class);
                boolean playedCircuitBoard = playedCardClasses.contains(CircuitBoardFactory.class);
                boolean handCityCouncil = handCardClasses.contains(CityCouncil.class);
                boolean playedCityCouncil = playedCardClasses.contains(CityCouncil.class);
                boolean handVirtualEmployee = handCardClasses.contains(VirtualEmployeeDevelopment.class);
                boolean playedVirtualEmployee = playedCardClasses.contains(VirtualEmployeeDevelopment.class);


                features[idx++] = handAiCentral ? 1 : 0;
                features[idx++] = handAntiGravity ? 1 : 0;

                features[idx++] = playedAiCentral ? 1 : 0;
                features[idx++] = playedAntiGravity ? 1 : 0;

                features[idx++] = handCircuitBoard ? 1 : 0;
                features[idx++] = playedCircuitBoard ? 1 : 0;

                features[idx++] = handCityCouncil ? 1 : 0;
                features[idx++] = playedCityCouncil ? 1 : 0;

                features[idx++] = handVirtualEmployee ? 1 : 0;
                features[idx++] = playedVirtualEmployee ? 1 : 0;

                int potentialGetCards = (handAiCentral ? 2 : 0) + (handCircuitBoard ? 1 : 0);
                int realGetCards = (playedAiCentral ? 2 : 0) + (playedCircuitBoard ? 1 : 0);

                if (handCityCouncil || playedCityCouncil) {
                    int milestonesAchieved = (int) game.getMilestones().stream().filter(milestone -> milestone.isAchieved(player)).count();
                    if (handCityCouncil) {
                        potentialGetCards += (1 + milestonesAchieved);
                    }
                    if (playedCityCouncil) {
                        realGetCards += (1 + milestonesAchieved);
                    }
                }

                features[idx++] = potentialGetCards;
                features[idx++] = realGetCards;

                features[idx++] = (handAiCentral || handAntiGravity) && science >= 5 ? 1 : 0;
                features[idx++] = (handAiCentral || handAntiGravity) ? Math.max(0, handTagCounts[Tag.SCIENCE.ordinal()] - (handAiCentral ? 1 : 0) - (handAntiGravity ? 1 : 0)) : 0;
                features[idx++] = (handAiCentral || handAntiGravity) ? Math.min(1.0f, (float) science / 5.f) : 0;
                features[idx++] = handVirtualEmployee ? Math.min(1.0f, (float) science / 3.f) : 0;
            }

            {
                boolean handAM = handCardClasses.contains(AnaerobicMicroorganisms.class);
                boolean playedAM = playedCardClasses.contains(AnaerobicMicroorganisms.class);
                boolean handDecomposers = handCardClasses.contains(Decomposers.class);
                boolean playedDecomposers = playedCardClasses.contains(Decomposers.class);

                features[idx++] = handAM ? 1 : 0;
                features[idx++] = playedAM ? 1 : 0;
                features[idx++] = handDecomposers ? 1 : 0;
                features[idx++] = playedDecomposers ? 1 : 0;

                features[idx++] = handAM || playedAM ? handTagCounts[Tag.ANIMAL.ordinal()] + handTagCounts[Tag.PLANT.ordinal()] + handTagCounts[Tag.MICROBE.ordinal()] : 0;
                features[idx++] = handDecomposers || playedDecomposers ? handTagCounts[Tag.ANIMAL.ordinal()] + handTagCounts[Tag.PLANT.ordinal()] + handTagCounts[Tag.MICROBE.ordinal()] : 0;
            }

            //OCEAN BLUE CARDS
            {//good example
                boolean handAquifer = handCardActions.containsKey(CardAction.AQUIFER_PUMPING);
                boolean playedAquifer = playedCardActions.containsKey(CardAction.AQUIFER_PUMPING);

                features[idx++] = handAquifer ? 1 : 0;
                features[idx++] = playedAquifer ? 1 : 0;

                features[idx++] = (handAquifer || playedAquifer) ? oceansLeftRatio : 0;
                features[idx++] = (handAquifer || playedAquifer) && !game.getPlanet().isOceansMax() ? (10 - Math.min(5, player.getSteelIncome()) * 2) : 0;
            }

            {
                boolean handVolcanicPools = handCardClasses.contains(VolcanicPools.class);
                boolean playedVolcanicPools = playedCardClasses.contains(VolcanicPools.class);

                features[idx++] = handVolcanicPools ? 1 : 0;
                features[idx++] = playedVolcanicPools ? 1 : 0;

                features[idx++] = playedVolcanicPools || handVolcanicPools ? oceansLeftRatio : 0;
                features[idx++] = (handVolcanicPools || playedVolcanicPools) && !game.getPlanet().isOceansMax() ? Math.max(0, 12 - playedEnergy) : 0;
            }

            {
                boolean handCA = handCardClasses.contains(CommunityAfforestation.class);
                boolean playedCA = playedCardClasses.contains(CommunityAfforestation.class);

                features[idx++] = handCA ? 1 : 0;
                features[idx++] = playedCA ? 1 : 0;

                if (handCA || playedCA) {
                    int milestonesAchieved = (int) game.getMilestones().stream().filter(milestone -> milestone.isAchieved(player)).count();
                    features[idx++] = 14 - milestonesAchieved * 4;
                } else {
                    features[idx++] = 0;
                }
            }

            Map<GainType, FeatureStats> gainTypeToCounts = Map.of(
                    GainType.FOREST, forestsGainStats,
                    GainType.OXYGEN, oxygenGainStats,
                    GainType.TEMPERATURE, temperatureGainStats,
                    GainType.OCEAN, oceanGainStats,
                    GainType.TERRAFORMING_RATING, terraformingRatingGainStats
            );
            Map<GainType, FeatureStats> incomeTypeToCounts = Map.of(
                    GainType.HEAT, heatIncomeStats,
                    GainType.PLANT, plantIncomeStats
            );

            {
                boolean austellarCorp = playedCardActions.containsKey(CardAction.AUSTELLAR_CORPORATION);

                if (austellarCorp) {
                    features[idx++] = 1;

                    Milestone austerllarMilestone = game.getMilestones().get(player.getAustellarMilestone());
                    features[idx++] = austerllarMilestone.isAchieved() ? 0 : handEncoderHelperService.countMilestoneProgressByHand(austerllarMilestone, hand, handTagCounts, gainTypeToCounts, incomeTypeToCounts);
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean ecolineCorp = playedCardActions.containsKey(CardAction.ECOLINE_CORPORATION);
                features[idx++] = ecolineCorp ? 1.0f : 0.0f;

                if (ecolineCorp) {
                    features[idx++] = plantIncomeStats.sum;
                    features[idx++] = handTagCounts[Tag.PLANT.ordinal()];
                } else {
                    features[idx++] = 0.0f;
                    features[idx++] = 0.0f;
                }
            }

            {
                boolean nebulabCorp = playedCardActions.containsKey(CardAction.NEBU_LABS_CORPORATION);
                boolean handCS = handCardActions.containsKey(CardAction.COMMUNICATIONS_STREAMLINING);
                boolean playedCS = playedCardActions.containsKey(CardAction.COMMUNICATIONS_STREAMLINING);
                features[idx++] = handCS ? 1.0f : 0.0f;
                features[idx++] = (nebulabCorp ? 2 : 0) + (playedCS ? 1 : 0);

                if (nebulabCorp || handCS || playedCS) {
                    features[idx++] = Math.min(3, (int) player.getPhaseCards().stream().filter(phaseCard -> phaseCard != 0).count());
                } else {
                    features[idx++] = 0.0f;
                }
            }

            {
                boolean handAT = handCardActions.containsKey(CardAction.EXPERIMENTAL_TECHNOLOGY);
                boolean playedAT = playedCardActions.containsKey(CardAction.EXPERIMENTAL_TECHNOLOGY);
                features[idx++] = handAT ? 1 : 0;
                features[idx++] = playedAT ? 1 : 0;
                features[idx++] = (handAT || playedAT) ? player.getPhaseCards().stream().filter(phaseCard -> phaseCard != 0).count() : 0;
            }

            {
                boolean handFCM = handCardActions.containsKey(CardAction.FIBROUS_COMPOSITE_MATERIAL);
                boolean playedFCM = playedCardActions.containsKey(CardAction.FIBROUS_COMPOSITE_MATERIAL);
                features[idx++] = handFCM ? 1 : 0;
                features[idx++] = playedFCM ? 1 : 0;
                features[idx++] = (handFCM || playedFCM) ? player.getPhaseCards().stream().filter(phaseCard -> phaseCard != 0).count() : 0;
            }

            {
                boolean mayniCorp = playedCardActions.containsKey(CardAction.MAY_NI_PRODUCTIONS_CORPORATION);
                features[idx++] = mayniCorp ? 1.0f : 0.0f;
                features[idx++] = mayniCorp ? greenCount : 0;
            }

            {
                boolean unmiCorp = playedCardActions.containsKey(CardAction.UNMI_CORPORATION);
                features[idx++] = unmiCorp ? 1.0f : 0.0f;
                features[idx++] = unmiCorp ? (forestsGainStats.nonZeroCount + oxygenGainStats.nonZeroCount + temperatureGainStats.nonZeroCount + oceanGainStats.nonZeroCount + terraformingRatingGainStats.nonZeroCount) : 0;
            }

            {
                boolean zetacellCorp = playedCardActions.containsKey(CardAction.ZETACELL_CORPORATION);
                boolean handAlgae = handCardActions.containsKey(CardAction.ARCTIC_ALGAE);
                boolean playedAlgae = playedCardActions.containsKey(CardAction.ARCTIC_ALGAE);

                features[idx++] = zetacellCorp ? 1 : 0;
                features[idx++] = handAlgae ? 1 : 0;
                features[idx++] = playedAlgae ? 1 : 0;

                features[idx++] = (zetacellCorp ? 2 : 0) + (playedAlgae ? 4 : 0);

                features[idx++] = (handAlgae || playedAlgae || zetacellCorp) ? oceansLeftRatio : 0;
                features[idx++] = (handAlgae || playedAlgae || zetacellCorp) ? oxygenLeftRatio : 0;

                features[idx++] = handAlgae ? getMinRedTemperatureAvailabilityProgress() : 0;
            }

            {//good example
                boolean handSoil = handCardActions.containsKey(CardAction.VOLCANIC_SOIL);
                boolean playedSoil = playedCardActions.containsKey(CardAction.VOLCANIC_SOIL);

                features[idx++] = handSoil ? 1 : 0;
                features[idx++] = playedSoil ? 1 : 0;

                features[idx++] = (handSoil || playedSoil) ? temperatureLeftRatio : 0;
                features[idx++] = (handSoil || playedSoil) ? oxygenLeftRatio : 0;
            }

            {//good example
                boolean handFish = handCardActions.containsKey(CardAction.FISH);
                boolean playedFish = playedCardActions.containsKey(CardAction.FISH);

                features[idx++] = handFish ? 1 : 0;
                features[idx++] = playedFish ? 1 : 0;

                features[idx++] = (playedFish || handFish) ? oceansLeftRatio : 0;
                features[idx++] = handFish ? getMinRedTemperatureAvailabilityProgress() : 0;
            }

            {//good example
                boolean handHerbivores = handCardActions.containsKey(CardAction.HERBIVORES);
                boolean playedHerbivores = playedCardActions.containsKey(CardAction.HERBIVORES);

                features[idx++] = handHerbivores ? 1 : 0;
                features[idx++] = playedHerbivores ? 1 : 0;

                float triggersLeft = game.getPlanet().oceansLeft() + game.getPlanet().temperatureLeft() + game.getPlanet().oxygenLeft();

                features[idx++] = handHerbivores ? triggersLeft : 0;
                features[idx++] = playedHerbivores ? triggersLeft : 0;
                features[idx++] = handHerbivores ? Math.min(1, (float) game.getPlanet().oceansBuilt() / 5) : 0;//availability progress
            }

            features[idx++] = handCardClasses.contains(RestructuredResources.class) ? 1 : 0;//handled by log of plants and plantsincome
            features[idx++] = playedCardClasses.contains(RestructuredResources.class) ? 1 : 0;//handled by log of plants and plantsincome

            features[idx++] = handCardClasses.contains(DevelopmentCenter.class) ? 1 : 0;//handled by log of heat and heatincome
            features[idx++] = playedCardClasses.contains(DevelopmentCenter.class) ? 1 : 0;//handled by log of heat and heatincome


            {//Assembly Lines
                boolean handAL = handCardClasses.contains(AssemblyLines.class);
                boolean playedAL = playedCardClasses.contains(AssemblyLines.class);

                features[idx++] = handAL ? 1 : 0;
                features[idx++] = playedAL ? 1 : 0;

                if (handAL || playedAL) {
                    int tableEasyCards = (int) playedCardClasses.stream().filter(EncoderConstants.ASSEMBLY_LINES_EASY_CARDS::contains).count();
                    int handActiveCards = (int) hand.stream().filter(Card::isActiveCard).count();

                    features[idx++] = tableEasyCards;
                    features[idx++] = handActiveCards;
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean isHelionCorp = playedCardActions.containsKey(CardAction.HELION_CORPORATION);
                boolean handPI = handCardActions.containsKey(CardAction.POWER_INFRASTRUCTURE);
                boolean playedPI = playedCardActions.containsKey(CardAction.POWER_INFRASTRUCTURE);

                features[idx++] = isHelionCorp ? 1 : 0;
                features[idx++] = handPI ? 1 : 0;
                features[idx++] = playedPI ? 1 : 0;

                features[idx++] = isHelionCorp ? (float) Math.log1p(player.getHeatIncome()) : 0f;
                features[idx++] = isHelionCorp ? (float) Math.log1p(player.getHeat()) : 0f;
                features[idx++] = (handPI || playedPI) ? (float) Math.log1p(player.getHeatIncome()) : 0f;
                features[idx++] = (handPI || playedPI) ? (float) Math.log1p(player.getHeat()) : 0f;
            }

            features[idx++] = handCardClasses.contains(AssetLiquidation.class) ? 1 : 0;
            features[idx++] = playedCardClasses.contains(AssetLiquidation.class) ? 1 : 0;

            features[idx++] = handCardClasses.contains(BrainstormingSession.class) ? 1 : 0;
            features[idx++] = playedCardClasses.contains(BrainstormingSession.class) ? 1 : 0;

            {
                boolean handCaretakerContract = handCardClasses.contains(CaretakerContract.class);
                features[idx++] = handCaretakerContract ? 1 : 0;
                features[idx++] = playedCardClasses.contains(CaretakerContract.class) ? 1 : 0;

                features[idx++] = handCaretakerContract ? getMinYellowTemperatureAvailabilityProgress() : 0;
            }

            //Useful and potential heat capacity
            {//TODO this smells
                float playerHeatIncome = (float) player.getHeatIncome();
                if (playedCardActions.containsKey(CardAction.HYDRO_ELECTRIC)) {
                    playerHeatIncome += 2.5f;
                }
                float usefulHeatIncome;

                boolean hasInfiniteHeatSink = playedCardActions.containsKey(CardAction.HELION_CORPORATION)
                        || playedCardActions.containsKey(CardAction.POWER_INFRASTRUCTURE); // Твоя новая карта

                if (!game.getPlanet().isTemperatureMax() || hasInfiniteHeatSink) {
                    usefulHeatIncome = playerHeatIncome;
                } else {
                    float capacity = getHeatConsumeCapacityOnActions(playedCardActions);

                    usefulHeatIncome = Math.min(playerHeatIncome, capacity);
                }

                features[idx++] = (float) Math.log1p(usefulHeatIncome + 1);

                float potentialHeatCapacity = getHeatConsumeCapacityOnActions(handCardActions);
                if (handCardActions.containsKey(CardAction.POWER_INFRASTRUCTURE)) {
                    potentialHeatCapacity = 99;
                }
                features[idx++] = potentialHeatCapacity;
            }

            boolean hasHyperionCorp = playedCardActions.containsKey(CardAction.HYPERION_SYSTEMS_CORPORATION);
            boolean hasHandCommunityGardens = handCardClasses.contains(CommunityGardens.class);
            boolean hasPlayedCommunityGardens = playedCardClasses.contains(CommunityGardens.class);

            boolean handDroneAC = handCardClasses.contains(DroneAssistedConstruction.class);
            boolean playedDroneAC = playedCardClasses.contains(DroneAssistedConstruction.class);

            {
                features[idx++] = hasHandCommunityGardens ? 1 : 0;//potential can get MC and plant in 3rd phase
                features[idx++] = handDroneAC ? 1 : 0;//potential can get MC in 3rd phase
                features[idx++] = hasPlayedCommunityGardens ? 1 : 0;//can get extra plant if chose 3rd

                features[idx++] = (hasHyperionCorp ? 1 : 0);//extra MC if chosen 3
                features[idx++] = (hasPlayedCommunityGardens ? 2 : 0) + (hasHyperionCorp ? 1 : 0) + (playedDroneAC ? 2 : 0);//get MC in 3
            }

            {
                boolean hasCorp = playedCardActions.containsKey(CardAction.MINING_GUILD_CORPORATION);

                if (hasCorp) {
                    features[idx++] = 1;
                    features[idx++] = steelStats.nonZeroCount;
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean hasCorp = playedCardActions.containsKey(CardAction.SATURN_SYSTEMS_CORPORATION);

                if (hasCorp) {
                    features[idx++] = 1;
                    features[idx++] = handTagCounts[Tag.JUPITER.ordinal()];
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            boolean exocorpOnTable = playedCardActions.containsKey(CardAction.EXOCORP_CORPORATION);
            boolean compostingOnTable = playedCardActions.containsKey(CardAction.COMPOSTING_FACTORY);
            boolean compostingInHand = handCardActions.containsKey(CardAction.COMPOSTING_FACTORY);

            int activeSellBonus = (exocorpOnTable ? 1 : 0) + (compostingOnTable ? 1 : 0);
            {
                features[idx++] = activeSellBonus;

                if (activeSellBonus > 0) {
                    features[idx++] = player.getCardIncome();
                    features[idx++] = player.getHand().size();
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }

                features[idx++] = compostingInHand ? 1 : 0;

                boolean matterManufacturingOnTable = playedCardActions.containsKey(CardAction.MATTER_MANUFACTORING);
                boolean matterManufacturingInHand = handCardActions.containsKey(CardAction.MATTER_MANUFACTORING);

                boolean thinkTankOnTable = playedCardActions.containsKey(CardAction.THINKTANK);
                boolean thinkTankInHand = handCardActions.containsKey(CardAction.THINKTANK);

                int cardToMcMargin = 0;
                if (matterManufacturingOnTable) cardToMcMargin += (3 + activeSellBonus - 1);
                if (thinkTankOnTable) cardToMcMargin += (3 + activeSellBonus - 2);

                features[idx++] = cardToMcMargin;

                int potentialMargin = 0;
                if (matterManufacturingInHand) potentialMargin += (3 + activeSellBonus - 1);
                if (thinkTankInHand) potentialMargin += (3 + activeSellBonus - 2);

                features[idx++] = potentialMargin;
            }


            boolean hasHandFarmersMarket = handCardActions.containsKey(CardAction.FARMERS_MARKET);
            boolean hasPlayedFarmersMarket = playedCardActions.containsKey(CardAction.FARMERS_MARKET);

            features[idx++] = hasHandFarmersMarket ? 1 : 0;
            features[idx++] = hasPlayedFarmersMarket ? 1 : 0;

            boolean hasHandFarmingCoops = handCardActions.containsKey(CardAction.FARMING_COOPS);
            boolean hasPlayedFarmingCoops = playedCardActions.containsKey(CardAction.FARMING_COOPS);

            features[idx++] = hasHandFarmingCoops ? 1 : 0;
            features[idx++] = hasPlayedFarmingCoops ? 1 : 0;

            {
                boolean handGreenHouses = handCardActions.containsKey(CardAction.GREEN_HOUSES);
                features[idx++] = handGreenHouses ? 1 : 0;
                features[idx++] = playedCardActions.containsKey(CardAction.GREEN_HOUSES) ? 1 : 0;
                features[idx++] = handGreenHouses ? getMinYellowTemperatureAvailabilityProgress() : 0;
            }

            {
                boolean handITA = handCardClasses.contains(InnovativeTechnologiesAward.class);
                features[idx++] = handITA ? 1 : 0;
                features[idx++] = handITA ? getMinYellowTemperatureAvailabilityProgress() : 0;
            }

            {
                features[idx++] = handCardClasses.contains(MartianStudiesScholarship.class) ? 1 : 0;
            }

            {
                boolean handICC = handCardClasses.contains(ImportedConstructionCrews.class);
                features[idx++] = handICC ? 1 : 0;
                features[idx++] = handICC ? player.getPhaseCards().stream().filter(phaseCard -> phaseCard != 0).count() : 0;
            }

            boolean ghgHand = handCardActions.containsKey(CardAction.GHG_PRODUCTION);
            boolean ghgTable = playedCardActions.containsKey(CardAction.GHG_PRODUCTION);

            boolean regolithHand = handCardActions.containsKey(CardAction.REGOLITH_EATERS);
            boolean regolithTable = playedCardActions.containsKey(CardAction.REGOLITH_EATERS);

            boolean nitriteHand = handCardActions.containsKey(CardAction.NITRITE_REDUCTING);
            boolean nitriteTable = playedCardActions.containsKey(CardAction.NITRITE_REDUCTING);

            features[idx++] = ghgHand ? 1 : 0;
            features[idx++] = ghgTable ? 1 : 0;

            features[idx++] = regolithHand ? 1 : 0;
            features[idx++] = regolithTable ? 1 : 0;

            features[idx++] = nitriteHand ? 1 : 0;
            features[idx++] = nitriteTable ? 1 : 0;


            if (nitriteHand || nitriteTable) {
                features[idx++] = oceansLeftRatio;
            } else {
                features[idx++] = 0;
            }

            if (regolithHand || regolithTable) {
                features[idx++] = oxygenLeftRatio;
            } else {
                features[idx++] = 0;
            }

            if (ghgHand || ghgTable) {
                features[idx++] = temperatureLeftRatio;
            } else {
                features[idx++] = 0;
            }

            {
                boolean handSelfRep = handCardActions.containsKey(CardAction.SELF_REPLICATING_BACTERIA);
                boolean playedSelfRep = playedCardActions.containsKey(CardAction.SELF_REPLICATING_BACTERIA);

                features[idx++] = handSelfRep ? 1 : 0;
                features[idx++] = playedSelfRep ? 1 : 0;

                if (playedSelfRep) {
                    features[idx++] = player.getCardResourcesCount().getOrDefault(SelfReplicatingBacteria.class, 0);
                } else {
                    features[idx++] = 0;
                }

                float maxCostInHand = (float) hand.stream()
                        .filter(c -> !c.getClass().equals(SelfReplicatingBacteria.class))
                        .mapToInt(Card::getPrice)
                        .max()
                        .orElse(0);

                features[idx++] = (playedSelfRep && player.getCardResourcesCount()
                        .getOrDefault(SelfReplicatingBacteria.class, 0) >= 3)
                        ? Math.min(1.0f, maxCostInHand / 25.0f)
                        : 0;
            }

            {
                boolean handLivestock = handCardActions.containsKey(CardAction.LIVESTOCK);
                boolean playedLivestock = playedCardActions.containsKey(CardAction.LIVESTOCK);

                features[idx++] = handLivestock ? 1 : 0;
                features[idx++] = playedLivestock ? 1 : 0;

                features[idx++] = (playedLivestock || handLivestock) ? temperatureLeftRatio : 0;

                int currentOxygenLevel = game.getPlanet().getCurrentLevel(GlobalParameter.OXYGEN);
                int targetOxygenLevel = getOxygenStepFromColor(canAmplifyOxygenOrTemperature ? ParameterColor.R : ParameterColor.Y, false);

                float availabilityProgress = (currentOxygenLevel >= targetOxygenLevel) ? 1f : (float) currentOxygenLevel / targetOxygenLevel;
                features[idx++] = handLivestock ? availabilityProgress : 0;
            }

            {
                boolean handSA = handCardActions.containsKey(CardAction.SMALL_ANIMALS);
                boolean playedSA = playedCardActions.containsKey(CardAction.SMALL_ANIMALS);

                features[idx++] = handSA ? 1 : 0;
                features[idx++] = playedSA ? 1 : 0;
                features[idx++] = handSA ? getMinRedTemperatureAvailabilityProgress() : 0;

                float greeneryPotential = (float) (Math.log1p(player.getPlantsIncome()) + 0.5f * Math.log1p(player.getMcIncome() + player.getTerraformingRating()));
                features[idx++] = (handSA || playedSA) ? greeneryPotential : 0;
                features[idx++] = (handSA || playedSA) ? greeneryPotential * oxygenLeftRatio : 0;
            }

            {
                boolean handZoos = handCardActions.containsKey(CardAction.ZOOS);
                boolean playedZoos = playedCardActions.containsKey(CardAction.ZOOS);

                features[idx++] = handZoos ? 1 : 0;
                features[idx++] = playedZoos ? 1 : 0;

                features[idx++] = playedZoos ? player.getCardResourcesCount().get(Zoos.class) : 0;
                features[idx++] = (handZoos || playedZoos) ? game.getMilestones().stream().filter(milestone -> !milestone.isAchieved()).count() : 0;
            }

            {
                boolean handDevelopedInfrastructure = handCardClasses.contains(DevelopedInfrastructure.class);
                boolean playedDevelopedInfrastructure = playedCardClasses.contains(DevelopedInfrastructure.class);

                features[idx++] = handDevelopedInfrastructure ? 1 : 0;
                features[idx++] = playedDevelopedInfrastructure ? 1 : 0;

                features[idx++] = (playedDevelopedInfrastructure || handDevelopedInfrastructure) ? temperatureLeftRatio : 0;

                int blueCardsCount = (int) player.getPlayed().getCards().stream()
                        .map(cardService::getCard)
                        .filter(c -> c.getColor() == CardColor.BLUE)
                        .limit(5)
                        .count();

                features[idx++] = (handDevelopedInfrastructure || playedDevelopedInfrastructure) && !game.getPlanet().isTemperatureMax() ? (10 - (blueCardsCount >= 5 ? 5 : 0)) : 0;
            }

            {
                boolean handGCR = handCardClasses.contains(GasCooledReactors.class);
                boolean playedGCR = playedCardClasses.contains(GasCooledReactors.class);

                features[idx++] = handGCR ? 1 : 0;
                features[idx++] = playedGCR ? 1 : 0;

                features[idx++] = (handGCR || playedGCR) ? temperatureLeftRatio : 0;
                features[idx++] = (handGCR || playedGCR) && !game.getPlanet().isTemperatureMax() ? (12 - player.getPhaseCards().stream().filter(phaseCard -> phaseCard != 0).count() * 2) : 0;
            }

            {
                boolean handWI = handCardClasses.contains(WaterImportFromEuropa.class);
                boolean playedWI = playedCardClasses.contains(WaterImportFromEuropa.class);

                boolean handIMI = handCardClasses.contains(IoMiningIndustries.class);
                boolean playedIMI = playedCardClasses.contains(IoMiningIndustries.class);

                boolean handTG = handCardClasses.contains(TerraformingGanymede.class);

                features[idx++] = handWI ? 1 : 0;
                features[idx++] = playedWI ? 1 : 0;

                features[idx++] = handIMI ? 1 : 0;
                features[idx++] = playedIMI ? 1 : 0;

                features[idx++] = playedWI || handWI ? oceansLeftRatio : 0;

                features[idx++] = (handWI || playedWI) && !game.getPlanet().isOceansMax() ? Math.max(0, 12 - player.getTitaniumIncome()) : 0;

                int playedJupiterTagCount = playedTagToCount.getOrDefault(Tag.JUPITER, 0L).intValue();
                int handJupiterTagCount = handTagCounts[Tag.JUPITER.ordinal()];

                features[idx++] = ((handWI || playedWI) ? playedJupiterTagCount : 0) + ((handIMI || playedIMI) ? playedJupiterTagCount : 0);
                features[idx++] = ((handWI || playedWI) ? handJupiterTagCount : 0) + ((handIMI || playedIMI) ? handJupiterTagCount : 0);
                features[idx++] = handTG ? playedJupiterTagCount : 0;
            }

            {
                boolean handPP = handCardClasses.contains(ProgressivePolicies.class);
                boolean playedPP = playedCardClasses.contains(ProgressivePolicies.class);

                features[idx++] = handPP ? 1 : 0;
                features[idx++] = playedPP ? 1 : 0;

                features[idx++] = playedPP || handPP ? oxygenLeftRatio : 0;

                int eventCardsCount = playedTagToCount.getOrDefault(Tag.EVENT, 0L).intValue();
                features[idx++] = (handPP || playedPP) && !game.getPlanet().isOxygenMax() ? (10 - (eventCardsCount >= 4 ? 5 : 0)) : 0;
            }

            {
                boolean handIW = handCardClasses.contains(IronWorks.class);
                boolean playedIW = playedCardClasses.contains(IronWorks.class);

                features[idx++] = handIW ? 1 : 0;
                features[idx++] = playedIW ? 1 : 0;
                features[idx++] = (playedIW || handIW) ? oxygenLeftRatio : 0;
            }

            {
                boolean handSW = handCardClasses.contains(Steelworks.class);
                boolean playedSW = playedCardClasses.contains(Steelworks.class);

                features[idx++] = handSW ? 1 : 0;
                features[idx++] = playedSW ? 1 : 0;
                features[idx++] = (playedSW || handSW) ? oxygenLeftRatio : 0;
            }

            {
                boolean handSP = handCardClasses.contains(SolarPunk.class);
                boolean playedSP = playedCardClasses.contains(SolarPunk.class);

                features[idx++] = handSP ? 1 : 0;
                features[idx++] = playedSP ? 1 : 0;

                features[idx++] = playedSP || handSP ? oxygenLeftRatio : 0;
                features[idx++] = (handSP || playedSP) ? Math.max(0, 15 - player.getTitaniumIncome() * 2) : 0;

                //доход говорит о том, что мы в теории можем жать эту карту даже без скидки
                features[idx++] = (handSP || playedSP) ? (float) Math.log1p(player.getMcIncome() + player.getTerraformingRating()) : 0;
            }

            {
                boolean handST = handCardClasses.contains(StandardTechnology.class);
                boolean playedST = playedCardClasses.contains(StandardTechnology.class);

                features[idx++] = handST ? 1 : 0;
                features[idx++] = playedST ? 1 : 0;

                float totalProgress = game.getPlanet().oceansLeft() + game.getPlanet().temperatureLeft() + game.getPlanet().oxygenLeft();
                features[idx++] = handST || playedST ? totalProgress : 0;

                features[idx++] = (handST || playedST) ? (float) Math.log1p(player.getMcIncome() + player.getTerraformingRating()) : 0;
            }

            {
                boolean handBirds = handCardClasses.contains(Birds.class);
                boolean playedBirds = playedCardClasses.contains(Birds.class);

                features[idx++] = handBirds ? 1 : 0;
                features[idx++] = playedBirds ? 1 : 0;

                int currentOxygenLevel = game.getPlanet().getCurrentLevel(GlobalParameter.OXYGEN);
                int targetOxygenLevel = getOxygenStepFromColor(canAmplifyOxygenOrTemperature ? ParameterColor.Y : ParameterColor.W, false);

                float availabilityProgress = (currentOxygenLevel >= targetOxygenLevel) ? 1.0f : ((float) currentOxygenLevel / targetOxygenLevel);
                features[idx++] = handBirds ? availabilityProgress : 0;
            }

            {
                boolean handTardigrades = handCardClasses.contains(Tardigrades.class);
                boolean playedTardigrades = playedCardClasses.contains(Tardigrades.class);

                features[idx++] = handTardigrades ? 1 : 0;
                features[idx++] = playedTardigrades ? 1 : 0;
            }

            {//good example
                boolean handPhysicsComplex = handCardActions.containsKey(CardAction.PHYSICS_COMPLEX);
                boolean playedPhysicsComplex = playedCardActions.containsKey(CardAction.PHYSICS_COMPLEX);

                features[idx++] = handPhysicsComplex ? 1 : 0;
                features[idx++] = playedPhysicsComplex ? 1 : 0;

                features[idx++] = playedPhysicsComplex || handPhysicsComplex ? temperatureLeftRatio : 0;

                features[idx++] = handPhysicsComplex && science >= 4 ? 1 : 0;
                features[idx++] = handPhysicsComplex ? Math.min(1f, science / 4f) : 0;
            }

            {//good example
                boolean plantation = handCardClasses.contains(Plantation.class);
                features[idx++] = plantation ? 1 : 0;
                features[idx++] = plantation && science >= 4 ? 1 : 0;
                features[idx++] = plantation ? Math.min(1f, science / 4f) : 0;
            }

            {
                boolean scienceHand = handCardClasses.contains(MarsUniversity.class);
                boolean scienceTable = playedCardClasses.contains(MarsUniversity.class);

                if (scienceHand || scienceTable) {
                    features[idx++] = scienceTable ? 1.0f : 0.0f; // Активен сейчас
                    features[idx++] = scienceHand ? 1.0f : 0.0f;  // Можно активировать скоро

                    features[idx++] = (float) handTagCounts[Tag.SCIENCE.ordinal()];
                    features[idx++] = (float) handTagCounts[Tag.PLANT.ordinal()];

                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean apolloCorp = handCardClasses.contains(ApolloIndustriesCorporation.class);
                boolean conferenceHand = handCardClasses.contains(OlympusConference.class);
                boolean conferenceTable = playedCardClasses.contains(OlympusConference.class);

                features[idx++] = handTagCounts[Tag.SCIENCE.ordinal()];
                features[idx++] = ((apolloCorp ? 1 : 0) + (conferenceTable ? 1 : 0)) / 2f;
                features[idx++] = conferenceHand ? 1 : 0;
            }

            {
                boolean matterGenOnTable = playedCardActions.containsKey(CardAction.MATTER_GENERATOR);
                boolean matterGenInHand = handCardActions.containsKey(CardAction.MATTER_GENERATOR);

                features[idx++] = matterGenOnTable ? 1.0f : 0.0f;
                features[idx++] = matterGenInHand ? 1.0f : 0.0f;
                features[idx++] = (matterGenOnTable || matterGenInHand) ? hand.size() : 0;
            }

            {
                boolean tableRC = playedCardActions.containsKey(CardAction.REDRAFTED_CONTRACTS);
                boolean handRC = handCardActions.containsKey(CardAction.REDRAFTED_CONTRACTS);
                boolean tableSS = playedCardActions.containsKey(CardAction.SOFTWARE_STREAMLINING);
                boolean handSS = handCardActions.containsKey(CardAction.SOFTWARE_STREAMLINING);

                features[idx++] = tableRC ? 1.0f : 0.0f;
                features[idx++] = handRC ? 1.0f : 0.0f;
                features[idx++] = tableSS ? 1.0f : 0.0f;
                features[idx++] = handSS ? 1.0f : 0.0f;

                if (tableRC || handRC || tableSS || handSS) {
                    // Больше карт = выше шанс, что среди них есть 3 бесполезных.
                    float handSizeFactor = Math.min(1.0f, player.getHand().size() / 10.0f);

                    // 2. Фактор "голода" (Card Income)
                    // Если доход карт +3 или +4, нам обмен не так важен (мы и так много тянем).
                    // Если доход карт 0 или 1, обмен 3-на-3 критически важен для поиска решений.
                    float hungerFactor = Math.max(0.0f, 1.0f - (player.getCardIncome() / 4.0f));

                    // Итоговый потенциал: высок, когда рука полная, а приток новых карт слабый.
                    float cyclingUtility = handSizeFactor * hungerFactor;

                    features[idx++] = tableRC || handRC ? cyclingUtility : 0;
                    features[idx++] = tableSS || handSS ? cyclingUtility : 0;
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean handAJ = handCardClasses.contains(ArtificialJungle.class);
                boolean tableAJ = playedCardClasses.contains(ArtificialJungle.class);

                boolean handWBS = handCardActions.containsKey(CardAction.WOOD_BURNING_STOVES);
                boolean tableWBS = playedCardActions.containsKey(CardAction.WOOD_BURNING_STOVES);

                features[idx++] = handAJ ? 1 : 0;
                features[idx++] = tableAJ ? 1 : 0;

                features[idx++] = (handWBS || tableWBS) ? temperatureLeftRatio : 0;

                if (handAJ || tableAJ || handWBS || tableWBS) {
                    features[idx++] = player.getPlants() > 0 ? 1 : 0;
                    features[idx++] = (float) Math.log1p(player.getPlants());
                    features[idx++] = player.getPlantsIncome() > 0 ? 1 : 0;
                    features[idx++] = (float) Math.log1p(player.getPlantsIncome());
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
                features[idx++] = (handWBS || tableWBS) && player.getPlants() >= 3 ? 1 : 0;
            }

            features[idx++] = handCardActions.containsKey(CardAction.HYDRO_ELECTRIC) ? 1 : 0;
            features[idx++] = playedCardActions.containsKey(CardAction.HYDRO_ELECTRIC) ? 1 : 0;

            // ===============================================
            // БЛОК 4: TOTAL AGGREGATING PARAMETERS
            // ===============================================

            float Table_Plant_Action_Output = getTotalPlantActionOutput(playedCardActions);
            features[idx++] = Table_Plant_Action_Output;

            float Hand_Plant_Action_Output = getTotalPlantActionOutput(handCardActions);
            features[idx++] = Hand_Plant_Action_Output;

            //summarize all cards that give VP per microbe/animal entity
            float VP_RESOURCE_SENSITIVITY_HAND = (float) CARDS_WITH_VP_PER_RESOURCE.entrySet().stream()
                    .filter(e -> handCardClasses.contains(e.getKey()))
                    .mapToDouble(Map.Entry::getValue)
                    .sum();
            features[idx++] = VP_RESOURCE_SENSITIVITY_HAND;

            float VP_RESOURCE_SENSITIVITY_TABLE = (float) CARDS_WITH_VP_PER_RESOURCE.entrySet().stream()
                    .filter(e -> playedCardClasses.contains(e.getKey()))
                    .mapToDouble(Map.Entry::getValue)
                    .sum();
            features[idx++] = VP_RESOURCE_SENSITIVITY_TABLE;

            float[] producingConsumingMicrobes = handleProducingConsumingMicrobeSynergy();//playedProducingMicrobeCards, playedConsumingMicrobeCards, handProducingMicrobeCards, handConsumingMicrobeCards
            float playedProducingMicrobeCards = producingConsumingMicrobes[0];
            float playedConsumingMicrobeCards = producingConsumingMicrobes[1];
            float handProducingMicrobeCards = producingConsumingMicrobes[2];
            float handConsumingMicrobeCards = producingConsumingMicrobes[3];

            features[idx++] = playedProducingMicrobeCards;
            features[idx++] = playedConsumingMicrobeCards;
            features[idx++] = handProducingMicrobeCards;
            features[idx++] = handConsumingMicrobeCards;

            features[idx++] = directlyProducingMicrobeCardsCount(playedCardActions);
            features[idx++] = directlyProducingMicrobeCardsCount(handCardActions);

            {
                boolean filterFeedersInHand = handCardActions.containsKey(CardAction.FILTER_FEEDERS);
                boolean filterFeedersPlayed = playedCardActions.containsKey(CardAction.FILTER_FEEDERS);

                features[idx++] = (filterFeedersPlayed ? 1 : 0);
                features[idx++] = (filterFeedersInHand ? 1 : 0);

                boolean canBuild = game.getPlanet().getRevealedOceans().size() >= 2;
                features[idx++] = (filterFeedersInHand && canBuild ? 1 : 0);
                features[idx++] = (filterFeedersPlayed || filterFeedersInHand) ? handConsumingMicrobeCards : 0;
                features[idx++] = (filterFeedersPlayed || filterFeedersInHand) ? playedConsumingMicrobeCards : 0;
            }

            {
                boolean handECF = handCardClasses.contains(ExtremeColdFungus.class);
                boolean playedECF = playedCardClasses.contains(ExtremeColdFungus.class);

                features[idx++] = handECF ? 1 : 0;
                features[idx++] = playedECF ? 1 : 0;

                int currentTemp = game.getPlanet().getCurrentLevel(GlobalParameter.TEMPERATURE);
                int maxTemp = getTemperatureStepFromColor(canAmplifyOxygenOrTemperature ? ParameterColor.R : ParameterColor.P, true);

                float utilityWindow = currentTemp <= maxTemp ? (float) (maxTemp - currentTemp + 1) : 0;
                features[idx++] = (handECF || playedECF) ? utilityWindow : 0;

                features[idx++] = (handECF || playedECF) ? handConsumingMicrobeCards : 0;
                features[idx++] = (handECF || playedECF) ? playedConsumingMicrobeCards : 0;
            }

            {
                boolean handDF = handCardClasses.contains(DecomposingFungus.class);
                boolean playedDF = playedCardClasses.contains(ExtremeColdFungus.class);

                features[idx++] = handDF ? 1 : 0;
                features[idx++] = playedDF ? 1 : 0;

                features[idx++] = (handDF || playedDF) ? handProducingMicrobeCards : 0;
                features[idx++] = (handDF || playedDF) ? playedProducingMicrobeCards : 0;
            }

            {
                boolean handCB = handCardClasses.contains(ConservedBiome.class);
                boolean playedCB = playedCardClasses.contains(ConservedBiome.class);

                features[idx++] = handCB ? 1 : 0;
                features[idx++] = playedCB ? 1 : 0;

                features[idx++] = (handCB || playedCB) ? handConsumingMicrobeCards : 0;
                features[idx++] = (handCB || playedCB) ? playedConsumingMicrobeCards : 0;

                features[idx++] = (handCB || playedCB) ? countVpPerAnimalCards(handCardClasses) : 0;
                features[idx++] = (handCB || playedCB) ? vpFromBestAnimalCard(playedCardClasses) : 0;

                features[idx++] = handCB ? player.getForests() : 0;
            }

            {
                boolean handSF = handCardClasses.contains(SymbioticFungus.class);
                boolean playedSF = playedCardClasses.contains(SymbioticFungus.class);

                features[idx++] = handSF ? 1 : 0;
                features[idx++] = playedSF ? 1 : 0;

                int currentTemp = game.getPlanet().getCurrentLevel(GlobalParameter.TEMPERATURE);
                int minTemp = getTemperatureStepFromColor(canAmplifyOxygenOrTemperature ? ParameterColor.P : ParameterColor.R, false); // Минимум для старта
                float availability = currentTemp >= minTemp ? 1.0f : (float) currentTemp / minTemp;
                features[idx++] = handSF ? availability : 0;

                features[idx++] = (handSF || playedSF) ? handConsumingMicrobeCards : 0;
                features[idx++] = (handSF || playedSF) ? playedConsumingMicrobeCards : 0;
            }

            {
                boolean handViralEnhancers = handCardClasses.contains(ViralEnhancers.class);
                boolean playedViralEnhancers = playedCardClasses.contains(ViralEnhancers.class);

                features[idx++] = handViralEnhancers ? 1 : 0;
                features[idx++] = playedViralEnhancers ? 1 : 0;

                int cardPower = handTagCounts[Tag.PLANT.ordinal()] + handTagCounts[Tag.MICROBE.ordinal()] + handTagCounts[Tag.ANIMAL.ordinal()];

                if (handViralEnhancers || playedViralEnhancers) {
                    features[idx++] = cardPower;
                    features[idx++] = (playedConsumingMicrobeCards + handConsumingMicrobeCards) > 0 ? 1 : 0;
                    features[idx++] = (vpFromBestAnimalCard(playedCardClasses) > 0 || vpFromBestAnimalCard(handCardClasses) > 0) ? 1 : 0;
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }


            for (int i = 0; i < 5; i++) {
                features[idx++] = player.getPhaseCards().get(i) == 1 ? 1 : 0;//first upgrade
                features[idx++] = player.getPhaseCards().get(i) == 2 ? 1 : 0;//second upgrade
            }
            for (int i = 0; i < 5; i++) {
                features[idx++] = anotherPlayer.getPhaseCards().get(i) == 1 ? 1 : 0;//first upgrade
                features[idx++] = anotherPlayer.getPhaseCards().get(i) == 2 ? 1 : 0;//second upgrade
            }

            int globalUpgrades = (int) (handCardActions.getOrDefault(CardAction.UPDATE_PHASE_CARD, 0L) + 2 * handCardActions.getOrDefault(CardAction.UPDATE_PHASE_CARD_TWICE, 0L) + handCardActions.getOrDefault(CardAction.CRYOGENIC_SHIPMENT, 0L));
            int phase1Upgrades = globalUpgrades + handCardActions.getOrDefault(CardAction.UPDATE_PHASE_1_CARD, 0L).intValue();
            int phase2Upgrades = globalUpgrades + handCardActions.getOrDefault(CardAction.UPDATE_PHASE_2_CARD, 0L).intValue();
            int phase3Upgrades = globalUpgrades + handCardActions.getOrDefault(CardAction.COMMUNICATIONS_STREAMLINING, 0L).intValue();
            int phase4Upgrades = globalUpgrades + handCardActions.getOrDefault(CardAction.UPDATE_PHASE_4_CARD, 0L).intValue();

            features[idx++] = phase1Upgrades > 0 ? 1 : 0;
            features[idx++] = phase1Upgrades > 1 ? 1 : 0;
            features[idx++] = phase2Upgrades > 0 ? 1 : 0;
            features[idx++] = phase2Upgrades > 1 ? 1 : 0;
            features[idx++] = phase3Upgrades > 0 ? 1 : 0;
            features[idx++] = phase3Upgrades > 1 ? 1 : 0;
            features[idx++] = phase4Upgrades > 0 ? 1 : 0;
            features[idx++] = phase4Upgrades > 1 ? 1 : 0;
            features[idx++] = globalUpgrades > 0 ? 1 : 0;
            features[idx++] = globalUpgrades > 1 ? 1 : 0;

            //discounts
            features[idx++] = (handCardClasses.contains(EarthCatapult.class) ? 2 : 0) + (handCardClasses.contains(ResearchOutpost.class) ? 1 : 0) + (handCardClasses.contains(HohmannTransferShipping.class) ? 1 : 0);
            features[idx++] = (playedCardClasses.contains(EarthCatapult.class) ? 2 : 0) + (playedCardClasses.contains(ResearchOutpost.class) ? 1 : 0) + (playedCardClasses.contains(HohmannTransferShipping.class) ? 1 : 0);

            {
                boolean hasThorgateCorp = playedCardActions.containsKey(CardAction.THORGATE_CORPORATION);//-3 mc

                boolean subsidesInHand = handCardActions.containsKey(CardAction.ENERGY_SUBSIDIES);//-4 mc and a card
                boolean subsidesPlayed = playedCardActions.containsKey(CardAction.ENERGY_SUBSIDIES);

                features[idx++] = subsidesInHand ? 1.0f : 0.0f;//need to track because effect also gives a card
                features[idx++] = subsidesPlayed ? 1.0f : 0.0f;

                float activeDiscount = (hasThorgateCorp ? 3.0f : 0.0f) + (subsidesPlayed ? 4.0f : 0.0f);
                float potentialDiscount = subsidesInHand ? 4.0f : 0.0f;

                features[idx++] = activeDiscount;

                float energyCostInHand = getTagTotalCostInHand(Tag.ENERGY);
                features[idx++] = energyCostInHand;

                features[idx++] = (float) ((activeDiscount + potentialDiscount) * Math.log1p(energyCostInHand));
            }

            {
                boolean playedTeractor = playedCardActions.containsKey(CardAction.TERACTOR_CORPORATION);//-3 mc per earth
                boolean playedConference = playedCardActions.containsKey(CardAction.INTERPLANETARY_CONFERENCE);//-3 mc and card per earth/jupiter
                boolean handConference = handCardActions.containsKey(CardAction.INTERPLANETARY_CONFERENCE);

                features[idx++] = handConference ? 1.0f : 0.0f;
                features[idx++] = playedConference ? 1.0f : 0.0f;

                float currentEarthDiscount = 0;
                float currentJupiterDiscount = 0;

                if (playedTeractor) {
                    currentEarthDiscount += 3.0f;
                }
                if (playedConference) {
                    currentEarthDiscount += 3.0f;
                    currentJupiterDiscount += 3.0f;
                }

                features[idx++] = currentEarthDiscount;
                features[idx++] = currentJupiterDiscount;

                float earthCostInHand = getTagTotalCostInHand(Tag.EARTH);
                float jupiterCostInHand = getTagTotalCostInHand(Tag.JUPITER);

                features[idx++] = handConference ? (float) Math.log1p(earthCostInHand) : 0f;
                features[idx++] = handConference ? (float) Math.log1p(jupiterCostInHand) : 0f;

                features[idx++] = currentEarthDiscount > 0 ? (float) Math.log1p(earthCostInHand) : 0f;
                features[idx++] = currentJupiterDiscount > 0 ? (float) Math.log1p(jupiterCostInHand) : 0f;
            }

            {
                boolean corpOnTable = playedCardActions.containsKey(CardAction.INTERPLANETARY_CINEMATICS); // -2 mc per event

                boolean card2InHand = handCardActions.containsKey(CardAction.MEDIA_GROUP); //-5 mc per event
                boolean card2OnTable = playedCardActions.containsKey(CardAction.MEDIA_GROUP); //-5 mc per event

                boolean card3InHand = handCardActions.containsKey(CardAction.OPTIMAL_AEROBRAKING); //+2 heat +2 plants
                boolean card3OnTable = playedCardActions.containsKey(CardAction.OPTIMAL_AEROBRAKING); //+2 heat +2 plants

                boolean card4InHand = handCardActions.containsKey(CardAction.RECYCLED_DETRITUS); //+2 cards per event
                boolean card4OnTable = playedCardActions.containsKey(CardAction.RECYCLED_DETRITUS);//+2 cards per event

                boolean card5InHand = handCardActions.containsKey(CardAction.IMPACT_ANALYSIS); //+1 card
                boolean card5OnTable = playedCardActions.containsKey(CardAction.IMPACT_ANALYSIS); //+1 card

                float totalEventMcDiscount = ((corpOnTable ? 2.0f : 0) + (card2OnTable ? 5.0f : 0.0f)) / 7.0f;
                float totalPotentialMcDiscount = card2InHand ? 1 : 0;
                features[idx++] = totalEventMcDiscount;
                features[idx++] = totalPotentialMcDiscount;

                features[idx++] = card3InHand ? 1 : 0;//+2 heat + 2 plants
                features[idx++] = card3OnTable ? 1 : 0;//+2 heat + 2 plants

                float totalCardDrawBonus = ((card4OnTable ? 2.0f : 0.0f) + (card5OnTable ? 1.0f : 0.0f)) / 3.0f;
                float potentialCardDrawBonus = ((card4InHand ? 2.0f : 0.0f) + (card5InHand ? 1.0f : 0.0f)) / 3.0f;
                features[idx++] = totalCardDrawBonus;
                features[idx++] = potentialCardDrawBonus;

                int eventTagsInHand = handTagCounts[Tag.EVENT.ordinal()];
                features[idx++] = (corpOnTable || card2OnTable || card3OnTable || card4OnTable || card5OnTable) ? eventTagsInHand : 0;
                features[idx++] = (card2InHand || card3InHand || card4InHand || card5InHand) ? eventTagsInHand : 0;
            }

            {
                boolean handOutpost = handCardActions.containsKey(CardAction.ORBITAL_OUTPOST);
                boolean playedOO = playedCardActions.containsKey(CardAction.ORBITAL_OUTPOST);

                features[idx++] = handOutpost ? 1 : 0;
                features[idx++] = playedOO ? 1 : 0;

                features[idx++] = (handOutpost || playedOO) ? hand.stream().filter(card -> card.getTags().size() <= 1).count() : 0;
            }

            {
                features[idx++] = playedCardActions.containsKey(CardAction.RESEARCH_GRANT) ? 1 : 0;
                features[idx++] = handCardActions.containsKey(CardAction.RESEARCH_GRANT) ? 1 : 0;
            }

            {
                features[idx++] = playedCardActions.containsKey(CardAction.CELESTIOR_CORPORATION) ? 1 : 0;
            }

            {
                features[idx++] = playedCardActions.containsKey(CardAction.MODPRO_CORPORATION) ? 1 : 0;
            }

            {
                boolean corpOnTable = playedCardActions.containsKey(CardAction.DEVTECHS_CORPORATION);

                if (corpOnTable) {
                    float totalCostInHand = 0f;
                    int count = 0;
                    for (Card c : hand) {
                        if (c.getColor() == CardColor.GREEN) {
                            totalCostInHand += c.getPrice();
                            count++;
                        }
                    }

                    features[idx++] = 1;
                    features[idx++] = count;
                    features[idx++] = (float) Math.log1p(totalCostInHand);
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean corpOnTable = playedCardActions.containsKey(CardAction.SULTIRA_CORPORATION);

                if (corpOnTable) {
                    float totalCostInHand = 0f;
                    for (Card c : hand) {
                        if (c.getTags().contains(Tag.ENERGY)) {
                            totalCostInHand += c.getPrice();
                        }
                    }

                    features[idx++] = 1;
                    features[idx++] = handTagCounts[Tag.ENERGY.ordinal()];
                    features[idx++] = (float) Math.log1p(totalCostInHand);
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean corpOnTable = playedCardActions.containsKey(CardAction.LAUNCH_STAR_CORPORATION);

                if (corpOnTable) {
                    float totalCostInHand = 0f;
                    int count = 0;
                    for (Card c : hand) {
                        if (c.getColor() == CardColor.BLUE) {
                            totalCostInHand += c.getPrice();
                            count++;
                        }
                    }

                    features[idx++] = 1;
                    features[idx++] = count;
                    features[idx++] = (float) Math.log1p(totalCostInHand);
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean corpOnTable = playedCardActions.containsKey(CardAction.CREDICOR_CORPORATION);

                if (corpOnTable) {
                    float totalCostInHand = 0f;
                    int count = 0;
                    for (Card c : hand) {
                        if (c.getPrice() >= 20) {
                            totalCostInHand += c.getPrice();
                            count++;
                        }
                    }

                    features[idx++] = 1;
                    features[idx++] = count;
                    features[idx++] = (float) Math.log1p(totalCostInHand);
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean corpOnTable = playedCardActions.containsKey(CardAction.ARCLIGHT_CORPORATION);

                if (corpOnTable) {
                    features[idx++] = 1;
                    features[idx++] = handTagCounts[Tag.ANIMAL.ordinal()] + handTagCounts[Tag.PLANT.ordinal()] + handTagCounts[Tag.MICROBE.ordinal()];
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean handEZ = handCardActions.containsKey(CardAction.ECOLOGICAL_ZONE);
                boolean tableEZ = playedCardActions.containsKey(CardAction.ECOLOGICAL_ZONE);

                features[idx++] = handEZ ? 1 : 0;
                features[idx++] = tableEZ ? 1 : 0;

                features[idx++] = (handEZ || tableEZ) ? handTagCounts[Tag.ANIMAL.ordinal()] + handTagCounts[Tag.PLANT.ordinal()] : 0;
            }

            {
                boolean handBA = handCardActions.containsKey(CardAction.BACTERIAL_AGGREGATES);
                boolean tableBA = playedCardActions.containsKey(CardAction.BACTERIAL_AGGREGATES);

                features[idx++] = handBA ? 1 : 0;
                features[idx++] = tableBA ? 1 : 0;

                features[idx++] = tableBA ? player.getCardResourcesCount().get(BacterialAggregates.class) : 0;
                features[idx++] = (handBA || tableBA) ? handTagCounts[Tag.EARTH.ordinal()] : 0;
            }

            {
                int extraCardsToDraftPotential = 0;
                int extraCardsToTakePotential = 0;

                if (handCardActions.containsKey(CardAction.INTERPLANETARY_RELATIONS)) {
                    extraCardsToDraftPotential++;
                    extraCardsToTakePotential++;
                }

                if (handCardActions.containsKey(CardAction.INTERNS)) {
                    extraCardsToDraftPotential += 2;
                }

                if (handCardActions.containsKey(CardAction.UNITED_PLANETARY)) {
                    extraCardsToDraftPotential++;
                    extraCardsToTakePotential++;
                }

                if (handCardActions.containsKey(CardAction.BACTERIAL_AGGREGATES)) {
                    extraCardsToDraftPotential++;
                }

                if (handCardActions.containsKey(CardAction.EXTENDED_RESOURCES)) {
                    extraCardsToTakePotential++;
                }

                DraftCardsDto draftCardsDto = draftCardsService.countCardsToTakeAndDraft(player);

                features[idx++] = draftCardsDto.getCardsToSee();
                features[idx++] = draftCardsDto.getCardsToTake();

                features[idx++] = extraCardsToDraftPotential;
                features[idx++] = extraCardsToTakePotential;
            }

            {
                features[idx++] = handCardActions.containsKey(CardAction.INTERPLANETARY_RELATIONS) ? 1 : 0;
                features[idx++] = playedCardActions.containsKey(CardAction.INTERPLANETARY_RELATIONS) ? 1 : 0;
                features[idx++] = handCardActions.containsKey(CardAction.INTERPLANETARY_RELATIONS) ? (player.getPlayed().size() - 1) : 0;//we get 1vp per 4 cards, this will be normalized, describe only potential
            }

            {
                boolean handIS = handCardClasses.contains(ImmigrationShuttles.class);
                boolean playedIS = playedCardClasses.contains(ImmigrationShuttles.class);

                features[idx++] = handIS ? 1 : 0;
                features[idx++] = playedIS ? 1 : 0;
                features[idx++] = playedIS ? handTagCounts[Tag.EARTH.ordinal()] : 0;
                features[idx++] = handIS ? handTagCounts[Tag.EARTH.ordinal()] : 0;
                features[idx++] = handIS ? earth : 0;
            }

            features[idx++] = handCardClasses.contains(BeamFromThoriumAsteroid.class) ? Math.max(0, handTagCounts[Tag.JUPITER.ordinal()] - 1) : 0;
            features[idx++] = handCardClasses.contains(BeamFromThoriumAsteroid.class) && playedJupiters >= 1 ? 1 : 0;

            features[idx++] = handCardClasses.contains(FusionPower.class) ? Math.max(0, handTagCounts[Tag.ENERGY.ordinal()] - 1) : 0;
            features[idx++] = handCardClasses.contains(FusionPower.class) && playedEnergy >= 2 ? 1 : 0;


            // ===============================================
            // БЛОК 4: REQUIREMENTS & PLAYABILITY (50 features) [130-179]
            // ===============================================


            // --- 4.4 Oxygen requirements ---


            extractOxygenRequirements(features, idx, canAmplifyOxygenOrTemperature);
            idx += 7;

            boolean potentialAmplify = canAmplifyOxygenOrTemperature || handCardClasses.contains(AdaptationTechnology.class);
            extractOxygenRequirements(features, idx, potentialAmplify);
            idx += 7;

            extractTemperatureRequirements(features, idx, canAmplifyOxygenOrTemperature);
            idx += 7;

            extractTemperatureRequirements(features, idx, potentialAmplify);
            idx += 7;

            extractOceanRequirements(features, idx);//+4 параметра
            idx += 3;

            features[idx++] = oxygenLeftRatio;
            features[idx++] = 1f - oxygenLeftRatio;

            features[idx++] = temperatureLeftRatio;
            features[idx++] = 1f - temperatureLeftRatio;

            features[idx++] = oceansLeftRatio;
            features[idx++] = 1f - oceansLeftRatio;

            float totalProgress = game.getPlanet().oceansBuilt() + game.getPlanet().getCurrentLevel(GlobalParameter.TEMPERATURE) + game.getPlanet().getCurrentLevel(GlobalParameter.OXYGEN);
            features[idx++] = totalProgress;

            // --- 4.1 Science requirements ---
            List<Float> scienceReqs = extract((Card c) -> {
                List<Tag> tagRequirements = c.getTagRequirements();
                if (tagRequirements.contains(Tag.SCIENCE)) {
                    return (float) tagRequirements.size();
                }
                return 0f;
            });
            FeatureStats scienceReqStats = computeStats(scienceReqs);

            features[idx++] = scienceReqStats.max / 5.0f;  // [130] Highest req
            features[idx++] = scienceReqStats.mean / 5.0f; // [131] Average req
            features[idx++] = scienceReqStats.nonZeroCount / 9.0f; // [132] Cards with req

            // Gap: насколько далеки от выполнения
            float scienceGap = 0;
            for (Card card : hand) {
                if (card.getTagRequirements().contains(Tag.SCIENCE)) {
                    scienceGap += Math.max(0, card.getTagRequirements().size() - science);
                }
            }
            features[idx++] = scienceGap;           // [134] Total gap

            if (handCardClasses.contains(AdvancedEcosystems.class)) {
                features[idx++] = 1;

                int satisfied = 0;
                if (playedTagToCount.getOrDefault(Tag.PLANT, 0L) > 0) satisfied++;
                if (playedTagToCount.getOrDefault(Tag.MICROBE, 0L) > 0) satisfied++;
                if (playedTagToCount.getOrDefault(Tag.ANIMAL, 0L) > 0) satisfied++;

                features[idx++] = satisfied / 3f;
            } else {
                features[idx++] = 0;
                features[idx++] = 0;
            }

            if (handCardClasses.contains(Crater.class)) {
                features[idx++] = 1;
                features[idx++] = Math.min(1, playedTagToCount.getOrDefault(Tag.EVENT, 0L) / 3f);
            } else {
                features[idx++] = 0;
                features[idx++] = 0;
            }

            {
                if (handCardClasses.contains(NitrogenRichAsteroid.class)) {
                    features[idx++] = 1;
                    features[idx++] = Math.min(4, playedTagToCount.getOrDefault(Tag.PLANT, 0L)) / 4f;
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                if (handCardClasses.contains(SyntheticCatastrophe.class)) {
                    features[idx++] = 1;
                    features[idx++] = redCount;
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                if (handCardClasses.contains(WorkCrews.class)) {
                    features[idx++] = 1;
                    features[idx++] = redCount + blueCount;
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            features[idx++] = (handCardClasses.contains(PrivateInvestorBeach.class)) && game.getMilestones().stream().anyMatch(milestone -> milestone.isAchieved(player)) ? 1 : 0;


            List<Milestone> milestones = game.getMilestones();
            for (Milestone m : milestones) {
                // 1. Мой прогресс (0.0 - 1.0)
                features[idx++] = (float) m.getValue(player, cardService) / m.getMaxValue();

                // 2. Прогресс оппонента
                features[idx++] = (float) m.getValue(anotherPlayer, cardService) / m.getMaxValue();

                boolean achieved = m.isAchieved();
                features[idx++] = achieved ? 1.0f : 0.0f;
                features[idx++] = achieved ? 0 : handEncoderHelperService.countMilestoneProgressByHand(m, hand, handTagCounts, gainTypeToCounts, incomeTypeToCounts);
            }

            List<BaseAward> awards = game.getAwards();
            for (BaseAward award : awards) {
                float value = handEncoderHelperService.countAwardProgressByHand(award, hand, handTagCounts);
                features[idx++] = value > 0 ? (float) Math.log(value) : 0;
            }

            {
                boolean advancedAlloysInHand = handCardClasses.contains(AdvancedAlloys.class);
                boolean advancedAlloysOnTable = playedCardClasses.contains(AdvancedAlloys.class);
                boolean phobologCorp = playedCardActions.containsKey(CardAction.PHOBOLOG_CORPORATION);

                float steelUnitValue = 2.0f + (advancedAlloysOnTable ? 1.0f : 0.0f);
                float titaniumUnitValue = 3.0f + (advancedAlloysOnTable ? 1.0f : 0.0f) + (phobologCorp ? 1.0f : 0.0f);

                float steelBuyingPower = player.getSteelIncome() * steelUnitValue;
                float titaniumBuyingPower = player.getTitaniumIncome() * titaniumUnitValue;

                features[idx++] = (float) Math.log1p(steelBuyingPower);
                features[idx++] = (float) Math.log1p(titaniumBuyingPower);

                int buildingTagsInHand = Math.max(0, handTagCounts[Tag.BUILDING.ordinal()] - (advancedAlloysInHand ? 1 : 0));
                int spaceTagsInHand = Math.max(0, handTagCounts[Tag.SPACE.ordinal()] - (advancedAlloysInHand ? 1 : 0));

                features[idx++] = (float) Math.log1p(steelBuyingPower * buildingTagsInHand);
                features[idx++] = (float) Math.log1p(titaniumBuyingPower * spaceTagsInHand);

                if (advancedAlloysInHand) {
                    float potentialSteelValue = 3.0f;
                    float potentialTitaniumValue = 4.0f + (phobologCorp ? 1.0f : 0.0f);

                    float potentialSteelPower = player.getSteelIncome() * potentialSteelValue;
                    float potentialTitaniumPower = player.getTitaniumIncome() * potentialTitaniumValue;

                    features[idx++] = (float) Math.log1p(potentialSteelPower);
                    features[idx++] = (float) Math.log1p(potentialTitaniumPower);
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            return features;
        }

        private List<Float> getNegativeHeatGainValuesOnBuild() {
            List<Float> result = null;
            for (Map.Entry<Class<?>, Float> entry : NEGATIVE_GAIN_CARDS.entrySet()) {
                if (handCardClasses.contains(entry.getKey())) {
                    if (result == null) {
                        result = new ArrayList<>(4);
                    }
                    result.add(entry.getValue());
                }
            }
            return result != null ? result : List.of();
        }

        private void handleEngineIncome(CardAction cardAction, float resourceCount, float[] features, int idx) {
            boolean cardInHand = handCardActions.containsKey(cardAction);
            boolean cardPlayed = playedCardActions.containsKey(cardAction);
            features[idx++] = cardInHand ? 1 : 0;
            features[idx++] = cardPlayed ? 1 : 0;
            features[idx++] = cardInHand ? resourceCount : 0;
            features[idx++] = cardPlayed ? resourceCount : 0;
        }

        private float getTagTotalCostInHand(Tag earth) {
            float totalCostInHand = 0f;
            for (Card c : hand) {
                if (c.getTags().contains(earth)) {
                    totalCostInHand += c.getPrice();
                }
            }
            return totalCostInHand;
        }

        private float getMinRedTemperatureAvailabilityProgress() {
            int currentLevel = game.getPlanet().getCurrentLevel(GlobalParameter.TEMPERATURE);
            int targetTemperatureLevel = getTemperatureStepFromColor(canAmplifyOxygenOrTemperature ? ParameterColor.P : ParameterColor.R, false);

            float availabilityProgress;
            if (currentLevel >= targetTemperatureLevel) {
                availabilityProgress = 1.0f; // Можно строить
            } else {
                availabilityProgress = (float) currentLevel / targetTemperatureLevel;
            }
            return availabilityProgress;
        }

        private float getMinYellowTemperatureAvailabilityProgress() {
            int currentLevel = game.getPlanet().getCurrentLevel(GlobalParameter.TEMPERATURE);
            int targetTemperatureLevel = getTemperatureStepFromColor(canAmplifyOxygenOrTemperature ? ParameterColor.R : ParameterColor.Y, false);

            float availabilityProgress;
            if (currentLevel >= targetTemperatureLevel) {
                availabilityProgress = 1.0f; // Можно строить
            } else {
                availabilityProgress = (float) currentLevel / targetTemperatureLevel;
            }
            return availabilityProgress;
        }

        private float getHeatConsumeCapacityOnActions(Map<CardAction, Long> cardActions) {
            float capacity = 0;
            if (cardActions.containsKey(CardAction.IRON_WORKS)) capacity += 4;
            if (cardActions.containsKey(CardAction.STEELWORKS)) capacity += 6;
            if (cardActions.containsKey(CardAction.GREEN_HOUSES)) capacity += 4;
            if (cardActions.containsKey(CardAction.CARETAKER_CONTRACT)) capacity += 8;
            if (cardActions.containsKey(CardAction.DEVELOPMENT_CENTER)) capacity += 2;
            return capacity;
        }

        private int getTotalPlantActionOutput(Map<CardAction, Long> cardActions) {
            return (cardActions.containsKey(CardAction.COMMUNITY_GARDENS) ? 1 : 0) +
                    (cardActions.containsKey(CardAction.DECOMPOSING_FUNGUS) ? 3 : 0) +
                    (cardActions.containsKey(CardAction.EXTREME_COLD_FUNGUS) ? 1 : 0) +
                    (cardActions.containsKey(CardAction.FARMERS_MARKET) ? 2 : 0) +
                    (cardActions.containsKey(CardAction.FARMING_COOPS) ? 3 : 0) +
                    (cardActions.containsKey(CardAction.GREEN_HOUSES) ? 4 : 0);
        }

        private float[] handleProducingConsumingMicrobeSynergy() {
            boolean temperatureMax = game.getPlanet().isTemperatureMax();
            boolean oxygenMax = game.getPlanet().isOxygenMax();
            boolean oceansMax = game.getPlanet().isOceansMax();

            long playedProducingMicrobeCards = playedCards.stream().filter(Card::producesMicrobe).count();
            long playedConsumingMicrobeCards = playedCards.stream().filter(Card::consumesMicrobe).count();

            long handProducingMicrobeCards = hand.stream().filter(Card::producesMicrobe).count();
            long handConsumingMicrobeCards = hand.stream().filter(Card::consumesMicrobe).count();

            if (playedCardClasses.contains(BacterialAggregates.class) && player.getCardResourcesCount().getOrDefault(BacterialAggregates.class, 0) >= 5) {
                playedConsumingMicrobeCards--;
            }

            if (handCardClasses.contains(ExtremeColdFungus.class) || handCardClasses.contains(BuffedExtremeColdFungus.class)) {
                ParameterColor temperatureColor = game.getPlanet().getTemperatureColor();
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

        private float vpFromBestAnimalCard(Set<Class<?>> cardClasses) {
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

        private float countVpPerAnimalCards(Set<Class<?>> cardClasses) {
            return ANIMAL_CARDS_VP_PER_RESOURCE.keySet().stream().filter(cardClasses::contains).count();
        }

        private int getImmediateMicrobeGainSum() {
            return (handCardActions.containsKey(CardAction.ASTROFARM) ? 2 : 0) + (handCardActions.containsKey(CardAction.IMPORTED_NITROGEN) ? 3 : 0);
        }

        private int getImmediatePlantGainCapacity() {
            return (handCardActions.containsKey(CardAction.IMPORTED_HYDROGEN) ? 3 : 0) + (handCardActions.containsKey(CardAction.LARGE_CONVOY) ? 5 : 0);
        }

        private int getImmediateAnimalGainCapacity() {
            return (handCardClasses.contains(CeosFavoriteProject.class) ? 2 : 0) + (handCardActions.containsKey(CardAction.IMPORTED_HYDROGEN) ? 2 : 0) + (handCardActions.containsKey(CardAction.LARGE_CONVOY) ? 3 : 0);
        }

        private int getImmediateMicrobeGainCapacity() {
            return (handCardClasses.contains(CeosFavoriteProject.class) ? 2 : 0) + (handCardActions.containsKey(CardAction.IMPORTED_HYDROGEN) ? 3 : 0) + (handCardActions.containsKey(CardAction.LOCAL_HEAT_TRAPPING) ? 2 : 0) + (handCardActions.containsKey(CardAction.CRYOGENIC_SHIPMENT) ? 3 : 0);
        }

        private int getImmediateAnimalGainSum() {
            return (handCardActions.containsKey(CardAction.EOS_CHASMA) ? 1 : 0) + (handCardActions.containsKey(CardAction.IMPORTED_NITROGEN) ? 2 : 0) + (handCardActions.containsKey(CardAction.LOCAL_HEAT_TRAPPING) ? 2 : 0) + (handCardActions.containsKey(CardAction.CRYOGENIC_SHIPMENT) ? 2 : 0);
        }

        private List<Float> extract(Function<Card, Float> extractor) {
            return hand.stream().map(extractor).collect(Collectors.toList());
        }

        private int getIncomeByType(Card card, GainType gainType) {
            List<Gain> projectIncomes = card.getCardMetadata().getIncomes();
            if (CollectionUtils.isEmpty(projectIncomes)) {
                return 0;
            }
            for (Gain projectIncome : projectIncomes) {
                if (projectIncome.getType() == gainType) {
                    return projectIncome.getValue();
                }
            }

            return 0;
        }

        private int getPositiveGainByType(Card card, GainType gainType) {
            List<Gain> bonuses = card.getCardMetadata().getBonuses();
            if (CollectionUtils.isEmpty(bonuses)) {
                return 0;
            }
            for (Gain bonus : bonuses) {
                if (bonus.getType() == gainType && bonus.getValue() > 0) {
                    return bonus.getValue();
                }
            }

            return 0;
        }

        private int getNegatedGainByType(Card card, GainType gainType) {
            List<Gain> bonuses = card.getCardMetadata().getBonuses();
            if (CollectionUtils.isEmpty(bonuses)) {
                return 0;
            }
            for (Gain bonus : bonuses) {
                if (bonus.getType() == gainType && bonus.getValue() < 0) {
                    return -bonus.getValue();
                }
            }

            return 0;
        }

        private int getGainSum(GainType gainType) {
            int count = 0;
            for (Card card : hand) {
                List<Gain> bonuses = card.getCardMetadata().getBonuses();
                if (CollectionUtils.isEmpty(bonuses)) {
                    continue;
                }
                for (Gain bonus : bonuses) {
                    if (bonus.getType() == gainType) {
                        count += bonus.getValue();
                    }
                }
            }

            return count;
        }

        private FeatureStats computeStats(List<Float> values) {
            FeatureStats stats = new FeatureStats();

            if (values.isEmpty()) return stats;

            DescriptiveStatistics ds = new DescriptiveStatistics();
            int nonZero = 0;

            for (float v : values) {
                ds.addValue(v);
                if (Math.abs(v) > 0.001f) nonZero++;
            }

            stats.sum = (float) ds.getSum();
            stats.mean = (float) ds.getMean();
            stats.max = (float) ds.getMax();
            stats.min = (float) ds.getMin();
            stats.std = (float) ds.getStandardDeviation();
            stats.median = (float) ds.getPercentile(50);
            stats.nonZeroCount = nonZero;

            return stats;
        }

        //purple 0-2
        //red 3-6
        //yellow 7-12
        //white 13-15


        private static final int O2_MAX_LIMIT = 14;
        private static final int TEMP_MAX_LIMIT = 19;


        private int getOxygenStepFromColor(ParameterColor color, boolean isMaxBoundary) {
            return switch (color) {
                case P -> // Purple: 0-2 (Min=0, Max=2)
                        isMaxBoundary ? 2 : 0;
                case R -> // Red: 3-6 (Min=3, Max=6)
                        isMaxBoundary ? 6 : 3;
                case Y -> // Yellow: 7-11 (Min=7, Max=11)
                        isMaxBoundary ? 11 : 7;
                case W -> // White: 12-14 (Min=12, Max=14)
                        isMaxBoundary ? 14 : 12;
                default ->
                    // Если карта не имеет требования по цвету, или требование уже выполнено
                        isMaxBoundary ? O2_MAX_LIMIT : 0;
            };
        }

        private ParameterColor getOxygenColorFromValue(int value) {
            if (value <= 2) {
                return P;
            } else if (value <= 6) {
                return R;
            } else if (value <= 11) {
                return Y;
            } else {
                return W;
            }
        }

        private int getTemperatureStepFromColor(ParameterColor color, boolean isMaxBoundary) {
            switch (color) {
                case P: // Purple: 0-5 (Min=0, Max=5)
                    return isMaxBoundary ? 5 : 0;
                case R: // Red: 6-10 (Min=6, Max=10)
                    return isMaxBoundary ? 10 : 6;
                case Y: // Yellow: 11-15 (Min=11, Max=15)
                    return isMaxBoundary ? 15 : 11;
                case W: // White: 16-19 (Min=16, Max=19)
                    return isMaxBoundary ? 19 : 16;
                default:
                    // Если карта не имеет требования по цвету, или требование уже выполнено
                    return isMaxBoundary ? O2_MAX_LIMIT : 0;
            }
        }

        private ParameterColor getTemperatureColorFromValue(int value) {
            if (value <= 5) {
                return P;
            } else if (value <= 10) {
                return R;
            } else if (value <= 15) {
                return Y;
            } else {
                return W;
            }
        }

        /**
         * Преобразует цветовые требования карты в числовые Min/Max границы.
         * Возвращает: {minReq, maxReq}
         */
        private int[] getOxygenReqBoundaries(Card card, boolean canAmplifyOxygenOrTemperature) {
            Collection<ParameterColor> reqs = card.getOxygenRequirement();

            if (CollectionUtils.isEmpty(reqs)) {
                return new int[]{0, O2_MAX_LIMIT};
            }

            if (canAmplifyOxygenOrTemperature) {
                Set<ParameterColor> effectiveReqs = new HashSet<>(reqs);

                for (ParameterColor color : reqs) {
                    effectiveReqs.addAll(getAdjacentColors(color));
                }

                reqs = effectiveReqs;
            }

            int overallMin = Integer.MAX_VALUE;
            int overallMax = Integer.MIN_VALUE;

            for (ParameterColor color : reqs) {
                overallMin = Math.min(overallMin, getOxygenStepFromColor(color, false));
                overallMax = Math.max(overallMax, getOxygenStepFromColor(color, true));
            }

            return new int[]{overallMin, Math.min(overallMax, O2_MAX_LIMIT)};
        }

        private List<ParameterColor> getAdjacentColors(ParameterColor color) {
            return switch (color) {
                case P -> List.of(ParameterColor.R); // P граничит только с R
                case R -> List.of(P, ParameterColor.Y);
                case Y -> List.of(ParameterColor.R, ParameterColor.W);
                case W -> List.of(ParameterColor.Y); // W граничит только с Y
            };
        }

        /**
         * Преобразует цветовые требования карты в числовые Min/Max границы.
         * Возвращает: {minReq, maxReq}
         */
        private int[] getTemperatureReqBoundaries(Card card, boolean canAmplifyOxygenOrTemperature) {
            Collection<ParameterColor> reqs = card.getTemperatureRequirement();

            if (CollectionUtils.isEmpty(reqs)) {
                return new int[]{0, TEMP_MAX_LIMIT};
            }

            if (canAmplifyOxygenOrTemperature) {
                Set<ParameterColor> effectiveReqs = new HashSet<>(reqs);

                for (ParameterColor color : reqs) {
                    effectiveReqs.addAll(getAdjacentColors(color));
                }

                reqs = effectiveReqs;
            }

            int overallMin = Integer.MAX_VALUE;
            int overallMax = Integer.MIN_VALUE;

            for (ParameterColor color : reqs) {
                overallMin = Math.min(overallMin, getTemperatureStepFromColor(color, false));
                overallMax = Math.max(overallMax, getTemperatureStepFromColor(color, true));
            }

            return new int[]{overallMin, Math.min(overallMax, TEMP_MAX_LIMIT)};
        }

        /**
         * Извлекает 4 признака, связанных с требованиями к кислороду.
         * Признаки: [MinReq Mean, MinReq Max, MaxReq Min, Current O2 Value]
         */
        private void extractOxygenRequirements(float[] features, int idx, boolean canAmplifyOxygenOrTemperature) {
            int oxygenValue = game.getPlanet().getCurrentLevel(GlobalParameter.OXYGEN);
            ParameterColor oxygenColor = game.getPlanet().getOxygenColor();

            int ready = 0;
            int criticalNear = 0;
            int nearInZone = 0;
            int nextZoneOrFar = 0;

            List<Float> minReqs = new ArrayList<>();
            List<Float> maxReqs = new ArrayList<>();

            for (Card card : hand) {
                if (card.getClass() == ColonizerTrainingCamp.class && ((oxygenColor == P || oxygenColor == R) || (oxygenColor == Y && canAmplifyOxygenOrTemperature))) {
                    ready++;
                } else {
                    continue;
                }
                int[] boundaries = getOxygenReqBoundaries(card, canAmplifyOxygenOrTemperature);
                minReqs.add((float) boundaries[0]);
                maxReqs.add((float) boundaries[1]);

                int minReq = boundaries[0];
                int diff = minReq - oxygenValue;

                // --- Тактические корзины ---
                if (diff <= 0) {
                    ready++;
                } else if (diff == 1) {
                    criticalNear++; // ОДИН шаг! Это супер-важно.
                } else if (getOxygenColorFromValue(minReq) == oxygenColor) {
                    nearInZone++;  // В той же зоне, но нужно больше 1 шага.
                } else {
                    nextZoneOrFar++; // Нужно пересечь границу цвета.
                }
            }

            features[idx++] = ready;
            features[idx++] = criticalNear;
            features[idx++] = nearInZone;
            features[idx++] = nextZoneOrFar;

            // --- 1. MINIMUM REQUIREMENTS (Нижние границы) ---
            FeatureStats minReqStats = computeStats(minReqs);

            // [idx] MinReq Mean: Среднее минимальное требование (насколько сильно нужно толкать O2)
            features[idx++] = minReqStats.mean / (float) O2_MAX_LIMIT;

            // [idx+1] MinReq Max: Самый высокий барьер MinReq (самая дорогая активация)
            features[idx++] = minReqStats.max / (float) O2_MAX_LIMIT;

            // --- 2. MAXIMUM REQUIREMENTS (Верхние границы) ---
            FeatureStats maxReqStats = computeStats(maxReqs);

            // [idx+2] MaxReq Min: Самый низкий MaxReq (ближайшее окно закрытия)
            // Note: Мы ищем наименьшее значение, чтобы найти самое близкое ограничение.
            features[idx++] = maxReqStats.min / (float) O2_MAX_LIMIT;
        }

        /**
         * Извлекает 4 признака, связанных с требованиями к кислороду.
         * Признаки: [MinReq Mean, MinReq Max, MaxReq Min, Current O2 Value]
         */
        private void extractTemperatureRequirements(float[] features, int idx, boolean canAmplifyOxygenOrTemperature) {
            int temperatureValue = game.getPlanet().getCurrentLevel(GlobalParameter.TEMPERATURE);
            ParameterColor temperatureColor = game.getPlanet().getTemperatureColor();

            int ready = 0;
            int criticalNear = 0;
            int nearInZone = 0;
            int nextZoneOrFar = 0;

            List<Float> minReqs = new ArrayList<>();
            List<Float> maxReqs = new ArrayList<>();

            for (Card card : hand) {
                if (card.getClass() == ExtremeColdFungus.class && (temperatureColor == P || (temperatureColor == R && canAmplifyOxygenOrTemperature))) {
                    ready++;
                } else {
                    continue;
                }

                int[] boundaries = getTemperatureReqBoundaries(card, canAmplifyOxygenOrTemperature);
                minReqs.add((float) boundaries[0]);
                maxReqs.add((float) boundaries[1]);

                int minReq = boundaries[0];
                int diff = minReq - temperatureValue;

                // --- Тактические корзины ---
                if (diff <= 0) {
                    ready++;
                } else if (diff == 1) {
                    criticalNear++; // ОДИН шаг! Это супер-важно.
                } else if (getTemperatureColorFromValue(minReq) == temperatureColor) {
                    nearInZone++;  // В той же зоне, но нужно больше 1 шага.
                } else {
                    nextZoneOrFar++; // Нужно пересечь границу цвета.
                }
            }

            features[idx++] = ready;
            features[idx++] = criticalNear;
            features[idx++] = nearInZone;
            features[idx++] = nextZoneOrFar;

            // --- 1. MINIMUM REQUIREMENTS (Нижние границы) ---
            FeatureStats minReqStats = computeStats(minReqs);

            // [idx] MinReq Mean: Среднее минимальное требование (насколько сильно нужно толкать O2)
            features[idx++] = minReqStats.mean / (float) TEMP_MAX_LIMIT;

            // [idx+1] MinReq Max: Самый высокий барьер MinReq (самая дорогая активация)
            features[idx++] = minReqStats.max / (float) TEMP_MAX_LIMIT;

            // --- 2. MAXIMUM REQUIREMENTS (Верхние границы) ---
            FeatureStats maxReqStats = computeStats(maxReqs);

            // [idx+2] MaxReq Min: Самый низкий MaxReq (ближайшее окно закрытия)
            // Note: Мы ищем наименьшее значение, чтобы найти самое близкое ограничение.
            features[idx++] = maxReqStats.min / (float) TEMP_MAX_LIMIT;
        }

        private static final float OCEAN_MAX_NORM = 9.0f;

        private void extractOceanRequirements(float[] features, int idx) {
            List<Float> minReqs = new ArrayList<>();
            List<Float> maxReqs = new ArrayList<>();

            for (Card card : hand) {
                minReqs.add((float) getOceanMinReq(card));
                maxReqs.add((float) getOceanMaxReq(card));
            }

            // --- 1. MINIMUM REQUIREMENTS (Нижние границы) ---
            FeatureStats minReqStats = computeStats(minReqs);

            // [idx] MinReq Mean
            features[idx++] = minReqStats.mean / OCEAN_MAX_NORM;

            // [idx+1] MinReq Max
            features[idx++] = minReqStats.max / OCEAN_MAX_NORM;

            // --- 2. MAXIMUM REQUIREMENTS (Верхние границы) ---
            FeatureStats maxReqStats = computeStats(maxReqs);

            // [idx+2] MaxReq Min: Самый низкий верхний предел
            // Note: Используем MaxReq Min, чтобы найти карту, которую быстрее всего заблокирует Ocean
            features[idx++] = maxReqStats.min / OCEAN_MAX_NORM;
        }

        private int getOceanMinReq(Card card) {
            OceanRequirement req = card.getOceanRequirement();
            if (req == null) {
                return 0;
            }
            return req.getMinValue();
        }

        // Возвращает 9, если нет верхнего предела, иначе maxValue
        private int getOceanMaxReq(Card card) {
            OceanRequirement req = card.getOceanRequirement();
            if (req == null) {
                return 9; // Максимальное количество океанов
            }
            return req.getMaxValue();
        }
    }


}
