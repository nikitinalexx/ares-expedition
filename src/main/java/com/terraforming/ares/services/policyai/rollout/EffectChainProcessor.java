package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.policyai.GameArenaContext;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.encoder.PolicyTableAndHandEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EffectChainProcessor {

    private final PolicyTableAndHandEncoder encoder;

    public EffectChainProcessor(PolicyTableAndHandEncoder encoder) {
        this.encoder = encoder;
    }

    public void process(
            List<TagEffect> effects,
            MarsGame game,
            Player player,
            Player anotherPlayer,
            Card selectedCard) {

        List<TagEffect> active = effects.stream()
                .filter(TagEffect::isPresent)
                .toList();

        if (active.isEmpty()) return;

        PolicyRecord pendingRecord = null;
        boolean addedToArena = false;

        for (int ei = 0; ei < active.size(); ei++) {
            TagEffect effect = active.get(ei);

            EffectStateReader stateReader = pendingRecord != null
                    ? new RecordStateReader(pendingRecord)
                    : new PlayerStateReader(player);

            List<EffectDecision> decisions = effect.resolveDecisions(stateReader);

            // Encode один раз лениво при первом любом эффекте
            if (pendingRecord == null) {
                pendingRecord = new PolicyRecord();
                encoder.encode(game, player, anotherPlayer, pendingRecord);
                if (selectedCard != null) {
                    pendingRecord.selectCard(selectedCard);
                }
            }

            if (decisions.isEmpty()) {
                effect.applySideEffect(pendingRecord);
                continue;
            }

            // Есть решения — выставляем контекст этого эффекта
            effect.prepareContext(pendingRecord);

            // Если ещё не в арене — добавляем
            if (!addedToArena) {
                GameArenaContext.current().add(pendingRecord);
                addedToArena = true;
            }

            for (int di = 0; di < decisions.size(); di++) {
                EffectDecision decision = decisions.get(di);
                boolean lastDecisionInEffect = di == decisions.size() - 1;
                boolean lastEffectOverall = ei == active.size() - 1;
                // Есть ли ещё что-то после этого решения
                boolean hasMore = !lastDecisionInEffect || !lastEffectOverall;

                pendingRecord.chosenAction = decision.getChosenAction();

                if (hasMore) {
                    PolicyRecord next = new PolicyRecord();
                    next.copyFrom(pendingRecord);
                    decision.applyRollout(next);

                    // Если это последнее решение этого эффекта — сбрасываем его контекст
                    if (lastDecisionInEffect) {
                        effect.clearContext(next);
                    }

                    GameArenaContext.current().add(next);
                    pendingRecord = next;
                }
            }
        }

        // Если не было ни одного решения — в арену ничего не попало, всё верно
    }
}