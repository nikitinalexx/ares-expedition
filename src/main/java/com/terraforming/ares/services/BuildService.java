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

    public BuildDto findMostOptimalBuild(Card card, Player player, int discount) {
        BuildDto optimalBuild = null;
        for (BuildDto build : player.getBuilds()) {
            if ((build.getPriceLimit() == 0 || build.getPriceLimit() >= card.getPrice())
                    && (build.getType() == BuildType.GREEN_OR_BLUE
                    || (card.getColor() == CardColor.GREEN && build.getType() == BuildType.GREEN)
                    || (card.getColor() != CardColor.GREEN && (build.getType() == BuildType.BLUE_RED
                    || build.getType() == BuildType.BLUE_RED_OR_CARD
                    || build.getType() == BuildType.BLUE_RED_OR_MC)))) {
                if (optimalBuild == null) {
                    optimalBuild = build;
                }
                final int buildRealDiscount = Math.min(card.getPrice() - Math.min(card.getPrice(), discount), build.getExtraDiscount());
                final int optimalBuildRealDiscount = Math.min(card.getPrice() - Math.min(card.getPrice(), discount), optimalBuild.getExtraDiscount());
                if ((buildRealDiscount > optimalBuildRealDiscount || build.getPriceLimit() < optimalBuild.getPriceLimit())
                        || (buildRealDiscount == optimalBuildRealDiscount && build.getPriceLimit() == optimalBuild.getPriceLimit()
                        && build.getType() == BuildType.BLUE_RED)) {
                    optimalBuild = build;
                }
            }
        }
        return optimalBuild;
    }


}
