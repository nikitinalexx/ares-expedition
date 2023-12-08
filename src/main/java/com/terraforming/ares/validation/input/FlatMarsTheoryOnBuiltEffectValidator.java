package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.red.FlatMarsTheory;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class FlatMarsTheoryOnBuiltEffectValidator implements OnBuiltEffectValidator<FlatMarsTheory> {
    private final CardService cardService;

    @Override
    public Class<FlatMarsTheory> getType() {
        return FlatMarsTheory.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        int scienceTagsPlayed = cardService.countPlayedTags(player, Set.of(Tag.SCIENCE));
        if (scienceTagsPlayed > 1) {
            return "This project requires maximum 1 science played so far";
        }

        return null;
    }
}
