package com.terraforming.ares.services.ai.turnProcessors.network2.buildParams;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.turnProcessors.network2.Network2DraftCardsProjectionService;
import com.terraforming.ares.services.ai.turnProcessors.network2.dto.CardWithChanceModifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiMarsUniversityInputHandler {
    private final Network2DraftCardsProjectionService network2DraftCardsProjectionService;

    public void updateMarsUniversityInput(MarsGame game, Player player, int cardId, Map<Integer, List<Integer>> params) {
        if (!params.containsKey(InputFlag.MARS_UNIVERSITY_DUMMY_INPUT.getId())) {
            return;
        }
        int count = params.get(InputFlag.MARS_UNIVERSITY_DUMMY_INPUT.getId()).getFirst();
        if (count == 0) {
            return;
        }
        List<Integer> toDiscard = network2DraftCardsProjectionService.cardsToDiscardByProjectedValue(game, player)
                .stream()
                .map(CardWithChanceModifier::getCard)
                .filter(c -> c.getId() != cardId)
                .limit(count)
                .map(Card::getId)
                .toList();

        params.put(InputFlag.MARS_UNIVERSITY_CARD.getId(), toDiscard.isEmpty() ? List.of(InputFlag.SKIP_ACTION.getId()) : toDiscard);
    }
}
