package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;

public class MarsUniversityEffect implements TagEffect {

    private final List<Integer> marsUniversity;
    private final Player player;
    private final CardService cardService;

    public MarsUniversityEffect(Map<Integer, List<Integer>> params, Player player, CardService cardService) {
        this.marsUniversity = params.get(InputFlag.MARS_UNIVERSITY_CARD.getId());
        this.player = player;
        this.cardService = cardService;
    }

    @Override
    public boolean isPresent() {
        return !CollectionUtils.isEmpty(marsUniversity);
    }

    @Override
    public void prepareContext(PolicyRecord record) {
        record.marsUniversityDiscardsLeft = (byte) maxDecisions();
    }

    @Override
    public void clearContext(PolicyRecord record) {
        record.marsUniversityDiscardsLeft = 0;
    }

    @Override
    public List<EffectDecision> resolveDecisions(EffectStateReader state) {
        boolean fullSkip = marsUniversity.getFirst() == InputFlag.SKIP_ACTION.getId();

        if (fullSkip) {
            return List.of(skipDecision());
        }

        // Карты, которые игрок выбрал сбросить
        return marsUniversity.stream()
                .map(this::discardDecision)
                .toList();
    }

    private EffectDecision skipDecision() {
        return new EffectDecision() {
            @Override
            public int getChosenAction() {
                return ActionInputService.passActionId();
            }

            @Override
            public void applyRollout(PolicyRecord r) {
                r.marsUniversityDiscardsLeft = 0;
                // скип — ничего не меняется в состоянии
            }
        };
    }

    private EffectDecision discardDecision(int cardId) {
        return new EffectDecision() {
            @Override
            public int getChosenAction() {
                return ActionInputService.sellDiscardCardActionIndex(cardService.getCard(cardId));
            }

            @Override
            public void applyRollout(PolicyRecord r) {
                r.marsUniversityDiscardsLeft--;
                r.addGenericCardToHand(); // за сброс берём новую карту
            }
        };
    }

    private int maxDecisions() {
        return player.getHand().size() - 1;
    }
}
