package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.blue.Decomposers;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.Tag;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.terraforming.ares.model.InputFlag.DECOMPOSERS_TAKE_CARD;
import static com.terraforming.ares.model.InputFlag.DECOMPOSERS_TAKE_MICROBE;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
public class DecomposersOnBuiltEffectValidator implements OnBuiltEffectValidator<Decomposers> {

    @Override
    public Class<Decomposers> getType() {
        return Decomposers.class;
    }

    @Override
    public String validate(ProjectCard card, Player player, Map<Integer, List<Integer>> input) {
        long tagsCount = card.getTags()
                .stream()
                .filter(tag -> tag == Tag.ANIMAL || tag == Tag.MICROBE || tag == Tag.PLANT)
                .count();

        if (tagsCount == 0) {
            return null;
        }

        if (!input.containsKey(DECOMPOSERS_TAKE_MICROBE.getId())
                && !input.containsKey(DECOMPOSERS_TAKE_CARD.getId())) {
            return "When you play Animal/Microbe/Plant tag and have a Decomposers project, " +
                    "you need to decide if you want to take a Microbe or spend a Microbe and get a card";
        }

        int takeMicrobes = input.getOrDefault(DECOMPOSERS_TAKE_MICROBE.getId(), List.of(0)).get(0);
        int takeCards = input.getOrDefault(DECOMPOSERS_TAKE_CARD.getId(), List.of(0)).get(0);

        if (takeMicrobes + takeCards != tagsCount) {
            return "Decomposers Take Microbe/Card sum is not equal to played card Animal/Microbe/Plant tag count";
        }

        int microbesTotal = player.getCardResourcesCount().getOrDefault(Decomposers.class, 0) + takeMicrobes;

        if (microbesTotal < takeCards) {
            return "You can't pick Cards more than Microbes you have";
        }

        return null;
    }

}
