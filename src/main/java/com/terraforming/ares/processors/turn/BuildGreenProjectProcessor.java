package com.terraforming.ares.processors.turn;

import com.terraforming.ares.model.turn.BuildGreenProjectTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.BuildService;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DiscountService;
import com.terraforming.ares.services.TerraformingService;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class BuildGreenProjectProcessor extends GenericBuildProjectProcessor<BuildGreenProjectTurn> {

    public BuildGreenProjectProcessor(CardService marsDeckService,
                                      TerraformingService terraformingService,
                                      BuildService buildService,
                                      DiscountService discountService) {
        super(marsDeckService, terraformingService, buildService, discountService);
    }

    @Override
    public TurnType getType() {
        return TurnType.BUILD_GREEN_PROJECT;
    }
}
