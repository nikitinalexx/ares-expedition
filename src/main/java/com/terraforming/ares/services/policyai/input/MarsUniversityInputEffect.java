package com.terraforming.ares.services.policyai.input;

import com.terraforming.ares.cards.blue.Decomposers;
import com.terraforming.ares.cards.blue.MarsUniversity;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.action.ActionRegistry;
import com.terraforming.ares.services.policyai.action.HeadAction;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.rollout.EffectStateReader;
import com.terraforming.ares.services.policyai.service.AbstractPolicyProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MarsUniversityInputEffect extends AbstractPolicyProcessor implements InputGeneratorEffect {
    private final CardService cardService;

    @Override
    public boolean isPresent(PolicyRecord record, TableContext tableContext, Card card, Map<Integer, List<Integer>> input) {
        return (record.hasPlayedBlueCard(MarsUniversity.class) || card.getClass() == MarsUniversity.class)
                && countScienceTags(card, input) > 0;
    }

    @Override
    public void generateAndApply(PolicyRecord record, TableContext tableContext, Map<Integer, List<Integer>> input, Card card) {
        int max = countScienceTags(card, input);
        List<Card> handCards = new ArrayList<>();
        for (Integer handCardId : tableContext.getPlayer().getHand().getCards()) {
            handCards.add(cardService.getCard(handCardId));
        }

        if (handCards.isEmpty()) {
            input.put(InputFlag.MARS_UNIVERSITY_CARD.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
            return;
        }

        List<Integer> discarded = new ArrayList<>();
        record.marsUniversityDiscardsLeft = (byte) max;

        for (int i = 0; i < max; i++) {
            if (handCards.isEmpty()) break;

            List<HeadAction> validActions = new ArrayList<>();
            validActions.add(ActionInputService.passAction());

            Map<HeadAction, Card> actionToCard = new HashMap<>();
            // добавляем действия для каждой карты в руке
            for (Card handCard : handCards) {
                HeadAction action = ActionInputService.sellDiscardCardAction(handCard);
                validActions.add(action);
                actionToCard.put(action, handCard);
            }

            HeadAction best = predict(record, validActions);
            record.marsUniversityDiscardsLeft--;

            if (best == ActionInputService.passAction()) {
                break;
            }

            Card cardToDiscard = actionToCard.get(best);
            discarded.add(cardToDiscard.getId());
            record.removeCardFromHand(cardToDiscard);
            record.addGenericCardToHand();
            handCards.remove(cardToDiscard);
        }

        record.marsUniversityDiscardsLeft = 0;

        if (discarded.isEmpty()) {
            input.put(InputFlag.MARS_UNIVERSITY_CARD.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
        } else {
            input.put(InputFlag.MARS_UNIVERSITY_CARD.getId(), discarded);
        }
    }

    private int countScienceTags(Card card, Map<Integer, List<Integer>> input) {
        long tagCount = card.getTags().stream()
                .filter(t -> t == Tag.SCIENCE)
                .count();
        List<Integer> tagInput = input.getOrDefault(InputFlag.TAG_INPUT.getId(), List.of());
        if (!tagInput.isEmpty()) {
            Tag t = Tag.values()[tagInput.getFirst()];
            if (t == Tag.SCIENCE) {
                tagCount++;
            }
        }
        return (int) tagCount;
    }

}












