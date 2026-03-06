package com.terraforming.ares.services.policyai.rollout;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.policyai.GameArenaContext;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.encoder.PolicyTableAndHandEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
            Card playedCard) {

        List<TagEffect> active = effects.stream()
                .filter(TagEffect::isPresent)
                .toList();

        if (active.isEmpty()) return;

        PolicyRecord current = null;

        for (int ei = 0; ei < active.size(); ei++) {
            TagEffect effect = active.get(ei);

            EffectStateReader stateReader = current != null
                    ? new RecordStateReader(current)
                    : new PlayerStateReader(player);

            List<EffectDecision> decisions = effect.resolveDecisions(stateReader);

            if (current == null) {//TODO optimize if last
                current = new PolicyRecord();
                if (playedCard != null) {
                    current.selectCard(playedCard);
                }
                encoder.encode(game, player, anotherPlayer, current);
            }

            if (decisions.isEmpty()) {//TODO optimize if last
                effect.applySideEffect(current);
                continue;
            }

            effect.prepareContext(current);

            boolean isLastEffect = ei == active.size() - 1;

            for (int di = 0; di < decisions.size(); di++) {
                EffectDecision decision = decisions.get(di);
                boolean lastInEffect = di == decisions.size() - 1;
                boolean lastOverall = lastInEffect && isLastEffect;

                current.setAction(decision.getChosenAction());
                GameArenaContext.current().add(current);

                if (lastOverall) return;

                PolicyRecord next = new PolicyRecord();
                next.copyFrom(current);
                decision.applyRollout(next);

                if (lastInEffect) {
                    effect.clearContext(next);
                }

                current = next;
            }
        }
    }
}