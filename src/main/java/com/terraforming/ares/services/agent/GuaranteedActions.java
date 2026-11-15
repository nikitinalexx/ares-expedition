package com.terraforming.ares.services.agent;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One rule, and it is arithmetic rather than strategy: <b>if a blue action costs nothing and leaves the position weakly
 * better</b> (no credit, heat, plant, steel or titanium spent, no card given up, and at least one card or resource gained),
 * take it. The owner plays this way in his own agent ("a guaranteed-value blue action I press without asking the network"),
 * and the sanity bench shows why: our policy, trained on its own games, had never seen such an action and refused a free
 * two-card draw, while letting the value search decide the whole blue menu costs 13 points of win rate (36.5% -> 23.5%
 * against SMART, 2000 games).
 * <p>
 * The rule fires at most once per phase - only before the phase action is used. The extra activation that a phase upgrade
 * grants is exactly the decision that needs thought, so it is never taken by the rule.
 */
@Service
@RequiredArgsConstructor
public class GuaranteedActions {
    private final BlueActionDecisions blueActionDecisions;
    private final BlueInputDecisions blueInputDecisions;
    private final AiTurnService aiTurnService;
    private final CardService cardService;

    /** The card to press without asking anybody, or empty. */
    public Optional<Integer> freeBlueAction(MarsGame game, Player player) {
        if (!player.getActivatedBlueCards().isEmpty()) {
            // the phase action is already used: what is left is the extra activation, which is a real decision
            return Optional.empty();
        }
        List<Candidate> candidates = blueActionDecisions.build(game, player).candidates();
        for (Candidate candidate : candidates) {
            if (candidate.id() != BlueActionDecisions.STOP && isFreeGain(game, player, candidate.id())) {
                return Optional.of(candidate.id());
            }
        }
        return Optional.empty();
    }

    /** Applies the action on a copy and compares what it cost with what it gained. */
    private boolean isFreeGain(MarsGame game, Player player, int cardId) {
        try {
            MarsGame copy = new MarsGame(game);
            Player me = copy.getPlayerByUuid(player.getUuid());
            Card card = cardService.getCard(cardId);
            Map<Integer, List<Integer>> input = blueInputDecisions.fill(copy, me, card, decision -> 0);
            int cards = me.getHand().size();
            int mc = me.getMc();
            int heat = me.getHeat();
            int plants = me.getPlants();
            aiTurnService.performBlueAction(copy, me, cardId, input);
            boolean spent = me.getMc() < mc || me.getHeat() < heat || me.getPlants() < plants;
            boolean gaveUpCard = me.getHand().size() < cards;
            boolean gained = me.getHand().size() > cards || me.getMc() > mc || me.getHeat() > heat
                    || me.getPlants() > plants;
            return !spent && !gaveUpCard && gained;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
