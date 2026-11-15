package com.terraforming.ares.services.agent;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StandardProjectType;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.StandardProjectService;
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
    private final StandardProjectService standardProjectService;

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

    /**
     * Whether a standard project is a WORSE way to buy the same global parameter than a card already in hand
     * (owner's arithmetic: three oceans from Deimos Down cost ~26 credits, three standard oceans 45).
     * <p>
     * For every unit of the parameter the card gives (three oceans, one forest), the effective price per unit is
     * {@code (price - immediate credits the card pays back) / units}, and it must be strictly below the standard project
     * price for the project to be hidden. A card that needs on-built inputs or is not affordable now is ignored: it is not
     * a cheaper way to get the same thing in this position.
     */
    public boolean standardProjectIsDominated(MarsGame game, Player player, StandardActionChoice choice) {
        try {
            List<GainType> wanted = gainTypesOf(choice.kind());
            if (wanted.isEmpty() || choice.projectType() == null) {
                return false;
            }
            int projectPrice = standardProjectService.getProjectPrice(player, choice.projectType());
            for (Integer id : player.getHand().getCards()) {
                Card card = cardService.getCard(id);
                if (card == null || card.getCardMetadata() == null) {
                    continue;
                }
                int units = 0;
                int gain = 0;
                for (Gain bonus : card.getCardMetadata().getBonuses()) {
                    if (wanted.contains(bonus.getType())) {
                        units += Math.max(1, bonus.getValue());
                    }
                    if (bonus.getType() == GainType.MC) {
                        gain += bonus.getValue();
                    }
                }
                if (units == 0) {
                    continue;
                }
                for (Gain income : card.getCardMetadata().getIncomes()) {
                    if (income.getType() == GainType.MC) {
                        gain += income.getValue();
                    }
                }
                if (card.getCardMetadata().getCardAction() != null) {
                    continue; // a card that needs an on-built choice is not simply affordable
                }
                int net = card.getPrice() - gain;
                if (net <= 0) {
                    continue;
                }
                double perUnit = net / (double) units;
                if (perUnit < projectPrice) {
                    return true;
                }
            }
            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static List<GainType> gainTypesOf(int kind) {
        return switch (kind) {
            case StandardActionDecisions.SP_OCEAN -> List.of(GainType.OCEAN);
            case StandardActionDecisions.SP_FOREST -> List.of(GainType.FOREST, GainType.OXYGEN);
            case StandardActionDecisions.SP_TEMPERATURE -> List.of(GainType.TEMPERATURE);
            default -> List.of();
        };
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
