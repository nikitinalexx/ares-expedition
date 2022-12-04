package com.terraforming.ares.services.ai;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.dto.GameStatistics;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.PaymentValidationService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.ai.dto.CardValue;
import com.terraforming.ares.services.ai.dto.CardValueResponse;
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
    private final PaymentValidationService paymentValidationService;
    private final SpecialEffectsService specialEffectsService;
    private final AiBalanceService aiBalanceService;

    private Map<Integer, Map<Integer, CardValue>> cardIdToTurnToValueFirstPlayer;
    private Map<Integer, Map<Integer, CardValue>> cardIdToTurnToValueSecondPlayer;


    public FileCardValueService(CardService cardService, PaymentValidationService paymentValidationService, SpecialEffectsService specialEffectsService, AiBalanceService aiBalanceService) throws IOException {
        this.cardService = cardService;
        this.paymentValidationService = paymentValidationService;
        this.specialEffectsService = specialEffectsService;
        this.aiBalanceService = aiBalanceService;

        this.cardIdToTurnToValueFirstPlayer = initCardStatsFromFile("cardStatsSmartVsSmart.txt");
        this.cardIdToTurnToValueSecondPlayer = initCardStatsFromFile("cardStatsSmartVsSmart.txt");
    }

    private Map<Integer, Map<Integer, CardValue>> initCardStatsFromFile(String fileName) throws IOException {
        Map<Integer, Map<Integer, CardValue>> result = new HashMap<>();
        try(FileReader fr = new FileReader(fileName);
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
                    if (turnWithOccurence[1].equals("NaN")) {
                        cardValue = CardValue.builder().isNan(true).build();
                    } else {
                        cardValue = CardValue.builder().value(Double.parseDouble(turnWithOccurence[1])).build();
                    }
                    result.get(cardId).put(turnId, cardValue);

                }
            }
            System.out.println("Completed reading file");
        }
        return result;
    }

    public CardValueResponse getWorstCard(MarsGame game, Player player, List<Integer> cards, int turn) {
        double worstCardValue = 500.0;
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

                        return null;
                    }
            ).orElse(1.0);
        }

        int cardDiscount = paymentValidationService.getDiscount(card, player);

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.ENERGY_SUBSIDIES_DISCOUNT_4)) {
            cardDiscount += countCardTags(card, Tag.ENERGY) * 4;
        }

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.INTERPLANETARY_CONFERENCE)) {
            cardDiscount += countCardTags(card, Tag.EARTH) * 4;
            cardDiscount += countCardTags(card, Tag.JUPITER) * 4;
        }

        if (card.getTags().contains(Tag.EVENT) && hasCardAction(player, CardAction.OPTIMAL_AEROBRAKING)) {
            cardDiscount += 8;
        }

        if (card.getTags().contains(Tag.EVENT) && hasCardAction(player, CardAction.RECYCLED_DETRITUS)) {
            cardDiscount += 8;
        }

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

        Map<Integer, CardValue> turnToCardValue = (player.getUuid().startsWith("0") ? cardIdToTurnToValueFirstPlayer.get(card) : cardIdToTurnToValueSecondPlayer.get(card));

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

}