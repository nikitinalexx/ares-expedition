package com.terraforming.ares.services.policyai.service;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.dl4j.PolicyPrediction;
import com.terraforming.ares.services.ai.dto.MctsDecision;
import com.terraforming.ares.services.policyai.GameArenaContext;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.action.HeadAction;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.encoder.IncomeCategory;
import com.terraforming.ares.services.policyai.encoder.PolicyTableAndHandEncoder;
import com.terraforming.ares.services.policyai.encoder.PolicyTableGreenCardsFeature;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PolicyDecisionService {

    private final PolicyCorporationDecisionService policyCorporationDecisionService;
    private final PolicyCardsDecisionService policyCardsDecisionService;
    private final PolicyTableAndHandEncoder encoder;
    private final PolicyActionSelector actionSelector;

    private PolicyMctsService policyMctsService;

    @Lazy
    @Autowired
    public void setPolicyMctsService(PolicyMctsService policyMctsService) {
        this.policyMctsService = policyMctsService;
    }

    public List<Integer> mulliganCards(MarsGame game, Player currentPlayer) {
        return policyCardsDecisionService.mulliganCards(game, currentPlayer);
    }

    public int pickCorporation(MarsGame game, Player player) {
        return policyCorporationDecisionService.pickCorporation(game, player);
    }

    public int sultiraCorporationPhaseChoice(
            MarsGame game,
            Player player,
            Card corporation
    ) {
        return policyCorporationDecisionService.sultiraCorporationPhaseChoice(game, player, corporation);
    }

    public int apolloCorporationPhaseChoice(
            MarsGame game,
            Player player,
            Card corporation
    ) {
        return policyCorporationDecisionService.apolloCorporationPhaseChoice(game, player, corporation);
    }

    public int hyperionCorporationPhaseChoice(
            MarsGame game,
            Player player,
            Card corporation
    ) {
        return policyCorporationDecisionService.hyperionCorporationPhaseChoice(game, player, corporation);
    }

    public int exocorpCorporationPhaseChoice(
            MarsGame game,
            Player player,
            Card corporation
    ) {
        return policyCorporationDecisionService.exocorpCorporationPhaseChoice(game, player, corporation);
    }

    public int nebulabsCorporationPhaseChoice(
            MarsGame game,
            Player player,
            Card corporation
    ) {
        return policyCorporationDecisionService.nebulabsCorporationPhaseChoice(game, player, corporation);
    }

    public int modproCorporationPhaseChoice(
            MarsGame game,
            Player player,
            Card corporation
    ) {
        return policyCorporationDecisionService.modproCorporationPhaseChoice(game, player, corporation);
    }

    public Map<Integer, List<Integer>> austellarTagAndMilestoneChoice(MarsGame game, Player player, Card austellarCorporation) {
        return policyCorporationDecisionService.austellarTagAndMilestoneChoice(game, player, austellarCorporation);
    }

    public List<Integer> discardingFromHandPostBuild(MarsGame game, Player player, int count) {
        if (!AiConstants.POLICY_PLAYING) {
            return List.of();
        }

        return policyCardsDecisionService.discardingCardsFromHand(game, player, count, true);
    }

    public List<Integer> sellCardsAfterGenerationEnd(MarsGame game, Player player, int count) {
        if (!AiConstants.POLICY_PLAYING) {
            return List.of();
        }

        return policyCardsDecisionService.discardingCardsFromHand(game, player, count, false);
    }

    public List<Integer> discardingFromSelected(
            MarsGame game,
            Player player,
            List<Integer> discardingFrom,
            int cardsToDiscardCount
    ) {
        return policyCardsDecisionService.discardingFromSelected(game, player, discardingFrom, cardsToDiscardCount);
    }

    public int choosePhase(MarsGame game, Player player) {
        if (!AiConstants.POLICY_PLAYING) {
            return 0;
        }

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        PolicyRecord record = new PolicyRecord();
        encoder.encode(game, player, anotherPlayer, record);
        record.choosingPhase = true;

        List<HeadAction> validActions = new ArrayList<>();
        List<Integer> allPhases = new ArrayList<>(AiConstants.ALL_PHASES);
        if (player.getPreviousChosenPhase() != null) {
            allPhases.remove(player.getPreviousChosenPhase());
        }

        Map<HeadAction, Integer> actionToPhase = new HashMap<>();
        for (Integer phaseId : allPhases) {
            HeadAction action = ActionInputService.choosePhaseTurn(phaseId);
            validActions.add(action);
            actionToPhase.put(action, phaseId);
        }

        if (GameArenaContext.isSimulation()) {
            HeadAction chosen = actionSelector.chooseBestPhaseAction(record, validActions);
            return actionToPhase.get(chosen);
        }

        PolicyPrediction policyPrediction = actionSelector.predict(record);
        Map<Integer, Float> policyProbs = new HashMap<>();
        actionToPhase.forEach((action, phaseId) -> {
            policyProbs.put(phaseId, policyPrediction.getActionProbability(action.head(), action.localIndex()));
        });


        MctsDecision mctsDecision = policyMctsService.runMctsPhaseSelection(policyProbs, game, player.getUuid(), anotherPlayer.getUuid());
        int chosenPhase = mctsDecision.chosenPhase();
        if (GameArenaContext.isArenaAvailable()) {
            HeadAction bestAction = ActionInputService.choosePhaseTurn(chosenPhase);

            record.setAction(bestAction);
            record.setLegalActionsMask(validActions, record.chosenHead);
            record.setLegalHeadMask(validActions);
            if (AiConstants.POLICY_AVAILABLE) {
                record.policyDelta = PolicyLogitDeltaHelper.calculateLogitAdvantage(mctsDecision.winnerAvgQ(), policyPrediction.outcome);
                record.finalizedDelta = true;
                record.isBackwardPropagationStop = true;

                short[] localIndices = new short[validActions.size()];
                float[] probs =  new float[validActions.size()];

                for (int i = 0; i < validActions.size(); i++) {
                    HeadAction action = validActions.get(i);
                    localIndices[i] = (short) action.localIndex();
                    probs[i] = mctsDecision.policyTarget().get(actionToPhase.get(action));
                }

                record.setDistribution(localIndices, probs);
            }

            GameArenaContext.current().add(record);
        }
        return chosenPhase;
    }

    public Integer chooseCardForDoubleIncome(MarsGame game, Player player, List<Card> greenCards) {
        if (!AiConstants.POLICY_PLAYING) {
            return 0;
        }

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        PolicyRecord record = new PolicyRecord();
        encoder.encode(game, player, anotherPlayer, record);

        Map<HeadAction, IncomeCategory> actionToCategory = new HashMap<>();
        Map<HeadAction, Integer> actionToGreenCard = new HashMap<>();

        if (record.maxMcIncome[0] > 0) {
            actionToCategory.put(ActionInputService.specialPhasePaymentByIncomeType(IncomeCategory.MC), IncomeCategory.MC);
        }
        if (record.maxHeatIncome[0] > 0) {
            actionToCategory.put(ActionInputService.specialPhasePaymentByIncomeType(IncomeCategory.HEAT), IncomeCategory.HEAT);
        }
        if (record.maxPlantsIncome[0] > 0) {
            actionToCategory.put(ActionInputService.specialPhasePaymentByIncomeType(IncomeCategory.PLANT), IncomeCategory.PLANT);
        }
        if (record.maxCardsIncome[0] >= 1.0f) {
            actionToCategory.put(ActionInputService.specialPhasePaymentByIncomeType(IncomeCategory.CARD), IncomeCategory.CARD);
        }
        for (Card greenCard : greenCards) {
            Integer specialGreenCardIndex = AiConstants.GREEN_CARDS_INDEX_BY_CLASS.get(greenCard.getClass());
            if (specialGreenCardIndex != null) {
                actionToGreenCard.put(ActionInputService.specialGreenCardThatPays(specialGreenCardIndex), greenCard.getId());
            }
        }

        if (actionToCategory.isEmpty() && actionToGreenCard.isEmpty()) {
            return null;
        }

        List<HeadAction> validActions = new ArrayList<>(actionToCategory.keySet());
        validActions.addAll(actionToGreenCard.keySet());

        HeadAction chosen = actionSelector.chooseBestAction(record, validActions, null);

        if (actionToCategory.containsKey(chosen)) {
            Card bestCardByIncomeCategory = PolicyTableGreenCardsFeature.findBestCardByIncomeCategory(greenCards, actionToCategory.get(chosen), record);
            if (bestCardByIncomeCategory == null) {
                throw new IllegalStateException("No Card found for " + actionToCategory.get(chosen));
            }
            return bestCardByIncomeCategory.getId();
        } else {
            return actionToGreenCard.get(chosen);
        }
    }


}
