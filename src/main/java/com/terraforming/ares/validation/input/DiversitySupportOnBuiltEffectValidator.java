package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.red.DiversitySupport;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class DiversitySupportOnBuiltEffectValidator implements OnBuiltEffectValidator<DiversitySupport> {
    private final CardService cardService;

    @Override
    public Class<DiversitySupport> getType() {
        return DiversitySupport.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        int uniquePlayedTags = cardService.countUniquePlayedTags(player);
        if (uniquePlayedTags < 9) {
            return "Not enough different Tags played to build this project";
        }

        return null;
    }

}
