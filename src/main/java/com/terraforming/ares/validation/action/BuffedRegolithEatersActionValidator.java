package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.BuffedRegolithEaters;
import com.terraforming.ares.cards.blue.RegolithEaters;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class BuffedRegolithEatersActionValidator implements ActionValidator<BuffedRegolithEaters> {
    public static final String ERROR_MESSAGE = "RegolithEaters expects input to add or remove microbes";
    private final TerraformingService terraformingService;

    @Override
    public Class<BuffedRegolithEaters> getType() {
        return BuffedRegolithEaters.class;
    }

    @Override
    public String validate(MarsGame game, Player player, Map<Integer, List<Integer>> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters)) {
            return ERROR_MESSAGE;
        }

        final List<Integer> addDiscardInput = inputParameters.get(InputFlag.ADD_DISCARD_MICROBE.getId());
        if (CollectionUtils.isEmpty(addDiscardInput)) {
            return ERROR_MESSAGE;
        }

        Integer input = addDiscardInput.get(0);
        if (input != 1 && input != 2) {
            return "RegolithEaters invalid input, should be either 1 or 2";
        }

        if (input == 2 && player.getCardResourcesCount().get(BuffedRegolithEaters.class) < 2) {
            return "Not enough microbes to raise oxygen";
        }

        if (input == 2 && !terraformingService.canIncreaseOxygen(game)) {
            return "Can not increase Oxygen anymore";
        }

        return null;
    }
}
