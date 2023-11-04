package com.terraforming.ares.services;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.winPoints.WinPointsInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 17.05.2022
 */
@Service
@RequiredArgsConstructor
public class WinPointsService {
    private final CardService cardService;


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

    private int getWinPoints(int resources, int pointsRatio, int resourcesRatio) {
        if (pointsRatio >= resourcesRatio) {
            return resources * pointsRatio / resourcesRatio;
        } else {
            return pointsRatio * (resources / resourcesRatio);
        }
    }
}
