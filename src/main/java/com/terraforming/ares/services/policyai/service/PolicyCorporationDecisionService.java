package com.terraforming.ares.services.policyai.service;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.milestones.MilestoneType;
import com.terraforming.ares.services.CardService;
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
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class PolicyCorporationDecisionService {
    private final PolicyNNService policyNNService;
    private final PolicyRecordFeatureConverter policyRecordFeatureConverter;
    private final PolicyTableAndHandEncoder encoder;
    private final CardService cardService;
    private final PolicyActionSelector actionSelector;

    public int pickCorporation(MarsGame game, Player player) {

        if (!AiConstants.POLICY_PLAYING) {
            return 0;
        }

        Map<HeadAction, Integer> actionToCard = new HashMap<>();

        List<HeadAction> legalActions = player.getCorporations().getCards()
                .stream()
                .map(cardId -> {
                    Card card = cardService.getCard(cardId);
                    HeadAction action = ActionInputService.chooseCorporationAction(card);
                    actionToCard.put(action, cardId);
                    return action;
                })
                .toList();

        HeadAction chosen = decide(
                game,
                player,
                record -> {
                    record.initialSetup = true;
                    record.pickingCorporation = true;
                },
                legalActions
        );

        return actionToCard.get(chosen);
    }

    public int sultiraCorporationPhaseChoice(
            MarsGame game,
            Player player,
            Card corporation
    ) {
        if (!AiConstants.POLICY_PLAYING) {
            return 0;
        }

        player.setMc(corporation.getPrice());
        player.setHeat(2);//sultira effect on itself
        player.getPlayed().getCards().add(corporation.getId());
        player.setSelectedCorporationCard(corporation.getId());

        List<HeadAction> legalActions = List.of(
                ActionInputService.choosePhaseUpgradeAction(Constants.PHASE_1_UPGRADE_DISCOUNT),
                ActionInputService.choosePhaseUpgradeAction(Constants.PHASE_1_UPGRADE_BUILD_EXTRA)
        );

        HeadAction chosen = decide(
                game,
                player,
                record -> {
                    record.initialSetup = true;
                    record.choosingPhase1Upgrade = true;
                },
                legalActions
        );

        player.setMc(0);
        player.setHeat(0);
        player.getPlayed().getCards().clear();
        player.setSelectedCorporationCard(null);

        return Constants.PHASE_1_UPGRADE_DISCOUNT + legalActions.indexOf(chosen);
    }

    public int apolloCorporationPhaseChoice(
            MarsGame game,
            Player player,
            Card corporation
    ) {
        if (!AiConstants.POLICY_PLAYING) {
            return 0;
        }

        player.setMc(corporation.getPrice());
        player.getPlayed().getCards().add(corporation.getId());
        player.setSelectedCorporationCard(corporation.getId());

        List<HeadAction> legalActions = List.of(
                ActionInputService.choosePhaseUpgradeAction(Constants.PHASE_2_UPGRADE_PROJECT_AND_MC),
                ActionInputService.choosePhaseUpgradeAction(Constants.PHASE_2_UPGRADE_PROJECT_AND_CARD)
        );

        HeadAction chosen = decide(
                game,
                player,
                record -> {
                    record.initialSetup = true;
                    record.choosingPhase2Upgrade = true;
                },
                legalActions
        );

        player.setMc(0);
        player.getPlayed().getCards().clear();
        player.setSelectedCorporationCard(null);

        return Constants.PHASE_2_UPGRADE_PROJECT_AND_MC + legalActions.indexOf(chosen);
    }

    public int hyperionCorporationPhaseChoice(
            MarsGame game,
            Player player,
            Card corporation
    ) {
        if (!AiConstants.POLICY_PLAYING) {
            return 0;
        }

        player.setMc(corporation.getPrice());
        player.getPlayed().getCards().add(corporation.getId());
        player.setSelectedCorporationCard(corporation.getId());

        List<HeadAction> legalActions = List.of(
                ActionInputService.choosePhaseUpgradeAction(Constants.PHASE_3_UPGRADE_DOUBLE_REPEAT),
                ActionInputService.choosePhaseUpgradeAction(Constants.PHASE_3_UPGRADE_REVEAL_CARDS)
        );

        HeadAction chosen = decide(
                game,
                player,
                record -> {
                    record.initialSetup = true;
                    record.choosingPhase3Upgrade = true;
                },
                legalActions
        );

        player.setMc(0);
        player.getPlayed().getCards().clear();
        player.setSelectedCorporationCard(null);

        return Constants.PHASE_3_UPGRADE_DOUBLE_REPEAT + legalActions.indexOf(chosen);
    }

    public int exocorpCorporationPhaseChoice(
            MarsGame game,
            Player player,
            Card corporation
    ) {
        if (!AiConstants.POLICY_PLAYING) {
            return 0;
        }

        player.setMc(corporation.getPrice());
        player.getPlayed().getCards().add(corporation.getId());
        player.setSelectedCorporationCard(corporation.getId());

        List<HeadAction> legalActions = List.of(
                ActionInputService.choosePhaseUpgradeAction(Constants.PHASE_5_UPGRADE_KEEP_EXTRA),
                ActionInputService.choosePhaseUpgradeAction(Constants.PHASE_5_UPGRADE_SEE_EXTRA)
        );

        HeadAction chosen = decide(
                game,
                player,
                record -> {
                    record.initialSetup = true;
                    record.choosingPhase5Upgrade = true;
                },
                legalActions
        );

        player.setMc(0);
        player.getPlayed().getCards().clear();
        player.setSelectedCorporationCard(null);

        return Constants.PHASE_5_UPGRADE_KEEP_EXTRA + legalActions.indexOf(chosen);
    }

    public int nebulabsCorporationPhaseChoice(
            MarsGame game,
            Player player,
            Card corporation
    ) {
        if (!AiConstants.POLICY_PLAYING) {
            return 0;
        }

        player.setMc(corporation.getPrice());
        player.getPlayed().getCards().add(corporation.getId());
        player.setSelectedCorporationCard(corporation.getId());

        List<HeadAction> legalActions = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            legalActions.add(ActionInputService.choosePhaseUpgradeAction(i));
        }

        HeadAction chosen = decide(
                game,
                player,
                record -> {
                    record.initialSetup = true;
                    record.universalPhaseUpgradeCount = 1;
                },
                legalActions
        );

        player.setMc(0);
        player.getPlayed().getCards().clear();
        player.setSelectedCorporationCard(null);

        return legalActions.indexOf(chosen);
    }

    public int modproCorporationPhaseChoice(
            MarsGame game,
            Player player,
            Card corporation
    ) {
        if (!AiConstants.POLICY_PLAYING) {
            return 0;
        }

        player.setMc(corporation.getPrice());
        player.getPlayed().getCards().add(corporation.getId());
        player.setSelectedCorporationCard(corporation.getId());

        List<HeadAction> legalActions = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            legalActions.add(ActionInputService.chooseTag(i));
        }

        HeadAction chosen = decide(
                game,
                player,
                record -> {
                    record.initialSetup = true;
                    record.choosingTag = true;
                },
                legalActions
        );

        player.setMc(0);
        player.getPlayed().getCards().clear();
        player.setSelectedCorporationCard(null);

        return legalActions.indexOf(chosen);
    }

    public Map<Integer, List<Integer>> austellarTagAndMilestoneChoice(MarsGame game, Player player, Card austellarCorporation) {
        if (!AiConstants.POLICY_PLAYING) {
            return Map.of();
        }
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        player.setMc(austellarCorporation.getPrice());
        player.getPlayed().getCards().add(austellarCorporation.getId());
        player.setSelectedCorporationCard(austellarCorporation.getId());

        PolicyRecord mainRecord = new PolicyRecord();
        encoder.encode(game, player, anotherPlayer, mainRecord);
        mainRecord.choosingTag = true;
        mainRecord.initialSetup = true;

        player.setMc(0);
        player.getPlayed().getCards().clear();
        player.setSelectedCorporationCard(null);

        List<HeadAction> legalTagChoiceAction = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            legalTagChoiceAction.add(ActionInputService.chooseTag(i));
        }

        PolicyPrediction tagPrediction = policyNNService
                .predictBatch(List.of(policyRecordFeatureConverter.convert(mainRecord)), PolicyNNService.ModelType.FIRST)
                .getFirst();

        HeadAction bestTagAction = actionSelector.chooseBestAction(tagPrediction, legalTagChoiceAction);
        Tag bestTagChoice = Tag.values()[legalTagChoiceAction.indexOf(bestTagAction)];

        Map<Integer, List<Integer>> corporationInput = new HashMap<>();
        corporationInput.put(InputFlag.TAG_INPUT.getId(), List.of(bestTagChoice.ordinal()));

        mainRecord.choosingTag = false;
        mainRecord.austellarChoosing = true;

        switch (bestTagChoice) {
            case SPACE -> mainRecord.spaceTags[0]++;
            case EARTH -> mainRecord.earthTags[0]++;
            case EVENT -> mainRecord.eventTags[0]++;
            case SCIENCE -> mainRecord.scienceTags[0]++;
            case PLANT -> mainRecord.plantTags[0]++;
            case ENERGY -> mainRecord.energyTags[0]++;
            case BUILDING -> mainRecord.buildingTags[0]++;
            case ANIMAL -> mainRecord.animalTags[0]++;
            case JUPITER -> mainRecord.jupiterTags[0]++;
            case MICROBE -> mainRecord.microbeTags[0]++;
        }

        List<HeadAction> legalMilestoneChoiceActions = new ArrayList<>(game.getMilestones().size());

        for (int i = 0; i < game.getMilestones().size(); i++) {
            MilestoneType chosenMilestone = game.getMilestones().get(i).getType();
            legalMilestoneChoiceActions.add(ActionInputService.austellarMilestoneChoice(AiConstants.MILESTONE_TYPES.indexOf(chosenMilestone)));
        }

        PolicyPrediction milestonePrediction = policyNNService
                .predictBatch(List.of(policyRecordFeatureConverter.convert(mainRecord)), PolicyNNService.ModelType.FIRST)
                .getFirst();

        HeadAction bestMilestoneAction = actionSelector.chooseBestAction(milestonePrediction, legalMilestoneChoiceActions);
        corporationInput.put(InputFlag.AUSTELLAR_CORPORATION_MILESTONE.getId(), List.of(legalMilestoneChoiceActions.indexOf(bestMilestoneAction)));

        return corporationInput;
    }

    private HeadAction decide(
            MarsGame game,
            Player currentPlayer,
            Consumer<PolicyRecord> recordCustomizer,
            List<HeadAction> legalActions
    ) {
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());

        Player anotherPlayer = players.get(0) == currentPlayer ? players.get(1) : players.get(0);

        PolicyRecord record = new PolicyRecord();
        encoder.encode(game, currentPlayer, anotherPlayer, record);

        recordCustomizer.accept(record);

        PolicyPrediction prediction = policyNNService
                .predictBatch(
                        List.of(policyRecordFeatureConverter.convert(record)),
                        PolicyNNService.ModelType.FIRST
                )
                .getFirst();

        return actionSelector.chooseBestAction(prediction, legalActions);
    }
}
