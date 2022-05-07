package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.blue.Decomposers;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.Tag;

import java.util.Map;

import static com.terraforming.ares.model.InputFlag.DECOMPOSERS_TAKE_CARD;
import static com.terraforming.ares.model.InputFlag.DECOMPOSERS_TAKE_MICROBE;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
public class DecomposersProjectInputValidator implements ProjectInputValidator {

    @Override
    public String validate(ProjectCard card, Player player, Map<Integer, Integer> input) {
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

        if (input.get(DECOMPOSERS_TAKE_MICROBE.getId()) + input.get(DECOMPOSERS_TAKE_CARD.getId()) != tagsCount) {
            return "Decomposers Take Microbe/Card sum is not equal to played card Animal/Microbe/Plant tag count";
        }

        int microbesTotal = player.getCardResourcesCount().get(Decomposers.class) + input.get(DECOMPOSERS_TAKE_MICROBE.getId());

        if (microbesTotal < input.get(DECOMPOSERS_TAKE_CARD.getId())) {
            return "You can't pick Cards more than Microbes you have";
        }

        return null;
    }

}
