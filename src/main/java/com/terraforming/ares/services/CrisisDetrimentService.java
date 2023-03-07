package com.terraforming.ares.services;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.DetrimentToken;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.parameters.ParameterColor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 06.03.2023
 */
@Service
public class CrisisDetrimentService {

    public void updateDetrimentTokens(MarsGame game) {
        if (!game.isCrysis()) {
            return;
        }

        final Planet planet = game.getPlanet();

        Set<DetrimentToken> detrimentTokens = new HashSet<>();
        if (planet.getOxygenColor() == ParameterColor.Y) {
            detrimentTokens.add(DetrimentToken.OXYGEN_YELLOW);
        } else if (planet.getOxygenColor() == ParameterColor.R) {
            detrimentTokens.add(DetrimentToken.OXYGEN_RED);
        }

        if (planet.getTemperatureColor() == ParameterColor.Y) {
            detrimentTokens.add(DetrimentToken.TEMPERATURE_YELLOW);
        } else if (planet.getTemperatureColor() == ParameterColor.R) {
            detrimentTokens.add(DetrimentToken.TEMPERATURE_RED);
        }

        if (planet.isValidOcean(ParameterColor.Y)) {
            detrimentTokens.add(DetrimentToken.OCEAN_YELLOW);
        } else if (planet.isValidOcean(ParameterColor.R)) {
            detrimentTokens.add(DetrimentToken.OCEAN_RED);
        }

        game.getCrysisData().setDetrimentTokens(detrimentTokens);
    }

    public boolean canPlayFirstPhase(MarsGame game) {
        return !(game.isCrysis()
                && !CollectionUtils.isEmpty(game.getCrysisData().getDetrimentTokens())
                && game.getCrysisData().getDetrimentTokens().contains(DetrimentToken.OXYGEN_RED));
    }

    public boolean canGetFirstPhaseBonus(MarsGame game) {
        return !(game.isCrysis()
                && !CollectionUtils.isEmpty(game.getCrysisData().getDetrimentTokens())
                && game.getCrysisData().getDetrimentTokens().contains(DetrimentToken.OXYGEN_YELLOW));
    }

    public boolean detrimentForPriceIncrease1(MarsGame game) {
        return game.isCrysis()
                && !CollectionUtils.isEmpty(game.getCrysisData().getDetrimentTokens())
                && game.getCrysisData().getDetrimentTokens().contains(DetrimentToken.TEMPERATURE_YELLOW);
    }

    public boolean detrimentForPriceIncrease3(MarsGame game) {
        return game.isCrysis()
                && !CollectionUtils.isEmpty(game.getCrysisData().getDetrimentTokens())
                && game.getCrysisData().getDetrimentTokens().contains(DetrimentToken.TEMPERATURE_RED);
    }

    public boolean detrimentFromOceanIsPresent(MarsGame game) {
        return game.isCrysis()
                && !CollectionUtils.isEmpty(game.getCrysisData().getDetrimentTokens())
                && (game.getCrysisData().getDetrimentTokens().contains(DetrimentToken.OCEAN_YELLOW)
                || game.getCrysisData().getDetrimentTokens().contains(DetrimentToken.OCEAN_RED)
        );
    }

    public int getOceanDetrimentSize(MarsGame game) {
        if (!game.isCrysis() || CollectionUtils.isEmpty(game.getCrysisData().getDetrimentTokens())) {
            return 0;
        }
        final Set<DetrimentToken> detrimentTokens = game.getCrysisData().getDetrimentTokens();
        if (detrimentTokens.contains(DetrimentToken.OCEAN_YELLOW)) {
            return 1;
        } else if (detrimentTokens.contains(DetrimentToken.OCEAN_RED)) {
            return 2;
        } else {
            return 0;
        }
    }


}
