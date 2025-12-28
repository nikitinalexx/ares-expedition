package com.terraforming.ares.services;

import com.terraforming.ares.model.*;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 27.02.2023
 */
@Service
public class BuildService {

    public void addExtraActionFromSecondPhase(Player player) {
        if (player.getBuilds().stream().anyMatch(build -> build.getType() == BuildType.BLUE_RED)) {
            player.getBuilds().add(new BuildDto(BuildType.BLUE_RED));
        } else if (player.getBuilds().stream().anyMatch(build -> build.getType() == BuildType.BLUE_RED_OR_CARD)) {
            player.getBuilds().add(new BuildDto(BuildType.BLUE_RED_OR_CARD));
        } else if (player.getBuilds().stream().anyMatch(build -> build.getType() == BuildType.BLUE_RED_OR_MC)) {
            player.getBuilds().add(new BuildDto(BuildType.BLUE_RED_OR_MC));
        } else if (player.getBuilds().stream().anyMatch(build -> build.getType() == BuildType.GREEN_OR_BLUE)) {
            player.getBuilds().add(new BuildDto(BuildType.BLUE_RED));
        }
    }

    public BuildDto findMostOptimalBuild(Card card, Player player, int discountAlreadyApplied) {
        BuildDto optimalBuild = null;
        int bestRealDiscount = -1;

        int remainingPrice = Math.max(0, card.getPrice() - discountAlreadyApplied);

        for (BuildDto build : player.getBuilds()) {

            // Ограничение по цене карты
            if (build.getPriceLimit() > 0 && build.getPriceLimit() < card.getPrice()) {
                continue;
            }

            // Проверка типа билда
            boolean typeMatches =
                    build.getType() == BuildType.GREEN_OR_BLUE
                            || (card.getColor() == CardColor.GREEN && build.getType().isGreen())
                            || (card.getColor() != CardColor.GREEN && (build.getType().isBlueRed()));

            if (!typeMatches) continue;

            int realDiscount = Math.min(remainingPrice, build.getExtraDiscount());

            if (optimalBuild == null
                    || realDiscount > bestRealDiscount
                    || (realDiscount == bestRealDiscount
                    && build.getPriceLimit() < optimalBuild.getPriceLimit())
                    || (realDiscount == bestRealDiscount
                    && build.getPriceLimit() == optimalBuild.getPriceLimit()
                    && build.getType() == BuildType.BLUE_RED)) {

                optimalBuild = build;
                bestRealDiscount = realDiscount;
            }
        }

        return optimalBuild;
    }



}
