package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.BuffedHydroElectricEnergy;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class BuffedHydroElectricEnergyActionValidator implements ActionValidator<BuffedHydroElectricEnergy> {
    private final HydroElectricEnergyActionValidator validator;

    @Override
    public Class<BuffedHydroElectricEnergy> getType() {
        return BuffedHydroElectricEnergy.class;
    }

    @Override
    public String validate(MarsGame game, Player player) {
        return validator.validate(game, player);
    }
}
