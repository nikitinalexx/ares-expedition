package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.blue.Decomposers;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.terraforming.ares.model.InputFlag.DECOMPOSERS_TAKE_CARD;
import static com.terraforming.ares.model.InputFlag.DECOMPOSERS_TAKE_MICROBE;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class DecomposersOnBuiltEffectValidator implements OnBuiltEffectValidator<Decomposers> {
    private final CardService cardService;

    @Override
    public Class<Decomposers> getType() {
        return Decomposers.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        long tagsCount = cardService.countCardTags(card, Set.of(Tag.ANIMAL, Tag.MICROBE, Tag.PLANT), input);

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
