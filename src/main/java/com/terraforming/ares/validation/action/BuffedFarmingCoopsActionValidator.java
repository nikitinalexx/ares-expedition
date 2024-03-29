package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.BuffedFarmingCoops;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class BuffedFarmingCoopsActionValidator implements ActionValidator<BuffedFarmingCoops> {
    private final FarmingCoopsActionValidator validator;

    @Override
    public Class<BuffedFarmingCoops> getType() {
        return BuffedFarmingCoops.class;
    }

    @Override
    public String validate(MarsGame game, Player player, Map<Integer, List<Integer>> inputParameters) {
        return validator.validate(game, player, inputParameters);
    }

}
