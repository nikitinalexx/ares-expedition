package com.terraforming.ares.processors.turn;

import com.terraforming.ares.model.turn.BuildBlueRedProjectTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.*;
import com.terraforming.ares.services.ai.CardsCollectService;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class BuildBlueRedProjectProcessor extends GenericBuildProjectProcessor<BuildBlueRedProjectTurn> {

    public BuildBlueRedProjectProcessor(CardService marsDeckService,
                                        BuildService buildService,
                                        DiscountService discountService,
                                        MarsContextProvider marsContextProvider) {
        super(marsDeckService, buildService, discountService, marsContextProvider);
    }

    @Override
    public TurnType getType() {
        return TurnType.BUILD_BLUE_RED_PROJECT;
    }
}
