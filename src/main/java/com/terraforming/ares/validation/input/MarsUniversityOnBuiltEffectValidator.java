package com.terraforming.ares.validation.input;

import com.terraforming.ares.cards.blue.MarsUniversity;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@Component
@RequiredArgsConstructor
public class MarsUniversityOnBuiltEffectValidator implements OnBuiltEffectValidator<MarsUniversity> {
    private final CardService cardService;

    @Override
    public Class<MarsUniversity> getType() {
        return MarsUniversity.class;
    }

    @Override
    public String validate(Card card, Player player, Map<Integer, List<Integer>> input) {
        long scienceTagsCount = cardService.countCardTags(card, Set.of(Tag.SCIENCE), input);

        if (scienceTagsCount == 0) {
            return null;
        }

        List<Integer> cardsInput = input.get(InputFlag.MARS_UNIVERSITY_CARD.getId());

        if (CollectionUtils.isEmpty(cardsInput)) {
            return "You need to provide input for the Mars University for the science tags";
        }

        if (cardsInput.contains(InputFlag.SKIP_ACTION.getId())) {
            return null;
        }

        if (cardsInput.size() > scienceTagsCount) {
            return "You can't discard more cards than the number of Science tags";
        }

        for (Integer cardIdToDiscard : cardsInput) {
            if (!player.getHand().containsCard(cardIdToDiscard)) {
                return "Can't discard the card that you don't have " + cardIdToDiscard;
            }
            if (cardIdToDiscard == card.getId()) {
                return "Can't discard the card that you are playing right now";
            }
        }

        return null;
    }

}
