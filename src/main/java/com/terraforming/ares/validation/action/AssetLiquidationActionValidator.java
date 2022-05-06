package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.ArtificialJungle;
import com.terraforming.ares.cards.blue.AssetLiquidation;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.PlayerContext;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Component
public class AssetLiquidationActionValidator implements ActionValidator<AssetLiquidation> {
    @Override
    public Class<AssetLiquidation> getType() {
        return AssetLiquidation.class;
    }

    @Override
    public String validate(Planet planet, PlayerContext player) {
        if (player.getTerraformingRating() < 1) {
            return "Not enough TR to get cards";
        }

        return null;
    }

}
