package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.OrbitalCleanup;
import com.terraforming.ares.cards.red.Harvest;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class OrbitalCleanupOnBuiltEffectValidator implements OnBuiltEffectValidator<OrbitalCleanup> {

    @Override
    public Class<OrbitalCleanup> getType() {
        return OrbitalCleanup.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        if (player.getMcIncome() < 2) {
            return "Not enough MC income to build the project";
        }

        return null;
    }
}
