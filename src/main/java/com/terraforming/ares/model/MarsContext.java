package com.terraforming.ares.model;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.TerraformingService;
import lombok.Builder;
import lombok.Value;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@Value
@Builder
public class MarsContext {
    MarsGame game;
    Player player;
    TerraformingService terraformingService;
    CardService cardService;
}
