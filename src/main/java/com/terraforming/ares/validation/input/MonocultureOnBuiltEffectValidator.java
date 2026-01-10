package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.Monocultures;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 30.12.2025
 */
@Component
public class MonocultureOnBuiltEffectValidator implements OnBuiltEffectValidator<Monocultures> {

    @Override
    public Class<Monocultures> getType() {
        return Monocultures.class;
    }

    @Override
    public String validate(MarsGame game, Card card, Player player, Map<Integer, List<Integer>> input) {
        if (player.getTerraformingRating() < 1) {
            return "Not enough terraforming rating";
        }

        return null;
    }
}
