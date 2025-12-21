package com.terraforming.ares.services.ai.hand;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.buffedCorporations.BuffedArclightCorporation;
import com.terraforming.ares.cards.corporations.ArclightCorporation;
import com.terraforming.ares.cards.corporations.CelestiorCorporation;
import com.terraforming.ares.cards.corporations.HyperionSystemsCorporation;
import com.terraforming.ares.cards.corporations.ModproCorporation;
import com.terraforming.ares.cards.green.*;
import com.terraforming.ares.cards.red.*;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.model.parameters.OceanRequirement;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.services.*;
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
    //TODO add parameter that reflects how many easy cards with have in hand/table and how many difficult to use cards
    private static final Set<Class<?>> ASSEMBLY_LINES_EASY_CARDS = Set.of(
            CelestiorCorporation.class,
            HyperionSystemsCorporation.class,
            ModproCorporation.class,
            AdvancedScreeningTechnology.class,
            AiCentral.class,
            AssetLiquidation.class,
            Birds.class,
            BrainstormingSession.class,
            CircuitBoardFactory.class,
            CommunityGardens.class,
            ConservedBiome.class,
            ExtremeColdFungus.class,
            FarmersMarket.class,
            GhgProductionBacteria.class,
            HydroElectricEnergy.class,
            MatterGenerator.class,
            MatterManufactoring.class,
            NitriteReductingBacteria.class,
            RedraftedContracts.class,
            RegolithEaters.class,
            SelfReplicatingBacteria.class,
            SymbioticFungus.class,
            Tardigrades.class,
            ThinkTank.class,
            CityCouncil.class,
            DroneAssistedConstruction.class,
            FibrousCompositeMaterial.class,
            VirtualEmployeeDevelopment.class
    );


    private static final Map<Class<?>, Float> CARDS_WITH_VP_PER_RESOURCE =//CHECK WITH CHATGPT IF SUCH ROUGH PARAMETER IS OK
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
                    Map.entry(PhysicsComplex.class, 0.5f)
            );

    private static final int VECTOR_SIZE = 1000;//TODO

    private final CardService cardService;
    private final WinPointsService winPointsService;
    private final SpecialEffectsService specialEffectsService;
    private final DraftCardsService draftCardsService;
    private final PaymentValidationService paymentValidationService;
    private final StandardProjectService standardProjectService;


    //todo should have a vector with max values and divide during collection to avoid future normalization

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

        public EncoderCalculator(MarsGame game, Player player, Player anotherPlayer) {
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


        }

        public float[] encodeHand() {
            float[] features = new float[VECTOR_SIZE];
            int idx = 0;

            if (hand.isEmpty()) {
                return features; // Все нули
            }

            // ===============================================
            // БЛОК 1: ОБЩИЕ ПРИЗНАКИ (30 features) [0-29]
            // ===============================================

            // --- 1.1 Размер руки ---
            features[idx++] = hand.size();  // [0]
            features[idx++] = 1f / (hand.size() + 1);

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
            FeatureStats heatStats = computeStats(heatIncome);
            features[idx++] = heatStats.sum; // [15]
            features[idx++] = heatStats.max; // [16]
            features[idx++] = heatStats.nonZeroCount; // [17]

            List<Float> plantIncome = extract((Card c) -> (float) getIncomeByType(c, GainType.PLANT));
            FeatureStats plantStats = computeStats(plantIncome);
            features[idx++] = plantStats.sum; // [18]
            features[idx++] = plantStats.max; // [19]
            features[idx++] = plantStats.nonZeroCount; // [20]

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
            FeatureStats forestsStats = computeStats(forestsGain);
            features[idx++] = forestsStats.sum;
            features[idx++] = forestsStats.max;
            features[idx++] = forestsStats.nonZeroCount;

            List<Float> temperatureGain = extract((Card c) -> (float) getPositiveGainByType(c, GainType.TEMPERATURE));
            FeatureStats temperatureGainStats = computeStats(temperatureGain);
            features[idx++] = temperatureGainStats.sum;
            features[idx++] = temperatureGainStats.max;
            features[idx++] = temperatureGainStats.nonZeroCount;

            int oxygenGainSum = getGainSum(GainType.OXYGEN);
            features[idx++] = oxygenGainSum;

            List<Float> oceanGain = extract((Card c) -> (float) getPositiveGainByType(c, GainType.OCEAN));
            FeatureStats oceanGainStats = computeStats(oceanGain);
            features[idx++] = oceanGainStats.sum;
            features[idx++] = oceanGainStats.max;
            features[idx++] = oceanGainStats.nonZeroCount;
            //TODO EXTRA STAT FOR EACH OF SUCH PARAMETER THAT MULTIPLIES OCEANS LEFT
//            features[idx++] = oceanGainStats.sum * oceansRemaining;
//            features[idx++] = oceanGainStats.max * oceansRemaining;

            List<Float> positivePlantGain = extract((Card c) -> (float) getPositiveGainByType(c, GainType.PLANT));
            if (handCardActions.containsKey(CardAction.FARMING_COOPS)) {
                positivePlantGain.add(3f);//bugfix
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
            features[idx++] = negativeTRgainStats.sum / 6.0f;

            // --- 1.5 Теги (количество каждого тега в руке) ---
            int[] tagCounts = new int[11];
            for (Card card : hand) {
                List<Tag> tags = card.getTags();
                for (Tag tag : tags) {
                    tagCounts[tag.ordinal()]++;
                }
            }

            for (int i = 0; i < tagCounts.length; i++) {
                features[idx++] = tagCounts[i];    // [30-40] Tag counts
            }

            // --- 1.6 Тег diversity ---
            int uniqueTags = 0;
            for (int count : tagCounts) {
                if (count > 0) uniqueTags++;
            }
            features[idx++] = uniqueTags;          // [41] Разнообразие тегов

            // --- 1.7 Самый частый тег ---
            int maxTagCount = 0;
            for (int count : tagCounts) {
                maxTagCount = Math.max(maxTagCount, count);
            }
            features[idx++] = maxTagCount;         // [42] Max tag concentration

            //real incomes, lets artificial jungle to know if it has plants
            features[idx++] = player.getPlantsIncome() > 0 ? 1 : 0;
            features[idx++] = (float) Math.log1p(player.getPlants());
            features[idx++] = (float) Math.log1p(player.getPlantsIncome());

            //real incomes, lets development center to know if it has plants
            features[idx++] = (float) Math.log1p(player.getHeat());
            features[idx++] = (float) Math.log1p(player.getHeatIncome());

            features[idx++] = player.getCardIncome() > 0 ? 1 : 0;
            features[idx++] = (float) Math.log1p(player.getCardIncome());


            features[idx++] = playedTagToCount.getOrDefault(Tag.SPACE, 0L);
            features[idx++] = playedTagToCount.getOrDefault(Tag.EARTH, 0L);
            features[idx++] = playedTagToCount.getOrDefault(Tag.EVENT, 0L);
            features[idx++] = playedTagToCount.getOrDefault(Tag.SCIENCE, 0L);
            features[idx++] = playedTagToCount.getOrDefault(Tag.PLANT, 0L);
            features[idx++] = playedTagToCount.getOrDefault(Tag.ENERGY, 0L);
            features[idx++] = playedTagToCount.getOrDefault(Tag.BUILDING, 0L);
            features[idx++] = playedTagToCount.getOrDefault(Tag.ANIMAL, 0L);
            features[idx++] = playedTagToCount.getOrDefault(Tag.JUPITER, 0L);
            features[idx++] = playedTagToCount.getOrDefault(Tag.MICROBE, 0L);


            features[idx++] = getImmediateMicrobeGainSum();//cards that when built put microbes on any other chosen card
            features[idx++] = getImmediateAnimalGainSum();//cards that when built put animals on any other chose card

            features[idx++] = handCardActions.containsKey(CardAction.HEAT_EARTH_INCOME) ? playedTagToCount.getOrDefault(Tag.EARTH, 0L) : 0;
            features[idx++] = handCardActions.containsKey(CardAction.MC_EARTH_INCOME) ? handCardActions.get(CardAction.MC_EARTH_INCOME) * playedTagToCount.getOrDefault(Tag.EARTH, 0L) : 0;
            features[idx++] = handCardActions.containsKey(CardAction.MC_ANIMAL_PLANT_INCOME) ? playedTagToCount.getOrDefault(Tag.ANIMAL, 0L) + playedTagToCount.getOrDefault(Tag.PLANT, 0L) : 0;
            features[idx++] = handCardActions.containsKey(CardAction.PLANT_PLANT_INCOME) ? playedTagToCount.getOrDefault(Tag.PLANT, 0L) : 0;
            int howManyScienceWasPlayed = playedTagToCount.getOrDefault(Tag.SCIENCE, 0L).intValue();
            features[idx++] = handCardActions.containsKey(CardAction.CARD_SCIENCE_INCOME) ? (float) howManyScienceWasPlayed / 3 : 0;
            features[idx++] = handCardActions.containsKey(CardAction.MC_SCIENCE_INCOME) ? howManyScienceWasPlayed : 0;
            features[idx++] = handCardActions.containsKey(CardAction.MC_2_BUILDING_INCOME) ? (float) playedTagToCount.getOrDefault(Tag.BUILDING, 0L) / 2 : 0;
            features[idx++] = handCardActions.containsKey(CardAction.MC_ENERGY_INCOME) ? playedTagToCount.getOrDefault(Tag.ENERGY, 0L) : 0;
            features[idx++] = handCardActions.containsKey(CardAction.MC_SPACE_INCOME) ? playedTagToCount.getOrDefault(Tag.SPACE, 0L) : 0;
            features[idx++] = handCardActions.containsKey(CardAction.HEAT_SPACE_INCOME) ? playedTagToCount.getOrDefault(Tag.SPACE, 0L) : 0;
            features[idx++] = handCardActions.containsKey(CardAction.MC_EVENT_INCOME) ? playedTagToCount.getOrDefault(Tag.EVENT, 0L) : 0;
            features[idx++] = handCardActions.containsKey(CardAction.HEAT_ENERGY_INCOME) ? playedTagToCount.getOrDefault(Tag.ENERGY, 0L) : 0;
            features[idx++] = handCardActions.containsKey(CardAction.PLANT_MICROBE_INCOME) ? playedTagToCount.getOrDefault(Tag.MICROBE, 0L) : 0;
            features[idx++] = handCardActions.containsKey(CardAction.MC_FOREST_INCOME) ? player.getForests() : 0;
            features[idx++] = handCardActions.containsKey(CardAction.PROCESSED_METALS) ? playedTagToCount.getOrDefault(Tag.ENERGY, 0L) : 0;//card per energy tag
            features[idx++] = handCardActions.containsKey(CardAction.AWARD_WINNING_REFLECTOR) ? 1 : 0;//card that requires a milestone achieved to give a bonus, covered by milestone metrics
            features[idx++] = (handCardActions.containsKey(CardAction.TOURISM)) ? 1 : 0;//card that gives TR per milestone, covered also by milestone metrics


            // ===============================================
            // БЛОК 2: УНИКАЛЬНЫЕ ЭФФЕКТЫ (50 features) [30-79]
            // ===============================================
            features[idx++] = (handCardActions.getOrDefault(CardAction.AUTOMATED_FACTORIES, 0L) + handCardActions.getOrDefault(CardAction.TALL_STATION, 0L)) / 2.0F;//build extra at cost <9
            features[idx++] = (handCardActions.getOrDefault(CardAction.ENERGY_STORAGE, 0L));//card requires 7 TR
            features[idx++] = handCardClasses.contains(Microprocessors.class) ? 1 : 0;//draw 2 cards then discard a card
            features[idx++] = handCardClasses.contains(InventionContest.class) ? 1 : 0;//draw 3 cards then discard a card
            features[idx++] = (handCardActions.getOrDefault(CardAction.PROCESSING_PLANT, 0L));//gives card with building tag


            features[idx++] = handCardClasses.contains(AssortedEnterprises.class) ? 1 : 0;//lets u build any extra card with 2mc discount
            features[idx++] = handCardClasses.contains(BusinessContracts.class) ? 1 : 0;//take 4 cards and discard 2
            features[idx++] = handCardClasses.contains(CeosFavoriteProject.class) ? 1 : 0;//can put 2 animals or 2 microbes TODO - add a parameter that couns how many good applicable cards are present for it or something
            features[idx++] = (handCardActions.getOrDefault(CardAction.IMPORTED_HYDROGEN, 0L));//gives 3 plants OR 3 microbes OR 2 animals TODO analise how applicable it is right now?
            features[idx++] = handCardClasses.contains(InvestmentLoan.class) ? 1 : 0;//gain 10 mc after built
            features[idx++] = handCardClasses.contains(LargeConvoy.class) ? 1 : 0;//gives 5 plants or 3 animals TODO something???
            features[idx++] = handCardClasses.contains(LocalHeatTrapping.class) ? 1 : 0;//-3 heat, +4 plants, +2 animals or + 2 microbes TODO something???
            features[idx++] = handCardClasses.contains(NitrogenRichAsteroid.class) && playedTagToCount.getOrDefault(Tag.PLANT, 0L) >= 3 ? 4 : 0;//get 4 plants if have at least 3 plant tags TODO something???
            features[idx++] = handCardClasses.contains(SpecialDesign.class) ? 1 : 0;//lets you build 1 building with +- requirement TODO count how many cards it can help with?
            features[idx++] = handCardClasses.contains(SyntheticCatastrophe.class) ? 1 : 0;//can return 1 red card to your hand. Count how many applicable cards you have on table and you have in hand?--
            features[idx++] = handCardClasses.contains(WorkCrews.class) ? 1 : 0;//build blue or red card with discount of -11 TODO do we need to measure red/blue cards in hand? do we need to calculate how useful that 11 discount can be?
            features[idx++] = handCardClasses.contains(BiomedicalImports.class) ? 1 : 0;//raise oxygen or improve phase TODO we already have separate raise oxygen and separate improve phase can we do something?
            features[idx++] = handCardClasses.contains(CryogenicShipment.class) ? 1 : 0;//add 3 microbes OR 2 animals TODO somehow measure useful cards to apply this to?
            features[idx++] = handCardClasses.contains(InnovativeTechnologiesAward.class) ? player.getPhaseCards().stream().filter(phaseCard -> phaseCard != 0).count() : 0;


            features[idx++] = handCardClasses.contains(AdaptationTechnology.class) && !canAmplifyOxygenOrTemperature ? 1 : 0;
            features[idx++] = specialEffectsService.ownsSpecialEffect(player, SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT) ? 1 : 0;


            features[idx++] = handCardClasses.contains(AdvancedScreeningTechnology.class) ? 1 : 0;//other cards that synergise will already have the related flags 1/0


            boolean handAiCentral = handCardClasses.contains(AiCentral.class);
            boolean handAntiGravity = handCardClasses.contains(AntiGravityTechnology.class);

            features[idx++] = (handAiCentral ? 2 : 0) + (handCardClasses.contains(CircuitBoardFactory.class) ? 1 : 0);//how many cards we can earn with actions
            features[idx++] = handAiCentral ? 1 : 0;
            features[idx++] = (playedCardClasses.contains(AiCentral.class)) ? 1 : 0;

            features[idx++] = handAntiGravity ? 1 : 0;
            features[idx++] = (playedCardClasses.contains(AntiGravityTechnology.class)) ? 1 : 0;

            {//common both for AiCentral and AntiGravityTechnology
                features[idx++] = (handAiCentral || handAntiGravity) && howManyScienceWasPlayed >= 5 ? 1 : 0;
                features[idx++] = (handAiCentral || handAntiGravity) ? (float) howManyScienceWasPlayed / 5.f : 0;
            }

            if (handCardClasses.contains(AnaerobicMicroorganisms.class)) {//TODO difficult
                features[idx++] = tagCounts[Tag.ANIMAL.ordinal()] + tagCounts[Tag.PLANT.ordinal()] + tagCounts[Tag.MICROBE.ordinal()];//TODO make more complex like claude suggests
            } else {
                features[idx++] = 0;
            }
            features[idx++] = handCardClasses.contains(Decomposers.class) ? (tagCounts[Tag.ANIMAL.ordinal()] + tagCounts[Tag.PLANT.ordinal()] + tagCounts[Tag.MICROBE.ordinal()]) : 0;//TODO gives microbes based on tag, I include tag count in hand into this counter


            //OCEAN BLUE CARDS
            {//good example
                boolean handAquifer = handCardActions.containsKey(CardAction.AQUIFER_PUMPING);
                boolean playedAquifer = playedCardActions.containsKey(CardAction.AQUIFER_PUMPING);

                features[idx++] = handAquifer ? 1 : 0;
                features[idx++] = playedAquifer ? 1 : 0;

                features[idx++] = (handAquifer || playedAquifer) ? game.getPlanet().oceansLeft() : 0;

                features[idx++] = game.getPlanet().isOceansMax() ? 0 : Math.min(5, player.getSteelIncome());
            }

            {
                boolean handVolcanicPools = handCardClasses.contains(VolcanicPools.class);
                boolean playedVolcanicPools = playedCardClasses.contains(VolcanicPools.class);

                features[idx++] = handVolcanicPools ? 1 : 0;
                features[idx++] = playedVolcanicPools ? 1 : 0;

                float oceansLeftRatio = (float) game.getPlanet().oceansLeft() / game.getPlanet().oceansMaxCount();
                features[idx++] = playedVolcanicPools ? oceansLeftRatio : 0;
                features[idx++] = handVolcanicPools ? (oceansLeftRatio * oceansLeftRatio) : 0;

                if (game.getPlanet().isOceansMax()) {//zero utility from discount
                    features[idx++] = 0;
                    features[idx++] = 0;
                } else if (handVolcanicPools || playedVolcanicPools) {
                    int playerTagEnergyCount = playedTagToCount.getOrDefault(Tag.ENERGY, 0L).intValue();//discount per 1 energy tag
                    int handEnergyCount = tagCounts[Tag.ENERGY.ordinal()];

                    features[idx++] = Math.min(1.0f, playerTagEnergyCount / 12.0f);
                    features[idx++] = Math.min(1.0f, handEnergyCount / 12.0f);
                }
            }

            {//good example
                boolean handAlgae = handCardActions.containsKey(CardAction.ARCTIC_ALGAE);
                boolean playedAlgae = playedCardActions.containsKey(CardAction.ARCTIC_ALGAE);

                features[idx++] = handAlgae ? 1 : 0;
                features[idx++] = playedAlgae ? 1 : 0;

                features[idx++] = (handAlgae || playedAlgae) ? game.getPlanet().oceansLeft() : 0;
                features[idx++] = (handAlgae || playedAlgae) ? game.getPlanet().oxygenLeft() : 0;

                features[idx++] = handAlgae ? getMinRedAvailabilityProgress(GlobalParameter.TEMPERATURE) : 0;
            }

            {//good example
                boolean handFish = handCardActions.containsKey(CardAction.FISH);
                boolean playedFish = playedCardActions.containsKey(CardAction.FISH);

                features[idx++] = handFish ? 1 : 0;
                features[idx++] = playedFish ? 1 : 0;

                features[idx++] = (playedFish || handFish) ? game.getPlanet().oceansLeft() : 0;
                features[idx++] = handFish ? getMinRedAvailabilityProgress(GlobalParameter.TEMPERATURE) : 0;
            }

            {//good example
                boolean handHerbivores = handCardActions.containsKey(CardAction.HERBIVORES);
                boolean playedHerbivores = playedCardActions.containsKey(CardAction.HERBIVORES);

                features[idx++] = handHerbivores ? 1 : 0;
                features[idx++] = playedHerbivores ? 1 : 0;

                float stepsLeft = game.getPlanet().oceansLeft()
                        + game.getPlanet().oxygenLeft()
                        + game.getPlanet().temperatureLeft();


                features[idx++] = handHerbivores ? stepsLeft : 0;
                features[idx++] = playedHerbivores ? stepsLeft : 0;
                features[idx++] = handHerbivores ? Math.min(1, (float) game.getPlanet().oceansBuilt() / 5) : 0;//availability progress
            }

            features[idx++] = handCardClasses.contains(RestructuredResources.class) ? 1 : 0;//handled by log of plants and plantsincome
            features[idx++] = playedCardClasses.contains(RestructuredResources.class) ? 1 : 0;//handled by log of plants and plantsincome

            features[idx++] = handCardClasses.contains(DevelopmentCenter.class) ? 1 : 0;//handled by log of heat and heatincome
            features[idx++] = playedCardClasses.contains(DevelopmentCenter.class) ? 1 : 0;//handled by log of heat and heatincome


            {//Assembly Lines
                features[idx++] = handCardClasses.contains(AssemblyLines.class) ? 1 : 0;
                features[idx++] = playedCardClasses.contains(AssemblyLines.class) ? 1 : 0;
                if (handCardClasses.contains(AssemblyLines.class) || playedCardClasses.contains(AssemblyLines.class)) {
                    int tableEasyCards = (int) playedCardClasses.stream().filter(ASSEMBLY_LINES_EASY_CARDS::contains).count();
                    int tableHardCards = (int) playedCards.stream().filter(card -> card.isActiveCard() && !ASSEMBLY_LINES_EASY_CARDS.contains(card.getClass())).count();

                    int handEasyCards = (int) handCardClasses.stream().filter(ASSEMBLY_LINES_EASY_CARDS::contains).count();
                    int handHardCards = (int) hand.stream().filter(card -> card.isActiveCard() && !ASSEMBLY_LINES_EASY_CARDS.contains(card.getClass())).count();

                    features[idx++] = tableEasyCards;
                    features[idx++] = tableHardCards;
                    features[idx++] = handEasyCards;
                    features[idx++] = handHardCards;
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {//TODO VERIFY WITH CHATGPT
                boolean isHelionCorp = playedCardActions.containsKey(CardAction.HELION_CORPORATION);
                boolean handPI = handCardActions.containsKey(CardAction.POWER_INFRASTRUCTURE);
                boolean playedPI = playedCardActions.containsKey(CardAction.POWER_INFRASTRUCTURE);

                features[idx++] = isHelionCorp ? 1 : 0;
                features[idx++] = handPI ? 1 : 0;
                features[idx++] = playedPI ? 1 : 0;

                float tempProgress = (float) game.getPlanet().getCurrentLevel(GlobalParameter.TEMPERATURE) / game.getPlanet().temperatureMax();

                // Если температура близка к максу, ценность PI в руке растет
                features[idx++] = handPI ? tempProgress : 0;

                //чем больше доход или количество тепла, тем мощнее наличие PowerInfrastructure в руке. При этом связываем её со схожими признаками на столе.
                if (handPI || playedPI || isHelionCorp) {
                    features[idx++] = (float) Math.log1p(player.getHeatIncome());
                    features[idx++] = (float) Math.log1p(player.getHeat());
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            features[idx++] = handCardClasses.contains(AssetLiquidation.class) ? 1 : 0;
            features[idx++] = handCardClasses.contains(BrainstormingSession.class) ? 1 : 0;

            features[idx++] = handCardClasses.contains(CaretakerContract.class) ? 1 : 0;
            features[idx++] = playedCardClasses.contains(CaretakerContract.class) ? 1 : 0;

            //Useful and potential heat capacity
            {
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

            {
                features[idx++] = hasHyperionCorp ? 1 : 0;//can get extra 1mc if chose 3rd
                features[idx++] = hasHandCommunityGardens ? 1 : 0;//potential get extra plant if chose 3rd
                features[idx++] = hasPlayedCommunityGardens ? 1 : 0;//can get extra plant if chose 3rd

                features[idx++] = (hasPlayedCommunityGardens ? 2 : 0) + (hasHyperionCorp ? 1 : 0);//2mc +1mc per action
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

            boolean exocorpOnTable = playedCardActions.containsKey(CardAction.EXOCORP_CORPORATION);
            boolean compostingOnTable = playedCardActions.containsKey(CardAction.COMPOSTING_FACTORY);
            int activeSellBonus = (compostingOnTable ? 1 : 0) + (exocorpOnTable ? 1 : 0);

            {
                boolean compostingInHand = handCardActions.containsKey(CardAction.COMPOSTING_FACTORY);

                features[idx++] = activeSellBonus;
                features[idx++] = player.getHand().size() * (3 + activeSellBonus);
                features[idx++] = player.getCardIncome() * (3 + activeSellBonus);

                float potentialSellBonus = (compostingInHand ? 1 : 0);
                features[idx++] = potentialSellBonus;
                features[idx++] = player.getHand().size() * potentialSellBonus;
                features[idx++] = player.getCardIncome() * potentialSellBonus;
            }

            {
                boolean matterManufactoringOnTable = playedCardActions.containsKey(CardAction.MATTER_MANUFACTORING);
                boolean matterManufactoringInHand = handCardActions.containsKey(CardAction.MATTER_MANUFACTORING);

                boolean thinkTankTable = playedCardActions.containsKey(CardAction.THINKTANK);
                boolean thinkTankHand = handCardActions.containsKey(CardAction.THINKTANK);

                features[idx++] = matterManufactoringOnTable ? 1.0f : 0.0f;
                features[idx++] = thinkTankTable ? 1.0f : 0.0f;
                features[idx++] = matterManufactoringInHand ? 1.0f : 0.0f;
                features[idx++] = thinkTankHand ? 1.0f : 0.0f;

                float cardArbitrageValue = 0;//выгода это цена карты - цена действий
                if (matterManufactoringOnTable) cardArbitrageValue += (3.0f + activeSellBonus - 1.0f);
                if (thinkTankTable) cardArbitrageValue += (3.0f + activeSellBonus - 2.0f);

                features[idx++] = cardArbitrageValue;
            }

            boolean hasHandFarmersMarket = handCardActions.containsKey(CardAction.FARMERS_MARKET);
            boolean hasPlayedFarmersMarket = playedCardActions.containsKey(CardAction.FARMERS_MARKET);

            features[idx++] = hasHandFarmersMarket ? 1 : 0;
            features[idx++] = hasPlayedFarmersMarket ? 1 : 0;

            boolean hasHandFarmingCoops = handCardActions.containsKey(CardAction.FARMING_COOPS);
            boolean hasPlayedFarmingCoops = playedCardActions.containsKey(CardAction.FARMING_COOPS);

            features[idx++] = hasHandFarmingCoops ? 1 : 0;
            features[idx++] = hasPlayedFarmingCoops ? 1 : 0;

            features[idx++] = handCardActions.containsKey(CardAction.GREEN_HOUSES) ? 1 : 0;
            features[idx++] = playedCardActions.containsKey(CardAction.GREEN_HOUSES) ? 1 : 0;

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

            float microbeProductionPower = getMicrobeProductionPower() + 1;//+1 because card can put microbe on itself

            if (nitriteHand) {
                float utility = Math.min(1.0f, (float) game.getPlanet().oceansLeft() / game.getPlanet().oceansMaxCount());
                features[idx++] = (1.0f + (0.33f * microbeProductionPower)) * utility;
            } else {
                features[idx++] = 0;
            }

            if (regolithHand) {
                float utility = Math.min(1.0f, (float) game.getPlanet().oxygenLeft() / game.getPlanet().oxygenMax());
                features[idx++] = (0.5f * microbeProductionPower) * utility;
            } else {
                features[idx++] = 0;
            }

            if (ghgHand) {
                float utility = Math.min(1.0f, (float) game.getPlanet().temperatureLeft() / game.getPlanet().temperatureMax());
                features[idx++] = (0.5f * microbeProductionPower) * utility;
            } else {
                features[idx++] = 0;
            }

            {
                boolean handSelfRep = handCardActions.containsKey(CardAction.SELF_REPLICATING_BACTERIA);
                boolean playedSelfRep = playedCardActions.containsKey(CardAction.SELF_REPLICATING_BACTERIA);

                features[idx++] = handSelfRep ? 1 : 0;
                features[idx++] = playedSelfRep ? 1 : 0;

                if (playedSelfRep) {
                    int currentMicrobes = player.getCardResourcesCount().getOrDefault(SelfReplicatingBacteria.class, 0);

                    float chargeProgress = Math.min(1.0f, (currentMicrobes + microbeProductionPower) / 5.0f);
                    features[idx++] = chargeProgress;
                } else {
                    features[idx++] = 0;
                }

                if (handSelfRep) {
                    features[idx++] = Math.min(1.0f, microbeProductionPower / 5.0f);
                } else {
                    features[idx++] = 0;
                }

                // 2. ГИБКАЯ ОЦЕНКА ЦЕЛИ:
                // Находим самую дорогую карту в руке (кроме самой бактерии)
                float maxCostInHand = (float) hand.stream()
                        .filter(c -> !c.getClass().equals(SelfReplicatingBacteria.class))
                        .mapToInt(Card::getPrice)
                        .max()
                        .orElse(0);

                // Нормируем стоимость. Например, 25 MC - это 1.0 (идеальная цель).
                // Если в руке карта за 50 MC, это все равно 1.0 (скидка 25 все равно уйдет полностью).
                // Если в руке карта за 10 MC, это 0.4.
                features[idx++] = Math.min(1.0f, maxCostInHand / 25.0f);

                // 3. Общий вес руки (дополнительно). Статистически потом нормализуем
                features[idx++] = (float) hand.stream()
                        .filter(c -> !c.getClass().equals(SelfReplicatingBacteria.class))
                        .mapToInt(Card::getPrice)
                        .sum();
            }

            {
                boolean handLivestock = handCardActions.containsKey(CardAction.LIVESTOCK);
                boolean playedLivestock = playedCardActions.containsKey(CardAction.LIVESTOCK);

                features[idx++] = handLivestock ? 1 : 0;
                features[idx++] = playedLivestock ? 1 : 0;

                float temperatureLeftRatio = (float) game.getPlanet().temperatureLeft() / game.getPlanet().temperatureMax();

                features[idx++] = playedLivestock ? temperatureLeftRatio : 0;
                features[idx++] = handLivestock ? temperatureLeftRatio * temperatureLeftRatio : 0;

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

                int currentTemperatureLevel = game.getPlanet().getCurrentLevel(GlobalParameter.TEMPERATURE);
                int targetOxygenLevel = getTemperatureStepFromColor(canAmplifyOxygenOrTemperature ? ParameterColor.P : ParameterColor.R, false);

                float availabilityProgress;
                if (currentTemperatureLevel >= targetOxygenLevel) {
                    availabilityProgress = 1.0f; // Можно строить
                } else {
                    availabilityProgress = (float) currentTemperatureLevel / targetOxygenLevel;
                }
                features[idx++] = handSA ? availabilityProgress : 0;

                // --- Блок "Способности строить леса" (Greenery Power) ---
                // 1. Считаем, сколько лесов мы можем строить в среднем (Растения + Деньги)
                float plantsIncome = (float) player.getPlantsIncome();
                float moneyIncome = (float) player.getMcIncome();

                float greeneryPerTurn = (plantsIncome / paymentValidationService.forestPriceInPlants(player)) + (moneyIncome / standardProjectService.getProjectPrice(player, StandardProjectType.FOREST));

                // Подаем как отдельный признак синергии для карты
                features[idx++] = (handSA || playedSA) ? greeneryPerTurn : 0;
            }

            {
                boolean handDevelopedInfrastructure = handCardClasses.contains(DevelopedInfrastructure.class);
                boolean playedDevelopedInfrastructure = playedCardClasses.contains(DevelopedInfrastructure.class);

                features[idx++] = handDevelopedInfrastructure ? 1 : 0;
                features[idx++] = playedDevelopedInfrastructure ? 1 : 0;

                float temperatureLeftRatio = (float) game.getPlanet().temperatureLeft() / game.getPlanet().temperatureMax();
                features[idx++] = playedDevelopedInfrastructure ? temperatureLeftRatio : 0;
                features[idx++] = handDevelopedInfrastructure ? (temperatureLeftRatio * temperatureLeftRatio) : 0;

                int blueCardsCount = (int) player.getPlayed().getCards().stream()
                        .map(cardService::getCard)
                        .filter(c -> c.getColor() == CardColor.BLUE)
                        .limit(5)
                        .count();

                //reflects the distance to a good discount for the action on this card
                features[idx++] = (handDevelopedInfrastructure || playedDevelopedInfrastructure) && !game.getPlanet().isTemperatureMax() ? blueCardsCount / 5.0f : 0;
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

                float oceansLeftRatio = (float) game.getPlanet().oceansLeft() / game.getPlanet().oceansMaxCount();
                features[idx++] = playedWI ? oceansLeftRatio : 0;
                features[idx++] = handWI ? (oceansLeftRatio * oceansLeftRatio) : 0;

                if (game.getPlanet().isOceansMax()) {
                    features[idx++] = 0;
                    features[idx++] = 0;
                } else if (handWI || playedWI) {
                    features[idx++] = Math.min(1.0f, player.getTitaniumIncome() / 12.0f);
                    features[idx++] = Math.min(1.0f, titaniumStats.sum / 12.0f);
                }

                int playedJupiterTagCount = playedTagToCount.getOrDefault(Tag.JUPITER, 0L).intValue();
                int handJupiterTagCount = tagCounts[Tag.JUPITER.ordinal()];

                features[idx++] = ((handWI || playedWI) ? playedJupiterTagCount : 0) + ((handIMI || playedIMI) ? playedJupiterTagCount : 0);
                features[idx++] = ((handWI || playedWI) ? handJupiterTagCount : 0) + ((handIMI || playedIMI) ? handJupiterTagCount : 0);
                features[idx++] = handTG ? playedJupiterTagCount : 0;
            }

            {
                boolean handPP = handCardClasses.contains(ProgressivePolicies.class);
                boolean playedPP = playedCardClasses.contains(ProgressivePolicies.class);

                features[idx++] = handPP ? 1 : 0;
                features[idx++] = playedPP ? 1 : 0;

                float oxygenLeftRatio = (float) game.getPlanet().oxygenLeft() / game.getPlanet().oxygenMax();
                features[idx++] = playedPP ? oxygenLeftRatio : 0;
                features[idx++] = handPP ? (oxygenLeftRatio * oxygenLeftRatio) : 0;

                int eventCardsCount = playedTagToCount.getOrDefault(Tag.EVENT, 0L).intValue();

                //reflects the distance to a good discount for the action on this card
                features[idx++] = (handPP || playedPP)
                        ? Math.min(1.0f, eventCardsCount / 4.0f) : 0;
            }

            {
                boolean handIW = handCardClasses.contains(IronWorks.class);
                boolean playedIW = playedCardClasses.contains(IronWorks.class);

                features[idx++] = handIW ? 1 : 0;
                features[idx++] = playedIW ? 1 : 0;

                float oxygenLeftRatio = (float) game.getPlanet().oxygenLeft() / game.getPlanet().oxygenMax();
                features[idx++] = playedIW ? oxygenLeftRatio : 0;
                features[idx++] = handIW ? (oxygenLeftRatio * oxygenLeftRatio) : 0;
            }

            {
                boolean handSW = handCardClasses.contains(Steelworks.class);
                boolean playedSW = playedCardClasses.contains(Steelworks.class);

                features[idx++] = handSW ? 1 : 0;
                features[idx++] = playedSW ? 1 : 0;

                float oxygenLeftRatio = (float) game.getPlanet().oxygenLeft() / game.getPlanet().oxygenMax();
                features[idx++] = playedSW ? oxygenLeftRatio : 0;
                features[idx++] = handSW ? (oxygenLeftRatio * oxygenLeftRatio) : 0;
            }

            {
                boolean handSP = handCardClasses.contains(SolarPunk.class);
                boolean playedSP = playedCardClasses.contains(SolarPunk.class);

                features[idx++] = handSP ? 1 : 0;
                features[idx++] = playedSP ? 1 : 0;

                float oxygenLeftRatio = (float) game.getPlanet().oxygenLeft() / game.getPlanet().oxygenMax();
                features[idx++] = playedSP ? oxygenLeftRatio : 0;
                features[idx++] = handSP ? (oxygenLeftRatio * oxygenLeftRatio) : 0;

                float tableTitanium = (float) player.getTitaniumIncome();
                float handTitanium = titaniumStats.sum;

                features[idx++] = (handSP || playedSP) ? Math.min(1.0f, (tableTitanium + handTitanium) / 7.5f) : 0;

                //доход говорит о том, что мы в теории можем жать эту карту даже без скидки
                features[idx++] = (handSP || playedSP) ? player.getMcIncome() : 0;
            }

            {
                boolean handST = handCardClasses.contains(StandardTechnology.class);
                boolean playedST = playedCardClasses.contains(StandardTechnology.class);

                features[idx++] = handST ? 1 : 0;
                features[idx++] = playedST ? 1 : 0;

                float totalProgress = (float) (game.getPlanet().oceansLeft() +
                        game.getPlanet().oxygenLeft() +
                        game.getPlanet().temperatureLeft());
                features[idx++] = handST || playedST ? totalProgress : 0;//TODO Почти все синергетические параметры должны светиться вне зависимости от того, карта на руке или на столе

                features[idx++] = handST || playedST ? (float) player.getMcIncome() : 0;
            }

            {
                boolean handBirds = handCardClasses.contains(Birds.class);
                boolean playedBirds = playedCardClasses.contains(Birds.class);

                features[idx++] = handBirds ? 1 : 0;
                features[idx++] = playedBirds ? 1 : 0;

                int currentOxygenLevel = game.getPlanet().getCurrentLevel(GlobalParameter.OXYGEN);
                int targetOxygenLevel = getOxygenStepFromColor(canAmplifyOxygenOrTemperature ? ParameterColor.Y : ParameterColor.W, false);

                float availabilityProgress;
                if (currentOxygenLevel >= targetOxygenLevel) {
                    availabilityProgress = 1.0f; // Можно строить
                } else {
                    availabilityProgress = (float) currentOxygenLevel / targetOxygenLevel;
                }
                features[idx++] = handBirds ? availabilityProgress : 0;
            }

            {
                boolean handTardigrades = handCardClasses.contains(Tardigrades.class);
                boolean playedTardigrades = playedCardClasses.contains(Tardigrades.class);

                features[idx++] = handTardigrades ? 1 : 0;
                features[idx++] = playedTardigrades ? 1 : 0;

                features[idx++] = (handTardigrades || playedTardigrades) ? microbeProductionPower : 0;
            }

            {//good example
                boolean handPhysicsComplex = handCardActions.containsKey(CardAction.PHYSICS_COMPLEX);
                boolean playedPhysicsComplex = playedCardActions.containsKey(CardAction.PHYSICS_COMPLEX);

                features[idx++] = handPhysicsComplex ? 1 : 0;
                features[idx++] = playedPhysicsComplex ? 1 : 0;

                float stepsLeft = game.getPlanet().temperatureLeft();

                features[idx++] = playedPhysicsComplex ? stepsLeft : 0;
                features[idx++] = handPhysicsComplex ? stepsLeft : 0;

                features[idx++] = handPhysicsComplex && howManyScienceWasPlayed >= 4 ? 1 : 0;
                features[idx++] = handPhysicsComplex ? (float) howManyScienceWasPlayed / 4.f : 0;
            }

            {
                boolean scienceHand = handCardClasses.contains(MarsUniversity.class);
                boolean scienceTable = playedCardClasses.contains(MarsUniversity.class);

                if (scienceHand || scienceTable) {
                    int scienceInHand = tagCounts[Tag.SCIENCE.ordinal()];
                    int plantsInHand = tagCounts[Tag.PLANT.ordinal()];

                    features[idx++] = (float) scienceInHand;

                    features[idx++] = (float) plantsInHand;

                    features[idx++] = scienceTable ? 1.0f : 0.0f; // Активен сейчас
                    features[idx++] = scienceHand ? 1.0f : 0.0f;  // Можно активировать скоро
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean conferenceHand = handCardClasses.contains(OlympusConference.class);
                boolean conferenceTable = playedCardClasses.contains(OlympusConference.class);

                if (conferenceHand || conferenceTable) {
                    int scienceInHand = tagCounts[Tag.SCIENCE.ordinal()];

                    features[idx++] = (float) scienceInHand;

                    features[idx++] = conferenceTable ? 1.0f : 0.0f; // Активен сейчас
                    features[idx++] = conferenceHand ? 1.0f : 0.0f;  // Можно активировать скоро
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean matterGenOnTable = playedCardActions.containsKey(CardAction.MATTER_GENERATOR);
                boolean matterGenInHand = handCardActions.containsKey(CardAction.MATTER_GENERATOR);

                features[idx++] = matterGenOnTable ? 1.0f : 0.0f;
                features[idx++] = matterGenInHand ? 1.0f : 0.0f;

                // 2. Параметр эффективности (только если карта на столе)
                float matterGenBonus = 0;
                if (matterGenOnTable) {

                    float cardIncome = (float) player.getCardIncome();
                    float handFactor = hand.size() / 10.0f; // Доп. бонус, если рука и так раздута

                    float confidence = Math.min(1.0f, cardIncome + handFactor);
                    matterGenBonus = (6.0f - 3 - activeSellBonus) * confidence;
                }
                features[idx++] = matterGenBonus;
            }

            {
                boolean tableRC = playedCardActions.containsKey(CardAction.REDRAFTED_CONTRACTS);
                boolean handRC = handCardActions.containsKey(CardAction.REDRAFTED_CONTRACTS);

                features[idx++] = tableRC ? 1.0f : 0.0f;
                features[idx++] = handRC ? 1.0f : 0.0f;

                // Больше карт = выше шанс, что среди них есть 3 бесполезных.
                float handSizeFactor = Math.min(1.0f, player.getHand().size() / 10.0f);

                // 2. Фактор "голода" (Card Income)
                // Если доход карт +3 или +4, нам обмен не так важен (мы и так много тянем).
                // Если доход карт 0 или 1, обмен 3-на-3 критически важен для поиска решений.
                float cardIncome = (float) player.getCardIncome();
                float hungerFactor = Math.max(0.0f, 1.0f - (cardIncome / 4.0f));

                // Итоговый потенциал: высок, когда рука полная, а приток новых карт слабый.
                float cyclingUtility = handSizeFactor * hungerFactor;

                features[idx++] = (tableRC || handRC) ? cyclingUtility : 0;
            }

            {
                boolean handAJ = handCardClasses.contains(ArtificialJungle.class);
                boolean tableAJ = playedCardClasses.contains(ArtificialJungle.class);

                boolean handWBS = handCardActions.containsKey(CardAction.WOOD_BURNING_STOVES);
                boolean tableWBS = playedCardActions.containsKey(CardAction.WOOD_BURNING_STOVES);

                features[idx++] = handAJ ? 1 : 0;
                features[idx++] = tableAJ ? 1 : 0;

                float utility = Math.min(1.0f, (float) game.getPlanet().temperatureLeft() / game.getPlanet().temperatureMax());
                features[idx++] = handWBS ? utility : 0;
                features[idx++] = tableWBS ? utility : 0;

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
            float HAND_TRIGGER_VP_SENSITIVITY = (float) CARDS_WITH_VP_PER_RESOURCE.entrySet().stream()
                    .filter(e -> handCardClasses.contains(e.getKey()))
                    .mapToDouble(Map.Entry::getValue)
                    .sum();
            features[idx++] = HAND_TRIGGER_VP_SENSITIVITY;

            float TABLE_TRIGGER_VP_SENSITIVITY = (float) CARDS_WITH_VP_PER_RESOURCE.entrySet().stream()
                    .filter(e -> playedCardClasses.contains(e.getKey()))
                    .mapToDouble(Map.Entry::getValue)
                    .sum();
            features[idx++] = TABLE_TRIGGER_VP_SENSITIVITY;

            idx += handleGlobalParameterSynergy(handCardClasses, features, idx);//+4
            idx += handleGlobalParameterSynergy(playedCardClasses, features, idx);//+4
            float[] producingConsumingMicrobes = handleProducingConsumingMicrobeSynergy();//playedProducingMicrobeCards, playedConsumingMicrobeCards, handProducingMicrobeCards, handConsumingMicrobeCards
            float playedProducingMicrobeCards = producingConsumingMicrobes[0];
            float playedConsumingMicrobeCards = producingConsumingMicrobes[1];
            float handProducingMicrobeCards = producingConsumingMicrobes[2];
            float handConsumingMicrobeCards = producingConsumingMicrobes[3];

            features[idx++] = playedProducingMicrobeCards;
            features[idx++] = playedConsumingMicrobeCards;
            features[idx++] = handProducingMicrobeCards;
            features[idx++] = handConsumingMicrobeCards;

            {
                boolean filterFeedersInHand = handCardActions.containsKey(CardAction.FILTER_FEEDERS);
                boolean filterFeedersPlayed = playedCardActions.containsKey(CardAction.FILTER_FEEDERS);

                boolean canBuild = game.getPlanet().getRevealedOceans().size() >= 2;
                features[idx++] = (filterFeedersPlayed ? 1 : 0);
                features[idx++] = (filterFeedersInHand ? 1 : 0);
                features[idx++] = (filterFeedersInHand && canBuild ? 1 : 0);
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

            float handAnimalConsumingCards = countAnimalConsumingCards(handCardClasses);
            float playedAnimalConsumingCards = countAnimalConsumingCards(playedCardClasses);
            features[idx++] = handAnimalConsumingCards;
            features[idx++] = playedAnimalConsumingCards;

            {
                boolean handViralEnhancers = handCardClasses.contains(ViralEnhancers.class);
                boolean playedViralEnhancers = playedCardClasses.contains(ViralEnhancers.class);

                features[idx++] = handViralEnhancers ? 1 : 0;
                features[idx++] = playedViralEnhancers ? 1 : 0;

                int cardPower = tagCounts[Tag.PLANT.ordinal()] + tagCounts[Tag.MICROBE.ordinal()] + tagCounts[Tag.ANIMAL.ordinal()];

                if (handViralEnhancers || playedViralEnhancers) {
                    features[idx++] = cardPower;
                    features[idx++] = (playedConsumingMicrobeCards + handConsumingMicrobeCards) > 0 ? 1 : 0;
                    features[idx++] = (playedAnimalConsumingCards + handAnimalConsumingCards) > 0 ? 1 : 0;
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }


            //phase upgrades handling
            features[idx++] = player.isPhaseUpgraded(1) ? 1 : 0;
            features[idx++] = player.isPhaseUpgraded(2) ? 1 : 0;
            features[idx++] = player.isPhaseUpgraded(3) ? 1 : 0;
            features[idx++] = player.isPhaseUpgraded(4) ? 1 : 0;
            features[idx++] = player.isPhaseUpgraded(5) ? 1 : 0;

            features[idx++] = (handCardActions.containsKey(CardAction.UPDATE_PHASE_CARD)) ? 1 : 0;
            features[idx++] = (handCardActions.containsKey(CardAction.UPDATE_PHASE_1_CARD)) ? 1 : 0;
            features[idx++] = (handCardActions.containsKey(CardAction.UPDATE_PHASE_2_CARD)) ? 1 : 0;
            features[idx++] = (handCardActions.containsKey(CardAction.UPDATE_PHASE_4_CARD)) ? 1 : 0;
            features[idx++] = (handCardActions.containsKey(CardAction.UPDATE_PHASE_CARD_TWICE)) ? 1 : 0;


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

            {//TODO CHECK CHATGPT
                boolean playedTeractor = playedCardActions.containsKey(CardAction.TERACTOR_CORPORATION);//-3 mc per earth
                boolean playedConference = playedCardActions.containsKey(CardAction.INTERPLANETARY_CONFERENCE);//-3 mc and card per earth/jupiter
                boolean handConference = handCardActions.containsKey(CardAction.INTERPLANETARY_CONFERENCE);

                features[idx++] = handConference ? 1.0f : 0.0f;
                features[idx++] = playedConference ? 1.0f : 0.0f;

                float currentEarthDiscount = 0;
                float currentJupiterDiscount = 0;
                float potentialDiscount = handConference ? 3.0f : 0.0f;

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
                features[idx++] = earthCostInHand + jupiterCostInHand;

                features[idx++] = (float) ((currentEarthDiscount + potentialDiscount) * Math.log1p(earthCostInHand) + (currentJupiterDiscount + potentialDiscount) * Math.log1p(jupiterCostInHand));
            }

            {
                boolean corpOnTable = playedCardActions.containsKey(CardAction.INTERPLANETARY_CINEMATICS); // -2 mc per event

                boolean card2InHand = handCardActions.containsKey(CardAction.MEDIA_GROUP); //-5 mc per event
                boolean card2OnTable = handCardActions.containsKey(CardAction.MEDIA_GROUP); //-5 mc per event

                boolean card3InHand = handCardActions.containsKey(CardAction.OPTIMAL_AEROBRAKING); //+2 heat +2 plants
                boolean card3OnTable = handCardActions.containsKey(CardAction.OPTIMAL_AEROBRAKING); //+2 heat +2 plants

                boolean card4InHand = handCardActions.containsKey(CardAction.RECYCLED_DETRITUS); //+2 cards per event
                boolean card4OnTable = handCardActions.containsKey(CardAction.RECYCLED_DETRITUS);//+2 cards per event

                boolean card5InHand = handCardActions.containsKey(CardAction.IMPACT_ANALYSIS); //+1 card
                boolean card5OnTable = handCardActions.containsKey(CardAction.IMPACT_ANALYSIS); //+1 card

                float totalEventMcDiscount = ((corpOnTable ? 2.0f : 0) + (card2InHand || card2OnTable ? 5.0f : 0.0f)) / 7.0f;

                float totalEventResourceBonus = (card3InHand || card3OnTable) ? 1.0f : 0.0f;

                float totalEventDrawBonus = ((card4InHand || card4OnTable ? 2.0f : 0.0f) + (card5InHand || card5OnTable ? 1.0f : 0.0f)) / 3.0f;

                int eventTagsInHand = tagCounts[Tag.EVENT.ordinal()];

                features[idx++] = eventTagsInHand * totalEventMcDiscount;
                features[idx++] = eventTagsInHand * totalEventResourceBonus;
                features[idx++] = eventTagsInHand * totalEventDrawBonus;

                features[idx++] = card2InHand ? 1 : 0;
                features[idx++] = card2OnTable ? 1 : 0;

                features[idx++] = card3InHand ? 1 : 0;
                features[idx++] = card3OnTable ? 1 : 0;

                features[idx++] = card4InHand ? 1 : 0;
                features[idx++] = card4OnTable ? 1 : 0;

                features[idx++] = card5InHand ? 1 : 0;
                features[idx++] = card5OnTable ? 1 : 0;
            }

            {
                features[idx++] = playedCardActions.containsKey(CardAction.CELESTIOR_CORPORATION) ? 1 : 0;
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
                boolean hasSultiraCorp = playedCardActions.containsKey(CardAction.SULTIRA_CORPORATION);
                features[idx++] = hasSultiraCorp ? 1.0f : 0.0f;
                features[idx++] = hasSultiraCorp ? tagCounts[Tag.ENERGY.ordinal()] : 0.0f;
            }

            {//TODO ask chatgpt, that we have programmed a generic heatUsefulness parameter, but it is not locally reflected in this corp
                boolean corpOnTable = playedCardActions.containsKey(CardAction.SULTIRA_CORPORATION);

                if (corpOnTable) {
                    float totalCostInHand = 0f;
                    int count = 0;
                    for (Card c : hand) {
                        if (c.getTags().contains(Tag.ENERGY)) {
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
                    features[idx++] = tagCounts[Tag.ANIMAL.ordinal()] + tagCounts[Tag.PLANT.ordinal()] + tagCounts[Tag.MICROBE.ordinal()];
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                int extraCardsToDraftPotential = 0;
                int extraCardsToTakePotential = 0;

                if (handCardActions.containsKey(CardAction.THARSIS_CORPORATION)) {
                    extraCardsToDraftPotential++;
                    extraCardsToTakePotential++;
                }

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

                if (handCardActions.containsKey(CardAction.BACTERIAL_AGGREGATES)) {//TODO in can give potentially up to 4 extra draft potential, should also be handled separatel
                    extraCardsToDraftPotential++;
                }

                if (handCardActions.containsKey(CardAction.EXTENDED_RESOURCES)) {
                    extraCardsToTakePotential++;
                }

                features[idx++] = extraCardsToDraftPotential;
                features[idx++] = extraCardsToTakePotential;

                features[idx++] = draftCardsService.countExtraCardsToDraft(player);
                features[idx++] = draftCardsService.countExtraCardsToTake(player);
            }

            {
                features[idx++] = handCardActions.containsKey(CardAction.INTERPLANETARY_RELATIONS) ? 1 : 0;
                features[idx++] = playedCardActions.containsKey(CardAction.INTERPLANETARY_RELATIONS) ? 1 : 0;
                features[idx++] = handCardActions.containsKey(CardAction.INTERPLANETARY_RELATIONS) ? (player.getPlayed().size() - 1) : 0;//we get 1vp per 4 cards, this will be normalized, describe only potential
            }

            //TODO check this
            Map<Class<?>, Float> winPointsFromCardWithResources = winPointsService.getWinPointsFromCardWithResources(player, hand);

            features[idx++] = winPointsFromCardWithResources.getOrDefault(ImmigrationShuttles.class, (float) 0);
            features[idx++] = winPointsFromCardWithResources.getOrDefault(ConservedBiome.class, (float) 0);//TODO not worked on this card yet
            features[idx++] = winPointsFromCardWithResources.size();         // TODO we need to know the absolute maximum


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

            float oxygenProgress = (float) game.getPlanet().getCurrentLevel(GlobalParameter.OXYGEN) / (float) O2_MAX_LIMIT;
            float temperatureProgress = (float) game.getPlanet().getCurrentLevel(GlobalParameter.TEMPERATURE) / (float) TEMP_MAX_LIMIT;
            float oceansProgress = game.getPlanet().oceansBuilt() / OCEAN_MAX_NORM;

            features[idx++] = oxygenProgress;
            features[idx++] = 1f - oxygenProgress;

            features[idx++] = temperatureProgress;
            features[idx++] = 1f - temperatureProgress;

            features[idx++] = oceansProgress;
            features[idx++] = 1f - oceansProgress;


            float totalProgress = (oxygenProgress + temperatureProgress + oceansProgress) / 3f;
            features[idx++] = totalProgress;
            features[idx++] = 1f - totalProgress;

            Map<Tag, Long> tagRequirementsTotal =//TODO CHECK ALL TAG REQUIREMENTS
                    hand.stream()
                            .flatMap(card -> card.getTagRequirements().stream())
                            .collect(Collectors.groupingBy(
                                    Function.identity(),
                                    Collectors.counting()
                            ));

            features[idx++] = tagRequirementsTotal.getOrDefault(Tag.JUPITER, 0L);
            features[idx++] = tagRequirementsTotal.getOrDefault(Tag.ENERGY, 0L);


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

            features[idx++] = howManyScienceWasPlayed; // [133] how many we have now

            // Gap: насколько далеки от выполнения
            float scienceGap = 0;
            for (Card card : hand) {
                if (card.getTagRequirements().contains(Tag.SCIENCE)) {
                    scienceGap += Math.max(0, card.getTagRequirements().size() - howManyScienceWasPlayed);
                }
            }
            features[idx++] = scienceGap;           // [134] Total gap

            features[idx++] = (handCardClasses.contains(AdvancedEcosystems.class)) ? 1 : 0;//3 specific tag requirements that are only required by this card
            features[idx++] = (handCardClasses.contains(Crater.class)) ? 1 : 0;//requires 3 event tags. TODO need to add complex analis. How close are we to getting?
            features[idx++] = (handCardClasses.contains(PrivateInvestorBeach.class)) && game.getMilestones().stream().anyMatch(milestone -> milestone.isAchieved(player)) ? 1 : 0;//requires milestone achieved TODO complex analys of how close to the milestone? there is another similar card that gives 4 heat


            List<Milestone> milestones = game.getMilestones();
            for (int i = 0; i < milestones.size(); i++) {
                Milestone m = milestones.get(i);

                // 1. Мой прогресс (0.0 - 1.0)
                features[idx++] = (float) m.getValue(player, cardService) / m.getMaxValue();

                // 2. Прогресс оппонента
                features[idx++] = (float) m.getValue(anotherPlayer, cardService) / m.getMaxValue();
                ;

                // 3. Статус (Кто уже захватил?)
                if (!m.isAchieved()) {
                    features[idx++] = 1.0f; // Никто
                } else if (m.isAchieved(player)) {
                    features[idx++] = 0.0f; // Я (уже не актуально для прогресса)
                } else {
                    features[idx++] = -1.0f; // Противник (забудь про этот майлстоун)
                }
            }

            features[idx++] = game.getMilestones().stream().anyMatch(milestone -> milestone.isAchieved(player)) ? 1 : 0;//did we achieve any milestone? used for cards that require milestones to be achieved
            features[idx++] = game.getMilestones().stream().filter(milestone -> milestone.isAchieved(player)).count();//how many milestones we have achieved
            features[idx++] = game.getMilestones().size() - game.getMilestones().stream().filter(Milestone::isAchieved).count();//how many milestones to achieve have left


//            boolean advancedAlloysInHand = handCardClasses.contains(AdvancedAlloys.class);
//            boolean advancedAlloysOnTable = playedCardClasses.contains(AdvancedAlloys.class);
//            boolean phobologCorp = playedCardActions.containsKey(CardAction.PHOBOLOG_CORPORATION);
//
//            features[idx++] = advancedAlloysInHand ? 1 : 0;
//            features[idx++] = advancedAlloysOnTable ? 1 : 0;
//            features[idx++] = phobologCorp ? 1 : 0;
//
//            if (advancedAlloysInHand || advancedAlloysOnTable || phobologCorp) {
//                features[idx++] = (player.getTitaniumIncome() * ((phobologCorp ? 1 : 0) + ((advancedAlloysInHand || advancedAlloysOnTable) ? 1 : 0)));
//            } else {
//                features[idx++] = 0;
//            }
//
//            if (advancedAlloysInHand || advancedAlloysOnTable) {
//                features[idx++] = player.getSteelIncome();
//            } else {
//                features[idx++] = 0;
//            }
//
//
//            {//steel+titanium discount potential
//                float steelValue = 2 + (advancedAlloysOnTable ? 1 : 0);
//                float titaniumValue = 3 + (advancedAlloysOnTable ? 1 : 0) + (exocorpOnTable ? 1 : 0);
//
//                float steelPower = player.getSteelIncome() * steelValue;
//                float titaniumPower = player.getTitaniumIncome() * titaniumValue;
//
//                float steelEfficiency = Math.min(1.0f, steelPower / 24.71f);
//                float titaniumEfficiency = Math.min(1.0f, titaniumPower / 27.21f);
//
//                features[idx++] = steelEfficiency * tagCounts[Tag.BUILDING.ordinal()];
//                features[idx++] = titaniumEfficiency * tagCounts[Tag.SPACE.ordinal()];
//            }

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

                int buildingTagsInHand = tagCounts[Tag.BUILDING.ordinal()];
                int spaceTagsInHand    = tagCounts[Tag.SPACE.ordinal()];

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



            int[] costBuckets = new int[5];
            // [0]: 0-5 MC (early game)
            // [1]: 6-12 MC (mid-early)
            // [2]: 13-20 MC (mid)
            // [3]: 21-30 MC (mid-late)
            // [4]: 31+ MC (late game)

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

            return features;
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

        private float getMinRedAvailabilityProgress(GlobalParameter globalParameter) {
            int currentLevel = game.getPlanet().getCurrentLevel(globalParameter);
            int targetTemperatureLevel = getTemperatureStepFromColor(canAmplifyOxygenOrTemperature ? ParameterColor.P : ParameterColor.R, false);

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

        /**
         * Cards that have an action that lets them put microbe on another card
         */
        private int getMicrobeProductionPower() {
            int power = 0;
            if (playedCardActions.containsKey(CardAction.CONSERVED_BIOME)) {
                power++;
            }
            if (playedCardActions.containsKey(CardAction.EXTREME_COLD_FUNGUS)) {
                power++;
            }
            if (playedCardActions.containsKey(CardAction.SYMBIOTIC_FUNGUD)) {
                power++;
            }
            return power;
        }

        private float[] handleProducingConsumingMicrobeSynergy() {//TODO test this method
            boolean temperatureMax = game.getPlanet().isTemperatureMax();
            boolean oxygenMax = game.getPlanet().isOxygenMax();
            boolean oceansMax = game.getPlanet().isOceansMax();

            long playedProducingMicrobeCards = playedCards.stream().filter(Card::producesMicrobe).count();
            long playedConsumingMicrobeCards = playedCards.stream().filter(Card::consumesMicrobe).count();

            long handProducingMicrobeCards = hand.stream().filter(Card::producesMicrobe).count();
            long handConsumingMicrobeCards = hand.stream().filter(Card::consumesMicrobe).count();

            if (handCardClasses.contains(ExtremeColdFungus.class) || handCardClasses.contains(BuffedExtremeColdFungus.class)) {
                ParameterColor temperatureColor = game.getPlanet().getTemperatureColor();
                if (temperatureColor == Y || temperatureColor == W || temperatureColor == R && !canAmplifyOxygenOrTemperature) {
                    handProducingMicrobeCards--;
                }
            }

            if (handCardClasses.contains(GhgProductionBacteria.class) || handCardClasses.contains(RegolithEaters.class) || handCardClasses.contains(SymbioticFungus.class)) {
                ParameterColor oxygenColor = game.getPlanet().getOxygenColor();
                if (oxygenColor == P && !canAmplifyOxygenOrTemperature) {
                    if (handCardClasses.contains(GhgProductionBacteria.class)) {
                        handProducingMicrobeCards--;
                        handConsumingMicrobeCards--;
                    }
                    if (handCardClasses.contains(RegolithEaters.class)) {
                        handProducingMicrobeCards--;
                        handConsumingMicrobeCards--;
                    }
                    if (handCardClasses.contains(SymbioticFungus.class)) {
                        handProducingMicrobeCards--;
                    }
                }
            }

            if (temperatureMax) {
                if (handCardClasses.contains(GhgProductionBacteria.class) || handCardClasses.contains(BuffedGhgProductionBacteria.class)) {
                    handConsumingMicrobeCards--;
                }
                if (playedCardClasses.contains(GhgProductionBacteria.class) || playedCardClasses.contains(BuffedGhgProductionBacteria.class)) {
                    playedConsumingMicrobeCards--;
                }
            }

            if (oxygenMax) {
                if (handCardClasses.contains(RegolithEaters.class)) {
                    handConsumingMicrobeCards--;
                }
                if (playedCardClasses.contains(RegolithEaters.class)) {
                    playedConsumingMicrobeCards--;
                }
            }

            if (oceansMax) {
                if (handCardClasses.contains(NitriteReductingBacteria.class)) {
                    handConsumingMicrobeCards--;
                }
                if (playedCardClasses.contains(NitriteReductingBacteria.class)) {
                    playedConsumingMicrobeCards--;
                }
            }

            return new float[]{playedProducingMicrobeCards, Math.max(0, playedConsumingMicrobeCards), Math.max(0, handProducingMicrobeCards), Math.max(0, handConsumingMicrobeCards)};
        }

        private float countAnimalConsumingCards(Set<Class<?>> cardClasses) {
            float count = 0;
            if (cardClasses.contains(ArclightCorporation.class) || cardClasses.contains(BuffedArclightCorporation.class)) {
                count += 0.5f;
            }
            if (cardClasses.contains(Birds.class)) {
                count += 1f;
            }
            if (cardClasses.contains(EcologicalZone.class)) {
                count += 0.5f;
            }
            if (cardClasses.contains(FilterFeeders.class)) {
                count += 0.33f;
            }
            if (cardClasses.contains(Fish.class)) {
                count += 1f;
            }
            if (cardClasses.contains(Herbivores.class)) {
                count += 0.5f;
            }
            if (cardClasses.contains(Livestock.class)) {
                count += 1f;
            }
            if (cardClasses.contains(SmallAnimals.class)) {
                count += 0.5f;
            }
            if (cardClasses.contains(Zoos.class)) {
                count += 1f;
            }
            return count;
        }

        private int handleGlobalParameterSynergy(Set<Class<?>> cardClasses, float[] features, int idx) {//TODO REVIEW BECAUSE THIS DUPLICATES TOO MUCH
            float oceanNotMax = game.getPlanet().isOceansMax() ? 0 : 1;
            float tempNotMax = game.getPlanet().isTemperatureMax() ? 0 : 1;
            float oxygenNotMax = game.getPlanet().isOxygenMax() ? 0 : 1;

            boolean hasTripleTrigger = cardClasses.contains(Herbivores.class);

            features[idx++] = oceanNotMax * (
                    (cardClasses.contains(ArcticAlgae.class) ? (4 + 4 * oxygenNotMax) : 0) + // 4 зелени оцениваем как ~8 монет
                            (cardClasses.contains(Fish.class) ? 1.0f : 0) +
                            (hasTripleTrigger ? 0.5f : 0)
            );


            features[idx++] = tempNotMax * (
                    (hasTripleTrigger ? 0.5f : 0) +
                            (cardClasses.contains(Livestock.class) ? 1.0f : 0) +
                            (cardClasses.contains(PhysicsComplex.class) ? 0.5f : 0)
            );


            features[idx++] = oxygenNotMax * (
                    (hasTripleTrigger ? 0.5f : 0)
            );


            features[idx++] = (cardClasses.contains(SmallAnimals.class) ? 0.5f : 0);

            return 4;
        }

        //TODO unused method?
        private boolean canBuildACardWithExtraAmplify(Card card) {
            if (!card.getTemperatureRequirement().isEmpty()) {
                ParameterColor currentTempColor = game.getPlanet().getTemperatureColor();
                List<ParameterColor> reqs = card.getTemperatureRequirement();

                if (!reqs.contains(currentTempColor)) {
                    if (canMatchWithShift(currentTempColor, reqs)) {
                        return true;
                    }
                }
            }

            if (!card.getOxygenRequirement().isEmpty()) {
                ParameterColor currentO2Color = game.getPlanet().getOxygenColor();
                List<ParameterColor> reqs = card.getOxygenRequirement();

                if (!reqs.contains(currentO2Color)) {
                    if (canMatchWithShift(currentO2Color, reqs)) {
                        return true;
                    }
                }
            }

            return false;
        }

        private boolean canMatchWithShift(ParameterColor currentColor, List<ParameterColor> requirements) {
            int currentIdx = currentColor.ordinal();
            ParameterColor[] allColors = ParameterColor.values(); // P, R, Y, W

            if (currentIdx > 0) {
                if (requirements.contains(allColors[currentIdx - 1])) {
                    return true;
                }
            }

            if (currentIdx < allColors.length - 1) {
                if (requirements.contains(allColors[currentIdx + 1])) {
                    return true;
                }
            }

            return false;
        }

        private int getImmediateMicrobeGainSum() {
            return (handCardActions.containsKey(CardAction.ASTROFARM) ? 2 : 0) + (handCardActions.containsKey(CardAction.IMPORTED_NITROGEN) ? 3 : 0);
        }

        private int getImmediateAnimalGainSum() {
            return (handCardActions.containsKey(CardAction.EOS_CHASMA) ? 1 : 0) + (handCardActions.containsKey(CardAction.IMPORTED_NITROGEN) ? 2 : 0);
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


        private class FeatureStats {
            float sum, mean, max, min, std, median;
            int nonZeroCount;
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
        //TODO отображает только если мы стремимся к требованию, например нам нужен кислород выше, сделать так, чтобы мы и в обратку смотрели, мол сейчас кислород повысится и что-то заблокируется
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
