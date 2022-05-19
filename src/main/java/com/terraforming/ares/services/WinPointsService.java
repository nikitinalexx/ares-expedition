package com.terraforming.ares.services;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.winPoints.WinPointsInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 17.05.2022
 */
@Service
@RequiredArgsConstructor
public class WinPointsService {
    private final CardService cardService;

    public int countWinPoints(Player player) {
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

        return winPoints;
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
                                resources = cardService.countPlayedTags(player, Tag.JUPITER);
                            }

                            if (winPointsInfo.getPoints() >= winPointsInfo.getResources()) {
                                return resources * winPointsInfo.getPoints() / winPointsInfo.getResources();
                            } else {
                                return winPointsInfo.getPoints() * (resources / winPointsInfo.getResources());
                            }
                        }
                ).sum();
    }
}
