package com.terraforming.ares.services.policyai.service;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.dl4j.PolicyNNService;
import com.terraforming.ares.services.ai.dl4j.PolicyPrediction;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.action.HeadAction;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.encoder.PolicyRecordFeatureConverter;
import com.terraforming.ares.services.policyai.encoder.PolicyTableAndHandEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PolicyDecisionService {
    private final PolicyCorporationDecisionService policyCorporationDecisionService;
    private final PolicyCardsDecisionService policyCardsDecisionService;
    private final PolicyTableAndHandEncoder encoder;
    private final PolicyNNService policyNNService;
    private final PolicyRecordFeatureConverter policyRecordFeatureConverter;
    private final PolicyActionSelector actionSelector;


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

        PolicyPrediction prediction = policyNNService.predictBatch(List.of(policyRecordFeatureConverter.convert(record)), PolicyNNService.ModelType.FIRST).getFirst();

        HeadAction chosen = actionSelector.chooseBestAction(prediction, validActions);


        return actionToPhase.get(chosen);
    }

}
