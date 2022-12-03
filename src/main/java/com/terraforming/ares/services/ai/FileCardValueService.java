package com.terraforming.ares.services.ai;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.PaymentValidationService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.ai.dto.CardValueResponse;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

/**
 * Created by oleksii.nikitin
 * Creation date 25.11.2022
 */
@Service
public class FileCardValueService {
    private static final double MAX_PRIORITY = 10.0;
    private final CardService cardService;
    private final PaymentValidationService paymentValidationService;
    private final SpecialEffectsService specialEffectsService;

    public FileCardValueService(CardService cardService, PaymentValidationService paymentValidationService, SpecialEffectsService specialEffectsService) throws IOException {
        this.cardService = cardService;
        this.paymentValidationService = paymentValidationService;
        this.specialEffectsService = specialEffectsService;

        try(FileReader fr = new FileReader("cardStats.txt");
            BufferedReader reader = new BufferedReader(fr)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    line = line.substring(1);
                    final int cardId = Integer.parseInt(line.split(" ")[0]);
                } else if (line.startsWith(".")) {
                    line = line.substring(1);

                }
            }
        }
    }

    public CardValueResponse getWorstCard(MarsGame game, Player player, List<Integer> cards, int turn) {
        double firstHalfCoefficient;
        double secondHalfCoefficient;
        final double middleTurn = getMiddleTurn(game);
        if (turn < middleTurn) {
            firstHalfCoefficient = (double) turn / middleTurn;
            secondHalfCoefficient = 1.0 - firstHalfCoefficient;
        } else {
            firstHalfCoefficient = 0.0;
            secondHalfCoefficient = 1.0;
        }

        double worstCardValue = 500.0;
        int worstCardIndex = 0;

        for (int i = 0; i < cards.size(); i++) {
            double cardValue = getCardValue(cards.get(i), firstHalfCoefficient, secondHalfCoefficient, game, player, turn);
            if (cardValue < worstCardValue) {
                worstCardValue = cardValue;
                worstCardIndex = i;
            }
        }


        return CardValueResponse.of(cards.get(worstCardIndex), worstCardValue);
    }

    public Card getBestCardAsCard(MarsGame game, Player player, List<Card> cards, int turn, boolean ignoreCardIfBad) {
        if (cards.isEmpty()) {
            return null;
        }

        final double middleTurn = getMiddleTurn(game);

        double firstHalfCoefficient;
        double secondHalfCoefficient;
        if (turn < middleTurn) {
            firstHalfCoefficient = (double) turn / middleTurn;
            secondHalfCoefficient = 1.0 - firstHalfCoefficient;
        } else {
            firstHalfCoefficient = 0.0;
            secondHalfCoefficient = 1.0;
        }

        double bestCardWorth = 0.0;
        Card bestCard = cards.get(0);

        for (int i = 0; i < cards.size(); i++) {
            double worth = getCardValue(cards.get(i).getId(), firstHalfCoefficient, secondHalfCoefficient, game, player, turn);
            if (worth >= bestCardWorth) {
                bestCardWorth = worth;
                bestCard = cards.get(i);
            }
        }

        if (ignoreCardIfBad && bestCardWorth <= 50.0) {
            return null;
        }

        return bestCard;
    }

    public Integer getBestCard(MarsGame game, Player player, List<Integer> cards, int turn) {
        double firstHalfCoefficient;
        double secondHalfCoefficient;

        final double middleTurn = getMiddleTurn(game);

        if (turn < middleTurn) {
            firstHalfCoefficient = (double) turn / middleTurn;
            secondHalfCoefficient = 1.0 - firstHalfCoefficient;
        } else {
            firstHalfCoefficient = 0.0;
            secondHalfCoefficient = 1.0;
        }

        double bestCard = 0.0;
        int bestCardId = 0;

        for (int i = 0; i < cards.size(); i++) {
            double worth = getCardValue(cards.get(i), firstHalfCoefficient, secondHalfCoefficient, game, player, turn);
            if (worth >= bestCard) {
                bestCard = worth;
                bestCardId = cards.get(i);
            }
        }

        return bestCardId;
    }

    private double getCardCoefficient(MarsGame game, Player player, Card card, int turn) {
        final double middleTurn = getMiddleTurn(game);
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

                        if (cardAction == CardAction.STEELWORKS || cardAction == CardAction.IRON_WORKS) {
                            if (player.getHeatIncome() > 5) {
                                return 1.5;
                            } else {
                                return 1.25;
                            }
                        }

                        if (cardAction == CardAction.CARETAKER_CONTRACT) {
                            if (player.getHeatIncome() >= 8) {
                                return 1.5;
                            } else if (player.getHeatIncome() >= 4) {
                                return 1.25;
                            }
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

    private double getCardValue(Integer card, double firstHalfCoefficient, double secondHalfCoefficient, MarsGame game, Player player, int turn) {
        return cardToWeightFirstHalf.get(card) * firstHalfCoefficient + cardToWeightSecondHalf.get(card) * secondHalfCoefficient
                * getCardCoefficient(game, player, cardService.getCard(card), turn);
    }

}
