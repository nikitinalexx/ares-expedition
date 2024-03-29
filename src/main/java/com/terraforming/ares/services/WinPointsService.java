package com.terraforming.ares.services;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.buffedCorporations.BuffedArclightCorporation;
import com.terraforming.ares.cards.corporations.ArclightCorporation;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.winPoints.WinPointsInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 17.05.2022
 */
@Service
@RequiredArgsConstructor
public class WinPointsService {
    public final CardService cardService;


    public int countWinPoints(Player player, MarsGame game) {
        if (game.isCrysis()) {
            return countCrysisWinPoints(game);
        }
        int winPoints = player.getTerraformingRating();

        winPoints += player.getForests();

        List<Card> cards = player.getPlayed()
                .getCards()
                .stream()
                .map(cardService::getCard).collect(Collectors.toList());

        winPoints += cards
                .stream()
                .mapToInt(Card::getWinningPoints)
                .sum();


        winPoints += getWinPointsFromResourceCards(player, cards);

        winPoints += milestonesWinPoints(player, game);

        winPoints += awardsWinPoints(player, game);

        return winPoints + player.getExtraPoints();
    }

    public float countWinPointsWithFloats(Player player, MarsGame game) {
        float winPoints = player.getTerraformingRating();

        winPoints += player.getForests();

        List<Card> cards = player.getPlayed()
                .getCards()
                .stream()
                .map(cardService::getCard).collect(Collectors.toList());

        winPoints += cards
                .stream()
                .mapToInt(Card::getWinningPoints)
                .sum();


        winPoints += getWinPointsFromResourceCardsWithFloats(game, player, cards);

        winPoints += milestonesWinPoints(player, game);

        winPoints += awardsWinPoints(player, game);

        return winPoints;
    }

    public int countCrysisWinPoints(MarsGame game) {
        int winPoints = game.getCrysisData().getCrisisVp();

        for (Player player : game.getPlayerUuidToPlayer().values()) {
            winPoints += player.getForests();

            List<Card> cards = player.getPlayed()
                    .getCards()
                    .stream()
                    .map(cardService::getCard).collect(Collectors.toList());

            winPoints += cards
                    .stream()
                    .mapToInt(Card::getWinningPoints)
                    .sum();

            winPoints += getWinPointsFromResourceCards(player, cards);
        }

        return winPoints;
    }

    private int milestonesWinPoints(Player player, MarsGame game) {
        return (int) game.getMilestones().stream().filter(milestone -> milestone.isAchieved(player)).count() * 3;
    }

    private int awardsWinPoints(Player player, MarsGame game) {
        return game.getAwards().stream().mapToInt(award -> award.getPlayerWinPoints(player)).sum();
    }

    private int getWinPointsFromResourceCards(Player player, List<Card> cards) {
        return cards.stream()
                .filter(card -> card.getCardMetadata().getWinPointsInfo() != null)
                .mapToInt(
                        card -> {
                            WinPointsInfo winPointsInfo = card.getCardMetadata().getWinPointsInfo();

                            int resources = (winPointsInfo.getType() == card.getCollectableResource())
                                    ? player.getCardResourcesCount().get(card.getClass())
                                    : 0;

                            if (winPointsInfo.getType() == CardCollectableResource.FOREST) {
                                resources = player.getForests();
                            } else if (winPointsInfo.getType() == CardCollectableResource.JUPITER) {
                                resources = cardService.countPlayedTags(player, Set.of(Tag.JUPITER));
                            } else if (winPointsInfo.getType() == CardCollectableResource.BLUE_CARD) {
                                resources = cardService.countPlayedCards(player, Set.of(CardColor.BLUE));
                            } else if (winPointsInfo.getType() == CardCollectableResource.ANY_CARD) {
                                resources = cardService.countPlayedCards(player, Set.of(CardColor.BLUE, CardColor.GREEN, CardColor.RED));
                            } else if (winPointsInfo.getType() == CardCollectableResource.EARTH) {
                                resources = cardService.countPlayedTags(player, Set.of(Tag.EARTH));
                            }

                            return getWinPoints(resources, winPointsInfo.getPoints(), winPointsInfo.getResources());
                        }
                ).sum();
    }

    private static final List<Class<?>> CARDS_TO_EXCLUDE_FROM_FLOAT_WIN_POINTS_CHECK = List.of(
            ArclightCorporation.class,
            BuffedArclightCorporation.class,
            EcologicalZone.class,
            Herbivores.class,
            PhysicsComplex.class,
            SmallAnimals.class,
            Birds.class,
            Fish.class,
            Livestock.class,
            FilterFeeders.class,
            Tardigrades.class
    );

    private float getWinPointsFromResourceCardsWithFloats(MarsGame game, Player player, List<Card> cards) {
        Map<Class<?>, Integer> collectableResources = player.getCardResourcesCount();

        int halfPointResources = collectableResources.getOrDefault(ArclightCorporation.class, 0) + collectableResources.getOrDefault(BuffedArclightCorporation.class, 0) + collectableResources.getOrDefault(EcologicalZone.class, 0)
                + collectableResources.getOrDefault(Herbivores.class, 0)
                + collectableResources.getOrDefault(PhysicsComplex.class, 0)
                + collectableResources.getOrDefault(SmallAnimals.class, 0);

        int onePointAnimals = collectableResources.getOrDefault(Birds.class, 0) + collectableResources.getOrDefault(Fish.class, 0) + collectableResources.getOrDefault(Livestock.class, 0);

        int oneThirdPointResources = collectableResources.getOrDefault(FilterFeeders.class, 0) + collectableResources.getOrDefault(Tardigrades.class, 0);

        if (collectableResources.containsKey(GhgProductionBacteria.class) && !game.getPlanetAtTheStartOfThePhase().isTemperatureMax()) {
            oneThirdPointResources += collectableResources.getOrDefault(GhgProductionBacteria.class, 0);
        }

        if (collectableResources.containsKey(NitriteReductingBacteria.class) && !game.getPlanetAtTheStartOfThePhase().isOceansMax()) {
            oneThirdPointResources += collectableResources.getOrDefault(NitriteReductingBacteria.class, 0);
        }

        if (collectableResources.containsKey(RegolithEaters.class) && !game.getPlanetAtTheStartOfThePhase().isOxygenMax()) {
            oneThirdPointResources += collectableResources.getOrDefault(RegolithEaters.class, 0);
        }

        return halfPointResources * 0.5f + onePointAnimals + oneThirdPointResources * 0.33f + cards.stream()
                .filter(card -> card.getCardMetadata().getWinPointsInfo() != null)
                .filter(card -> !CARDS_TO_EXCLUDE_FROM_FLOAT_WIN_POINTS_CHECK.contains(card.getClass()))
                .mapToInt(
                        card -> {
                            WinPointsInfo winPointsInfo = card.getCardMetadata().getWinPointsInfo();

                            int resources = (winPointsInfo.getType() == card.getCollectableResource())
                                    ? player.getCardResourcesCount().get(card.getClass())
                                    : 0;

                            if (winPointsInfo.getType() == CardCollectableResource.FOREST) {
                                resources = player.getForests();
                            } else if (winPointsInfo.getType() == CardCollectableResource.JUPITER) {
                                resources = cardService.countPlayedTags(player, Set.of(Tag.JUPITER));
                            } else if (winPointsInfo.getType() == CardCollectableResource.BLUE_CARD) {
                                resources = cardService.countPlayedCards(player, Set.of(CardColor.BLUE));
                            } else if (winPointsInfo.getType() == CardCollectableResource.ANY_CARD) {
                                resources = cardService.countPlayedCards(player, Set.of(CardColor.BLUE, CardColor.GREEN, CardColor.RED));
                            } else if (winPointsInfo.getType() == CardCollectableResource.EARTH) {
                                resources = cardService.countPlayedTags(player, Set.of(Tag.EARTH));
                            }

                            return getWinPoints(resources, winPointsInfo.getPoints(), winPointsInfo.getResources());
                        }
                ).sum();
    }

    private int getWinPoints(int resources, int pointsRatio, int resourcesRatio) {
        if (pointsRatio >= resourcesRatio) {
            return resources * pointsRatio / resourcesRatio;
        } else {
            return pointsRatio * (resources / resourcesRatio);
        }
    }
}
