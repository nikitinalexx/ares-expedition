package com.terraforming.ares.services.ai;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.blue.ArcticAlgae;
import com.terraforming.ares.cards.blue.GasCooledReactors;
import com.terraforming.ares.cards.blue.HohmannTransferShipping;
import com.terraforming.ares.cards.blue.VolcanicSoil;
import com.terraforming.ares.cards.green.*;
import com.terraforming.ares.cards.red.*;
import com.terraforming.ares.dto.GameStatistics;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DiscountService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.ai.dto.CardValue;
import com.terraforming.ares.services.ai.dto.CardValueResponse;
import com.terraforming.ares.services.ai.dto.ReducedCardValue;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by oleksii.nikitin
 * Creation date 25.11.2022
 */
@Service
public class FileCardValueService implements ICardValueService {
    private static final double MAX_PRIORITY = 10.0;
    private final CardService cardService;
    private final DiscountService discountService;
    private final SpecialEffectsService specialEffectsService;
    private final AiBalanceService aiBalanceService;

    private Map<Integer, Map<Integer, CardValue>> cardIdToTurnToValueTwoPlayer;
    private Map<Integer, Map<Integer, CardValue>> cardIdToTurnToValueFourPlayer;


    public FileCardValueService(CardService cardService,
                                SpecialEffectsService specialEffectsService,
                                AiBalanceService aiBalanceService,
                                DiscountService discountService) throws IOException {
        this.cardService = cardService;
        this.specialEffectsService = specialEffectsService;
        this.aiBalanceService = aiBalanceService;
        this.discountService = discountService;

        this.cardIdToTurnToValueTwoPlayer = initCardStatsFromFile("cardStatsExpansionRandom.txt", 1);
        this.cardIdToTurnToValueFourPlayer = initCardStatsFromFile("cardStatsRandom.txt", 2);
    }

    private Map<Integer, Map<Integer, CardValue>> initCardStatsFromFile(String fileName, int multiplier) throws IOException {
        Map<Integer, Map<Integer, CardValue>> result = new HashMap<>();
        try (FileReader fr = new FileReader(fileName);
             BufferedReader reader = new BufferedReader(fr)) {
            String line;
            int cardId = -1;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    line = line.substring(1);
                    cardId = Integer.parseInt(line.split(" ")[0]);
                    result.put(cardId, new HashMap<>());
                } else if (line.startsWith(".")) {
                    line = line.substring(1);
                    String[] turnWithOccurence = line.split(" ");
                    final int turnId = Integer.parseInt(turnWithOccurence[0]);
                    CardValue cardValue;
                    if (turnWithOccurence[1].equals("NaN") || turnWithOccurence[1].equals("0.0")) {
                        cardValue = CardValue.builder().isNan(true).build();
                    } else {
                        cardValue = CardValue.builder().value(Double.parseDouble(turnWithOccurence[1]) * multiplier).build();
                    }
                    result.get(cardId).put(turnId, cardValue);

                }
            }
            System.out.println("Completed reading file");
        }
        return result;
    }

    public CardValueResponse getWorstCard(MarsGame game, Player player, List<Integer> cards, int turn) {
        double worstCardValue = Float.MAX_VALUE;
        int worstCardIndex = 0;

        for (int i = 0; i < cards.size(); i++) {
            double cardValue = getCardValue(cards.get(i), game, player, turn);
            if (cardValue < worstCardValue) {
                worstCardValue = cardValue;
                worstCardIndex = i;
            }
        }


        return CardValueResponse.of(cards.get(worstCardIndex), worstCardValue);
    }

    public Card getBestCardToBuild(MarsGame game, Player player, List<Card> cards, int turn, boolean ignoreCardIfBad) {
        if (cards.isEmpty()) {
            return null;
        }

        double bestCardWorth = 0.0;
        Card bestCard = cards.get(0);

        for (int i = 0; i < cards.size(); i++) {
            double worth = getCardValue(cards.get(i).getId(), game, player, turn);
            if (worth >= bestCardWorth) {
                bestCardWorth = worth;
                bestCard = cards.get(i);
            }
        }

        if (ignoreCardIfBad && bestCardWorth < aiBalanceService.getMinimumBuildCardWorth()) {
            return null;
        }

        return bestCard;
    }

    public Integer getBestCard(MarsGame game, Player player, List<Integer> cards, int turn) {
        double bestCard = 0.0;
        int bestCardId = 0;

        for (int i = 0; i < cards.size(); i++) {
            double worth = getCardValue(cards.get(i), game, player, turn);
            if (worth >= bestCard) {
                bestCard = worth;
                bestCardId = cards.get(i);
            }
        }

        return bestCardId;
    }

    private double getCardCoefficient(MarsGame game, Player player, Card card, int turn) {
        final double middleTurn = 10;
        double coefficient = 1.0;
        if (card.getSpecialEffects().contains(SpecialEffect.ADVANCED_ALLOYS)) {
            if (turn < middleTurn * 1.75 && (player.getSteelIncome() > 0 || player.getTitaniumIncome() > 1)) {
                coefficient = 1.3;
            }
        } else if (card.getSpecialEffects().contains(SpecialEffect.SOLD_CARDS_COST_1_MC_MORE)) {
            coefficient = 1.3;
        } else if (card.getSpecialEffects().contains(SpecialEffect.ENERGY_SUBSIDIES_DISCOUNT_4)
                || card.getSpecialEffects().contains(SpecialEffect.INTERPLANETARY_CONFERENCE)
                || card.getSpecialEffects().contains(SpecialEffect.MEDIA_GROUP)) {
            if (turn < middleTurn * 1.25) {
                coefficient = 1.3;
            }
        } else if (card.getSpecialEffects().contains(SpecialEffect.EXTENDED_RESOURCES)
                || card.getSpecialEffects().contains(SpecialEffect.INTERNS)
                || card.getSpecialEffects().contains(SpecialEffect.UNITED_PLANETARY_ALLIANCE)) {
            if (turn < middleTurn) {
                coefficient = 1.5;
            }
        } else {
            coefficient = Optional.ofNullable(card.getCardMetadata()).map(CardMetadata::getCardAction).map(
                    cardAction -> {
                        if (cardAction == CardAction.RECYCLED_DETRITUS && turn < middleTurn * 1.25) {
                            return 1.5;
                        }
                        if (cardAction == CardAction.RESTRUCTURED_RESOURCES && turn < middleTurn * 1.25) {
                            return 1.5;
                        }
                        if ((cardAction == CardAction.ANTI_GRAVITY_TECH || cardAction == CardAction.AI_CENTRAL) && turn < middleTurn * 1.75) {
                            if (cardService.countPlayedTags(player, Set.of(Tag.SCIENCE)) >= 5) {
                                return MAX_PRIORITY;
                            } else {
                                return 1.5;
                            }
                        }
                        if (cardAction == CardAction.OLYMPUS_CONFERENCE || cardAction == CardAction.MARS_UNIVERSITY) {
                            return 1.3;
                        }
                        if (cardAction == CardAction.AQUIFER_PUMPING && player.getSteelIncome() < 3) {
                            return 0.0;
                        }
                        if ((cardAction == CardAction.AQUIFER_PUMPING
                                || cardAction == CardAction.ARCTIC_ALGAE
                                || cardAction == CardAction.FISH
                                || cardAction == CardAction.NITRITE_REDUCTING
                                || cardAction == CardAction.VOLCANIC_POOLS)
                                && game.getPlanet().isOceansMax()) {
                            return 0.0;
                        }
                        if (cardAction == CardAction.VOLCANIC_POOLS) {
                            if (player.getTitaniumIncome() <= 2 || cardService.countPlayedTags(player, Set.of(Tag.ENERGY)) < 4) {
                                return 0.5;
                            }
                        }

                        if ((cardAction == CardAction.DEVELOPED_INFRASTRUCTURE
                                || cardAction == CardAction.GHG_PRODUCTION
                                || cardAction == CardAction.LIVESTOCK
                                || cardAction == CardAction.WOOD_BURNING_STOVES) && game.getPlanet().isTemperatureMax()) {
                            return 0.0;
                        }
                        if ((cardAction == CardAction.FARMING_COOPS
                                || cardAction == CardAction.GREEN_HOUSES
                                || cardAction == CardAction.IRON_WORKS
                                || cardAction == CardAction.REGOLITH_EATERS
                                || cardAction == CardAction.STEELWORKS
                                || cardAction == CardAction.PROGRESSIVE_POLICIES) && game.getPlanet().isOxygenMax()) {
                            return 0.0;
                        }

                        if (cardAction == CardAction.HEAT_EARTH_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.EARTH));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.MC_ANIMAL_PLANT_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.ANIMAL, Tag.PLANT));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.MC_ANIMAL_PLANT_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.ANIMAL, Tag.PLANT));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.CARD_SCIENCE_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.SCIENCE));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.MC_EARTH_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.EARTH));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.PLANT_PLANT_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.PLANT));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.MC_SCIENCE_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.SCIENCE));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.MC_2_BUILDING_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.BUILDING));
                            if (playedTags > 1) {
                                return 1.0 + playedTags * 0.025;
                            }
                        }

                        if (cardAction == CardAction.MC_ENERGY_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.ENERGY));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.MC_SPACE_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.SPACE));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.HEAT_SPACE_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.SPACE));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.MC_EVENT_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.EVENT));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.HEAT_ENERGY_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.ENERGY));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.PLANT_MICROBE_INCOME) {
                            int playedTags = cardService.countPlayedTags(player, Set.of(Tag.MICROBE));
                            if (playedTags > 0) {
                                return 1.0 + playedTags * 0.05;
                            }
                        }

                        if (cardAction == CardAction.MC_FOREST_INCOME) {
                            if (player.getForests() > 0) {
                                return 1.0 + player.getForests() * 0.05;
                            }
                        }

                        if ((cardAction == CardAction.STEELWORKS || cardAction == CardAction.IRON_WORKS || cardAction == CardAction.CARETAKER_CONTRACT) && player.getHeatIncome() >= 8 && game.getPlanetAtTheStartOfThePhase().isTemperatureMax()) {
                            return 1.1;
                        }

                        if (cardAction == CardAction.POWER_INFRASTRUCTURE && game.getPlanet().isTemperatureMax()) {
                            return MAX_PRIORITY;
                        }

                        if (cardAction == CardAction.INNOVATIVE_TECHNOLOGIES || cardAction == CardAction.GAS_COOLED_REACTORS) {
                            return 1 + (0.1 * player.countPhaseUpgrades());
                        }

                        if (cardAction == CardAction.TOURISM || cardAction == CardAction.CITY_COUNCIL || cardAction == CardAction.COMMUNITY_AFFORESTATION) {
                            int milestonesAchieved = (int) game
                                    .getMilestones()
                                    .stream()
                                    .filter(milestone -> milestone.isAchieved(player)).count();

                            return 1 + (0.5 * milestonesAchieved);
                        }

                        int phaseUpgrades = player.countPhaseUpgrades();
                        boolean allPhasesUpgraded = (phaseUpgrades == Constants.UPGRADEABLE_PHASES_COUNT);


                        if (cardAction == CardAction.EXPERIMENTAL_TECHNOLOGY && allPhasesUpgraded) {
                            return 0.5;
                        }

                        if ((cardAction == CardAction.FIBROUS_COMPOSITE_MATERIAL ||
                                cardAction == CardAction.SOFTWARE_STREAMLINING) && allPhasesUpgraded) {
                            return 0.8;
                        }


                        return null;
                    }
            ).orElse(1.0);
        }

        int cardDiscount = getTotalCardDiscount(card, game, player);

        double ratio = ((double) cardDiscount / card.getPrice());

        double discountCoefficient = 1.00;
        if (ratio > 0 && ratio < 0.25) {
            discountCoefficient = 1.05;
        } else if (ratio >= 0.25 && ratio < 0.5) {
            discountCoefficient = 1.15;
        } else if (ratio >= 0.5 && ratio < 0.75) {
            discountCoefficient = 1.25;
        } else if (ratio >= 0.75) {
            discountCoefficient = 1.35;
        }

        if (turn >= middleTurn * 1.25 && card.getWinningPoints() > 0) {
            discountCoefficient *= 1.25;
        }

        coefficient *= discountCoefficient;

        return coefficient;
    }

    private int getTotalCardDiscount(Card card, MarsGame game, Player player) {
        int cardDiscount = discountService.getDiscount(game, card, player, Map.of());

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.ENERGY_SUBSIDIES_DISCOUNT_4)) {
            cardDiscount += countCardTags(card, Tag.ENERGY) * 4;
        }

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.INTERPLANETARY_CONFERENCE)) {
            cardDiscount += countCardTags(card, Tag.EARTH) * 4;
            cardDiscount += countCardTags(card, Tag.JUPITER) * 4;
        }

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.ORBITAL_OUTPOST_DISCOUNT) && card.getTags().size() <= 1) {
            cardDiscount += 3;
        }

        if (card.getTags().contains(Tag.EVENT) && hasCardAction(player, CardAction.OPTIMAL_AEROBRAKING)) {
            cardDiscount += 8;
        }

        if (card.getTags().contains(Tag.EVENT) && hasCardAction(player, CardAction.IMPACT_ANALYSIS)) {
            cardDiscount += 3;
        }

        if (card.getTags().contains(Tag.EVENT) && hasCardAction(player, CardAction.RECYCLED_DETRITUS)) {
            cardDiscount += 8;
        }

        if (hasCardAction(player, CardAction.ANTI_GRAVITY_TECH)) {
            if (game.getPlanetAtTheStartOfThePhase().isTemperatureMax()) {
                cardDiscount += 1;
            } else {
                cardDiscount += 4;
            }
            if (game.getPlanetAtTheStartOfThePhase().isOxygenMax()) {
                cardDiscount += 2;
            } else {
                cardDiscount += 4;
            }
        }

        return cardDiscount;
    }

    private boolean hasCardAction(Player player, CardAction cardAction) {
        return player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .map(Card::getCardMetadata)
                .filter(Objects::nonNull)
                .map(CardMetadata::getCardAction)
                .filter(Objects::nonNull)
                .anyMatch(action -> action == cardAction);
    }

    private int countCardTags(Card card, Tag tag) {
        return (int) card.getTags().stream().filter(t -> t == tag).count();
    }

    private double getCardValue(Integer card, MarsGame game, Player player, int turn) {
        if (turn == 0) {
            turn++;
        }

        final ReducedCardValue reducedEventValue = reducedCardValue(game, player, card);
        if (reducedEventValue.isValueReduced()) {
            return reducedEventValue.getValue();
        }

        Map<Integer, CardValue> turnToCardValue = game.getPlayerUuidToPlayer().size() == 2 ? cardIdToTurnToValueTwoPlayer.get(card) : cardIdToTurnToValueFourPlayer.get(card);

        int totalStatsCount = 0;
        double totalValue = 0;
        for (int i = turn; i < turn + aiBalanceService.getFutureTurnCardStatsToConsider() && i <= GameStatistics.MAX_TURNS_TO_CONSIDER; i++) {
            CardValue cardValue = turnToCardValue.get(i);
            if (!cardValue.isNan()) {
                totalValue += cardValue.getValue();
                totalStatsCount++;
            }
        }

        return (totalStatsCount == 0 ? 0 : totalValue / totalStatsCount)
                * getCardCoefficient(game, player, cardService.getCard(card), turn);
    }

    private ReducedCardValue reducedCardValue(MarsGame game, Player player, Integer cardId) {
        final Card card = cardService.getCard(cardId);

        int defaultSenseInCard = 4;

        final int totalCardDiscount = getTotalCardDiscount(card, game, player);

        Class<? extends Card> cardType = card.getClass();
        if (game.getPlanetAtTheStartOfThePhase().isOceansMax()) {
            if (cardType == ArtificialLake.class
                    || cardType == Comet.class
                    || cardType == GiantIceAsteroid.class
                    || cardType == ImportedHydrogen.class
                    || cardType == LargeConvoy.class
                    || cardType == PhobosFalls.class
                    || cardType == TechnologyDemonstration.class
                    || cardType == TowingAComet.class
                    || cardType == ArcticAlgae.class) {
                defaultSenseInCard -= 2;//makes some sense
            }
            if (cardType == ConvoyFromEuropa.class) {
                defaultSenseInCard -= 3;//makes small sense
            }
            if (cardType == Crater.class
                    || cardType == IceAsteroid.class
                    || cardType == IceCapMelting.class
                    || cardType == LakeMariners.class
                    || cardType == PermafrostExtraction.class
                    || cardType == SubterraneanReservoir.class
                    || cardType == TrappedHeat.class
                    || cardType == PrivateInvestorBeach.class) {
                defaultSenseInCard -= 4;//makes no sense
            }
        }

        if (game.getPlanetAtTheStartOfThePhase().isOxygenMax()) {
            if (cardType == AtmosphereFiltering.class) {
                defaultSenseInCard -= 4;
            }
            if (cardType == AirborneRadiation.class) {
                defaultSenseInCard -= 3;
            }
            if (cardType == Mangrove.class
                    || cardType == Plantation.class
                    || cardType == BiothermalPower.class) {
                defaultSenseInCard -= 2;
            }
        }

        if (game.getPlanetAtTheStartOfThePhase().isTemperatureMax()) {
            if (cardType == Comet.class
                    || cardType == DeimosDown.class
                    || cardType == GiantIceAsteroid.class
                    || cardType == NitrogenRichAsteroid.class
                    || cardType == PhobosFalls.class
                    || cardType == OreLeaching.class
                    || cardType == VolcanicSoil.class) {
                defaultSenseInCard -= 2;
            }
            if (cardType == LavaFlows.class
                    || cardType == DeepWellHeating.class
                    || cardType == GasCooledReactors.class) {
                defaultSenseInCard -= 4;
            }
        }

        int phaseUpgrades = player.countPhaseUpgrades();
        boolean allPhasesUpgraded = (phaseUpgrades == Constants.UPGRADEABLE_PHASES_COUNT);

        if (cardType == BiomedicalImports.class && game.getPlanetAtTheStartOfThePhase().isOxygenMax() && allPhasesUpgraded) {
            defaultSenseInCard -= 4;
        }

        if ((cardType == CryogenicShipment.class ||
                cardType == Exosuits.class ||
                cardType == TopographicMapping.class ||
                cardType == Biofoundries.class ||
                cardType == BlastFurnaces.class ||
                cardType == ManufacturingHub.class ||
                cardType == HeatReflectiveGlass.class ||
                cardType == HydroponicGardens.class ||
                cardType == IndustrialComplex.class ||
                cardType == MartianMuseum.class ||
                cardType == Metallurgy.class ||
                cardType == PerfluorocarbonProduction.class ||
                cardType == MagneticFieldGenerator.class ||
                cardType == Warehouses.class ||
                cardType == HohmannTransferShipping.class)
                && allPhasesUpgraded) {
            defaultSenseInCard -= 3;
        }

        if (cardType == ImportedConstructionCrews.class) {
            if (phaseUpgrades == Constants.UPGRADEABLE_PHASES_COUNT) {
                defaultSenseInCard -= 4;
            } else if (phaseUpgrades == Constants.UPGRADEABLE_PHASES_COUNT - 1) {
                defaultSenseInCard -= 3;
            }
        }

        if (cardType == OreLeaching.class && allPhasesUpgraded) {
            defaultSenseInCard -= 1;
        }

        if (cardType == MartianStudiesScholarship.class && player.isPhaseUpgraded(Constants.BUILD_BLUE_RED_PROJECTS_PHASE)) {
            defaultSenseInCard -= 3;
        }

        if (cardType == BiologicalFactories.class && player.isPhaseUpgraded(Constants.COLLECT_INCOME_PHASE)) {
            defaultSenseInCard -= 3;
        }

        if (defaultSenseInCard == 4) {
            return ReducedCardValue.noReduce();
        } else if (defaultSenseInCard == 1) {
            if (card.getPrice() - totalCardDiscount < 5) {
                return ReducedCardValue.useReducedValue(45);//we can build it because card is cheap
            } else {
                return ReducedCardValue.useReducedValue(0);
            }
        } else if (defaultSenseInCard <= 0) {
            if (card.getPrice() - totalCardDiscount < 0) {
                return ReducedCardValue.useReducedValue(40);//we can build it because card costs nothing
            } else {
                return ReducedCardValue.useReducedValue(0);
            }
        }
        return ReducedCardValue.noReduce();
    }

}
