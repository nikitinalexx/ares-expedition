package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.green.MartianLumber;
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
public class MartianLumberOnBuiltEffectValidator implements OnBuiltEffectValidator<MartianLumber> {

    @Override
    public Class<MartianLumber> getType() {
        return MartianLumber.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        if (player.getForests() < 2) {
            return "Not enough Forests to build the project";
        }

        return null;
    }
}
