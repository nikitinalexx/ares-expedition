package com.terraforming.ares.validation.action;

import com.terraforming.ares.cards.blue.SelfReplicatingBacteria;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class SelfReplicatingBacteriaActionValidator implements ActionValidator<SelfReplicatingBacteria> {

    @Override
    public Class<SelfReplicatingBacteria> getType() {
        return SelfReplicatingBacteria.class;
    }

    @Override
    public String validate(MarsGame game, Player player, List<Integer> inputParameters) {
        if (CollectionUtils.isEmpty(inputParameters)) {
            return "SelfReplicatingBacteria expects input to add or remove microbes";
        }

        Integer input = inputParameters.get(0);
        if (input != 1 && input != 5) {
            return "NitriteReductingBacteria invalid input, should be either 1 or 5";
        }

        if (input == 5 && player.getCardResourcesCount().get(SelfReplicatingBacteria.class) < 5) {
            return "Not enough microbes to perform Self Replicating Bacteria action";
        }

        return null;
    }
}
