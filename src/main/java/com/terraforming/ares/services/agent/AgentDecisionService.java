package com.terraforming.ares.services.agent;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Entry point used by the AI turn processors of the AGENT difficulty.
 * A trained model is used for a decision type only when one is loaded for that type; every other decision is played by
 * {@link DecisionAgent} (uniform random, unless a test replaces it).
 */
@Service
@RequiredArgsConstructor
public class AgentDecisionService {
    private final PickPhaseDecisions pickPhaseDecisions;
    private final CorporationDecisions corporationDecisions;
    private final MulliganDecisions mulliganDecisions;
    private final HandCardDecisions handCardDecisions;
    private final StandardActionDecisions standardActionDecisions;
    private final BuildCardDecisions buildCardDecisions;
    private final BuildPaymentDecisions buildPaymentDecisions;
    private final BuildInputDecisions buildInputDecisions;
    private final BlueActionDecisions blueActionDecisions;
    private final BlueInputDecisions blueInputDecisions;
    private final UnmiRtDecisions unmiRtDecisions;
    private final CollectIncomeDecisions collectIncomeDecisions;
    private final DecisionAgent agent;
    private final NeuralPolicy neuralPolicy;
    private final DecisionLogs decisionLogs;
    private final AiTurnService aiTurnService;
    private final CardService cardService;

    public int pickPhase(MarsGame game, Player player) {
        Decision decision = pickPhaseDecisions.build(game, player);
        return decision.candidates().get(choose(game, player, decision)).id();
    }

    public CorporationChoice pickCorporation(MarsGame game, Player player) {
        List<CorporationChoice> choices = corporationDecisions.options(game, player);
        int index = choose(game, player, corporationDecisions.decision(game, player, choices));
        return choices.get(index);
    }

    /**
     * Cards to replace. Empty means the player keeps the opening hand. Each discarded card and the final stop are
     * separate recorded decisions, so the learned value of "discard this" does not depend on an arbitrary order being
     * the only way to name a set.
     */
    public List<Integer> mulligan(MarsGame game, Player player) {
        List<Integer> discard = new ArrayList<>();
        while (true) {
            Decision decision = mulliganDecisions.build(game, player, discard);
            int id = decision.candidates().get(choose(game, player, decision)).id();
            if (id == MulliganDecisions.STOP) {
                return discard;
            }
            discard.add(id);
        }
    }

    /**
     * Cards the engine's pending discard wants removed. A revealed set (draft, draw-and-keep) is recorded as
     * {@link DecisionType#DRAFT_CARDS}; a discard from the whole hand as {@link DecisionType#DISCARD_CARDS}.
     * An empty result is the engine asking for nothing, and it is not a decision.
     */
    public List<Integer> discard(MarsGame game, Player player) {
        DiscardCardsTurn turn = (DiscardCardsTurn) player.getNextTurn();
        List<Integer> pool = turn.isOnlyFromSelectedCards() ? turn.getCards() : player.getHand().getCards();
        DecisionType type = turn.isOnlyFromSelectedCards() ? DecisionType.DRAFT_CARDS : DecisionType.DISCARD_CARDS;
        List<Integer> chosen = new ArrayList<>();
        for (int i = 0; i < turn.getSize(); i++) {
            Decision decision = handCardDecisions.build(type, game, player, pool, chosen, false);
            chosen.add(decision.candidates().get(choose(game, player, decision)).id());
        }
        return chosen;
    }

    /**
     * Cards to sell for their sell price right now. Empty means the agent keeps the hand and takes the other action
     * (build, blue action, and so on). The sale itself does not spend that action.
     */
    public List<Integer> sell(MarsGame game, Player player) {
        return takeUntilStop(DecisionType.SELL_CARDS, game, player, player.getHand().getCards(), 0);
    }

    /**
     * End-of-round sale. The hand must end at {@link Constants#MAX_HAND_SIZE_LAST_ROUND} or below, and selling more
     * than that is legal.
     */
    public List<Integer> sellLastRound(MarsGame game, Player player) {
        int minimum = Math.max(0, player.getHand().getCards().size() - Constants.MAX_HAND_SIZE_LAST_ROUND);
        return takeUntilStop(DecisionType.SELL_CARDS_LAST_ROUND, game, player, player.getHand().getCards(), minimum);
    }

    /**
     * One spend from the phase-3 menu, or empty when the agent stops and leaves blues/builds/skip to the fallback.
     * Helion exchange is offered one heat at a time; the next call sees the new heat.
     */
    public Optional<StandardActionChoice> standardAction(MarsGame game, Player player, List<TurnType> possibleTurns) {
        List<StandardActionChoice> choices = standardActionDecisions.options(game, player, possibleTurns);
        int index = choose(game, player, standardActionDecisions.decision(game, player, choices));
        StandardActionChoice choice = choices.get(index);
        if (choice.kind() == StandardActionDecisions.STOP) {
            return Optional.empty();
        }
        return Optional.of(choice);
    }

    /**
     * Which hand card to build, or empty to stop. On-built inputs and payment are separate decisions filled by
     * {@link #playBuildCard}.
     */
    public Optional<Integer> buildCard(MarsGame game, Player player) {
        Decision decision = buildCardDecisions.build(game, player);
        int id = decision.candidates().get(choose(game, player, decision)).id();
        if (id == BuildCardDecisions.STOP) {
            return Optional.empty();
        }
        return Optional.of(id);
    }

    public void playBuildCard(MarsGame game, Player player, int cardId) {
        Card card = cardService.getCard(cardId);
        Map<Integer, List<Integer>> input = buildInputDecisions.fill(game, player, card,
                decision -> choose(game, player, decision));
        List<BuildPaymentChoice> payments = buildPaymentDecisions.options(game, player, card, input);
        int index = choose(game, player, buildPaymentDecisions.decision(game, player, card, payments));
        aiTurnService.buildProject(game, player, cardId, payments.get(index).payments(), input);
    }

    /**
     * Which blue action to activate, or empty to stop. Inputs are a separate {@link DecisionType#BLUE_INPUT} sequence.
     */
    public Optional<Integer> blueAction(MarsGame game, Player player) {
        Decision decision = blueActionDecisions.build(game, player);
        int id = decision.candidates().get(choose(game, player, decision)).id();
        if (id == BlueActionDecisions.STOP) {
            return Optional.empty();
        }
        return Optional.of(id);
    }

    public void playBlueAction(MarsGame game, Player player, int cardId) {
        Card card = cardService.getCard(cardId);
        Map<Integer, List<Integer>> input = blueInputDecisions.fill(game, player, card,
                decision -> choose(game, player, decision));
        aiTurnService.performBlueAction(game, player, cardId, input);
    }

    /**
     * Whether to take the UNMI +1 TR for 6 MC now. Empty means stop (skip / leave for a later visit this phase).
     * Not called when the action is unavailable.
     */
    public Optional<Boolean> unmiRt(MarsGame game, Player player, List<TurnType> possibleTurns) {
        if (!unmiRtDecisions.available(player, possibleTurns)) {
            return Optional.empty();
        }
        Decision decision = unmiRtDecisions.build(game, player);
        int id = decision.candidates().get(choose(game, player, decision)).id();
        if (id == UnmiRtDecisions.STOP) {
            return Optional.empty();
        }
        return Optional.of(Boolean.TRUE);
    }

    public void playUnmiRt(MarsGame game, Player player) {
        aiTurnService.unmiRtCorporationTurn(game, player);
    }

    /**
     * Card to double-collect under the phase-4 upgrade, or empty to collect income once. When the upgrade does not
     * apply or no green card can pay again, returns empty without recording a decision.
     */
    public Optional<Integer> collectIncome(MarsGame game, Player player) {
        if (!collectIncomeDecisions.needsChoice(player)) {
            return Optional.empty();
        }
        Decision decision = collectIncomeDecisions.build(game, player);
        int id = decision.candidates().get(choose(game, player, decision)).id();
        if (id == CollectIncomeDecisions.STOP) {
            return Optional.empty();
        }
        return Optional.of(id);
    }

    public void playCollectIncome(Player player, Integer doubleCardId) {
        aiTurnService.collectIncomeTurn(player, doubleCardId);
    }

    /**
     * After terraforming is complete: sell the hand (forced), buy remaining affordable standard projects one at a time
     * (no stop — the engine forbids confirming while a project is still affordable), then confirm. Returns true when a
     * turn was made.
     */
    public boolean endgame(MarsGame game, Player player, List<TurnType> possibleTurns) {
        if (!game.gameEndCondition()) {
            return false;
        }
        if (!player.getHand().isEmpty()) {
            Card corporation = cardService.getCard(player.getSelectedCorporationCard());
            if (corporation.getCardMetadata() != null
                    && corporation.getCardMetadata().getCardAction() == CardAction.HELION_CORPORATION
                    && player.getHeat() > 0) {
                player.setMc(player.getMc() + player.getHeat());
                player.setHeat(0);
            }
            aiTurnService.sellCards(player, game, new ArrayList<>(player.getHand().getCards()));
            return true;
        }
        List<StandardActionChoice> spends = standardActionDecisions.options(game, player, possibleTurns).stream()
                .filter(c -> c.kind() != StandardActionDecisions.STOP)
                .filter(c -> c.kind() == StandardActionDecisions.SP_OCEAN
                        || c.kind() == StandardActionDecisions.SP_FOREST
                        || c.kind() == StandardActionDecisions.SP_TEMPERATURE
                        || c.kind() == StandardActionDecisions.SP_INFRASTRUCTURE_PLANTS
                        || c.kind() == StandardActionDecisions.SP_INFRASTRUCTURE_HEAT)
                .toList();
        if (!spends.isEmpty()) {
            int index = choose(game, player, standardActionDecisions.decision(game, player, spends));
            playStandardAction(game, player, spends.get(index));
            return true;
        }
        aiTurnService.confirmGameEnd(game, player);
        return true;
    }

    /** Executes a non-stop choice from {@link #standardAction}. */
    public void playStandardAction(MarsGame game, Player player, StandardActionChoice choice) {
        switch (choice.kind()) {
            case StandardActionDecisions.EXCHANGE_ONE_HEAT -> aiTurnService.exchangeHeat(game, player, 1);
            case StandardActionDecisions.PLANT_FOREST -> aiTurnService.plantForest(game, player);
            case StandardActionDecisions.INCREASE_TEMPERATURE -> aiTurnService.increaseTemperature(game, player);
            case StandardActionDecisions.INCREASE_INFRASTRUCTURE_PLANTS,
                 StandardActionDecisions.INCREASE_INFRASTRUCTURE_HEAT ->
                    aiTurnService.increaseInfrastructure(player, game, choice.input());
            case StandardActionDecisions.SP_OCEAN, StandardActionDecisions.SP_FOREST,
                 StandardActionDecisions.SP_TEMPERATURE, StandardActionDecisions.SP_INFRASTRUCTURE_PLANTS,
                 StandardActionDecisions.SP_INFRASTRUCTURE_HEAT ->
                    aiTurnService.standardProjectTurn(game, player, choice.projectType(), choice.input());
            default -> throw new IllegalStateException("Cannot play standard action kind " + choice.kind());
        }
    }

    private List<Integer> takeUntilStop(DecisionType type, MarsGame game, Player player, List<Integer> pool, int minimum) {
        List<Integer> chosen = new ArrayList<>();
        while (true) {
            Decision decision = handCardDecisions.build(type, game, player, pool, chosen, chosen.size() >= minimum);
            int id = decision.candidates().get(choose(game, player, decision)).id();
            if (id == CardFeatures.STOP_ID) {
                return chosen;
            }
            chosen.add(id);
        }
    }

    private int choose(MarsGame game, Player player, Decision decision) {
        int index = neuralPolicy.choose(decision);
        if (index < 0) {
            index = agent.choose(decision);
        }
        if (index < 0 || index >= decision.candidates().size()) {
            throw new IllegalStateException("Agent chose " + index + " out of " + decision.candidates().size()
                    + " candidates of " + decision.type());
        }
        decisionLogs.record(game, player, decision, index);
        return index;
    }
}
