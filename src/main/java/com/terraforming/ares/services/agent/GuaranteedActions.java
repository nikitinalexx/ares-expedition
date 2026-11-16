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

    /**
     * The phase-4 double production is a guaranteed gain: with the double-produce upgrade bought and at least one played
     * green card that can pay again, collecting income once more costs nothing and pays something. Stopping there is never
     * weakly better, so the agent does not ask the network (owner, 2026-11-16: "not taking the double payment when you have
     * the bonus is nonsense - it is the same guaranteed value").
     * <p>
     * Among the eligible cards the rule takes the one whose payment is largest, comparing within a fixed order of
     * dimensions (money, plants, heat, cards, TR, then production). All of them are at least as good as stopping; the order
     * only decides which bonus to take when several are available.
     */
    public Optional<Integer> doubleIncome(MarsGame game, Player player) {
        Integer phase = player.getChosenPhase();
        if (phase == null || phase != 4) {
            return Optional.empty();
        }
        if (!player.hasPhaseUpgrade(com.terraforming.ares.model.Constants.PHASE_4_UPGRADE_DOUBLE_PRODUCE)) {
            return Optional.empty();
        }
        int bestCard = -1;
        long bestScore = 0;
        for (Integer id : player.getPlayed().getCards()) {
            Card card = cardService.getCard(id);
            if (card == null || card.getColor() != com.terraforming.ares.model.CardColor.GREEN
                    || !card.canPayAgain()) {
                continue;
            }
            long score = paymentScore(game, player, card);
            if (score > bestScore) {
                bestScore = score;
                bestCard = id;
            }
        }
        return bestCard < 0 ? Optional.empty() : Optional.of(bestCard);
    }

    /**
     * What this card would pay if collected again, in a comparable unit: money and plants and heat first (they convert to
     * points directly), cards next, then income. A card that cannot pay in this position scores 0.
     */
    private long paymentScore(MarsGame game, Player player, Card card) {
        try {
            MarsGame copy = new MarsGame(game);
            Player probe = copy.getPlayerByUuid(player.getUuid());
            card.payAgain(copy, cardService, probe);
            long mc = probe.getMc() - player.getMc();
            long plants = probe.getPlants() - player.getPlants();
            long heat = probe.getHeat() - player.getHeat();
            long cards = probe.getHand().size() - player.getHand().size();
            long tr = probe.getTerraformingRating() - player.getTerraformingRating();
            long production = (probe.getMcIncome() - player.getMcIncome())
                    + (probe.getPlantsIncome() - player.getPlantsIncome())
                    + (probe.getHeatIncome() - player.getHeatIncome())
                    + (probe.getCardIncome() - player.getCardIncome());
            return 1000 * mc + 1000 * plants + 1000 * heat + 300 * cards + 1000 * tr + 200 * production;
        } catch (RuntimeException e) {
            return 0;
        }
    }

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
