package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.BuffedExtremeColdFungus;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 06.12.2023
 */
@Component
@RequiredArgsConstructor
public class BuffedExtremeColdFungusActionValidator implements ActionValidator<BuffedExtremeColdFungus> {
    private final ExtremeColdFungusActionValidator validator;

    @Override
    public Class<BuffedExtremeColdFungus> getType() {
        return BuffedExtremeColdFungus.class;
    }

    @Override
    public String validate(MarsGame game, Player player, Map<Integer, List<Integer>> inputParameters) {
        return validator.validate(game, player, inputParameters);
    }

}
