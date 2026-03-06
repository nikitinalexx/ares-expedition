package com.terraforming.ares.services.policyai.service;

import com.terraforming.ares.cards.blue.FilterFeeders;
import com.terraforming.ares.cards.buffedCorporations.BuffedArclightCorporation;
import com.terraforming.ares.cards.corporations.ArclightCorporation;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.awards.AwardType;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.payments.PaymentType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.dl4j.PolicyNNService;
import com.terraforming.ares.services.ai.dl4j.PolicyPrediction;
import com.terraforming.ares.services.ai.network2.Network2PaymentService;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.action.ActionRegistry;
import com.terraforming.ares.services.policyai.action.HeadAction;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.encoder.PolicyRecordFeatureConverter;
import com.terraforming.ares.services.policyai.input.InputGeneratorEffect;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractPolicyProcessor {

    @Autowired
    private PolicyNNService policyNNService;

    @Autowired
    private PolicyRecordFeatureConverter policyRecordFeatureConverter;

    @Autowired
    private PolicyActionSelector actionSelector;

    @Autowired
    private AiTurnService aiTurnService;

    @Autowired
    private CardService cardService;

    @Autowired
    private PolicyCardBuildParamsService policyCardBuildParamsService;

    @Autowired
    private Network2PaymentService network2PaymentService;

    protected void doBuild(Card cardToBuild, PolicyRecord record, List<InputGeneratorEffect> inputGeneratorEffects, TableContext tableContext) {
        MarsGame game = tableContext.getGame();
        Player player = tableContext.getPlayer();
        record.selectCard(cardToBuild);

        //dynamic tag affects payment discount, should decide first
        Map<Integer, List<Integer>> inputParams = generateDynamicTagInput(record, cardToBuild, tableContext);

        List<List<Payment>> allPossibleCardPayments = network2PaymentService.getAllPossibleCardPayments(game, player, cardToBuild, Map.of());
        List<Payment> bestPayment = chooseBuildPaymentPolicy(record, allPossibleCardPayments);

        addNonDynamicTagInput(inputGeneratorEffects, record, cardToBuild, tableContext, inputParams);

        aiTurnService.buildProject(game, player, cardToBuild.getId(), bestPayment, inputParams);
        return;
    }

    private List<Payment> chooseBuildPaymentPolicy(PolicyRecord record, List<List<Payment>> paymentsList) {
        if (paymentsList.isEmpty()) {
            throw new IllegalStateException("Unable to pay for the project");
        } else if (paymentsList.size() == 1) {
            return paymentsList.getFirst();
        }

        record.payingForTheBuild = true;

        Map<HeadAction, List<Payment>> actionToPayments = new HashMap<>();

        List<HeadAction> validActions = new ArrayList<>();

        outer:
        for (List<Payment> payments : paymentsList) {
            for (Payment payment : payments) {
                if (payment.getType() == PaymentType.MEGACREDITS && record.mc[0] < payment.getValue()) {
                    continue outer;
                }
            }

            HeadAction action = ActionInputService.payForTheBuild(getPaymentType(payments));
            actionToPayments.put(action, payments);
            validActions.add(action);
        }

        HeadAction bestAction = predict(record, validActions);

        record.payingForTheBuild = false;

        return actionToPayments.get(bestAction);
    }

    private Map<Integer, List<Integer>> generateDynamicTagInput(
            PolicyRecord record,
            Card card,
            TableContext context) {


        int dynamicCount = 0;
        for (Tag tag : card.getTags()) {
            if (tag == Tag.DYNAMIC) {
                dynamicCount++;
            }
        }
        Map<Integer, List<Integer>> input = new HashMap<>();

        if (dynamicCount != 1) {
            return input;
        }

        input.put(InputFlag.TAG_INPUT.getId(), List.of(getDynamicTagChoice(record, context.getGame(), context.getPlayer(), card)));

        return input;
    }

    private void addNonDynamicTagInput(
            List<InputGeneratorEffect> inputGeneratorEffects,
            PolicyRecord record,
            Card card,
            TableContext context,
            Map<Integer, List<Integer>> input) {

        for (InputGeneratorEffect effect : inputGeneratorEffects) {
            if (effect.isPresent(record, context, card, input)) {
                effect.generateAndApply(record, context, input, card);
            }
        }
    }

    private Integer getDynamicTagChoice(PolicyRecord record, MarsGame game, Player player, Card card) {
        List<HeadAction> legalTagChoiceAction = new ArrayList<>();
        Map<HeadAction, Integer> actionToTag = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            if (!network2PaymentService.hasEnoughMoneyForAnyBuild(game, player, card, Map.of(InputFlag.TAG_INPUT.getId(), List.of(i)))) {
                continue;
            }

            HeadAction possibleAction = ActionInputService.chooseTag(i);
            legalTagChoiceAction.add(possibleAction);
            actionToTag.put(possibleAction, i);
        }
        if (legalTagChoiceAction.isEmpty()) {
            throw new IllegalArgumentException("No valid tags were provided");
        } else if (legalTagChoiceAction.size() == 1) {
            return actionToTag.values().iterator().next();
        }
        record.choosingTag = true;

        HeadAction bestTagAction = predict(record, legalTagChoiceAction);

        record.choosingTag = false;

        Tag bestTagChoice = Tag.values()[actionToTag.get(bestTagAction)];

        switch (bestTagChoice) {
            case SPACE -> record.spaceTags[0]++;
            case EARTH -> record.earthTags[0]++;
            case EVENT -> record.eventTags[0]++;
            case SCIENCE -> record.scienceTags[0]++;
            case PLANT -> record.plantTags[0]++;
            case ENERGY -> record.energyTags[0]++;
            case BUILDING -> record.buildingTags[0]++;
            case ANIMAL -> record.animalTags[0]++;
            case JUPITER -> record.jupiterTags[0]++;
            case MICROBE -> record.microbeTags[0]++;
        }

        return bestTagChoice.ordinal();
    }

    protected List<Card> getAvailableProjects(MarsGame game, Player player) {
        List<Card> filteredCards = getPlayableCardsByColor(game, player);
        if (filteredCards.isEmpty()) {
            return List.of();
        }

        return policyCardBuildParamsService.getMinimalBuildParamVariations(game, player, filteredCards);
    }

    private List<Card> getPlayableCardsByColor(MarsGame game, Player player) {
        boolean canBuildGreen = player.canBuildGreen();
        boolean canBuildBlueRed = player.canBuildBlueRed();

        return player.getHand().getCards().stream()
                .map(cardService::getCard)
                .filter(card ->
                        !card.isBlankCard() &&
                                (
                                        (canBuildGreen && card.getColor() == CardColor.GREEN)
                                                || (canBuildBlueRed && (card.getColor() == CardColor.BLUE || card.getColor() == CardColor.RED))
                                )
                ).toList();
    }

    protected HeadAction predict(PolicyRecord record, List<HeadAction> validActions) {
        if (validActions.isEmpty()) {
            throw new IllegalArgumentException("No valid actions were provided");
        }
        if (validActions.size() == 1) {
            return validActions.getFirst();
        }

        PolicyPrediction prediction = policyNNService.predictBatch(List.of(policyRecordFeatureConverter.convert(record)), PolicyNNService.ModelType.FIRST).getFirst();

        return actionSelector.chooseBestAction(prediction, validActions);
    }

    protected void doHelionExchange(PolicyRecord record, Player player) {
        record.helionExchanging = true;

        int exchangeCount = 0;

        while (record.heat[0] > 0) {
            List<HeadAction> helionValidActions = exchangeCount > 0
                    ? List.of(ActionInputService.exchange1Heat(), ActionInputService.passAction())
                    : List.of(ActionInputService.exchange1Heat());

            HeadAction bestHelionAction = predict(record, helionValidActions);

            if (bestHelionAction == ActionInputService.passAction()) {
                break;
            }

            record.mc[0]++;
            record.heat[0]--;
            exchangeCount++;
        }

        record.helionExchanging = false;


        player.setMc(player.getMc() + exchangeCount);
        player.setHeat(player.getHeat() - exchangeCount);
    }

    protected void doCardExchange(PolicyRecord record, MarsGame game, Player player) {
        record.sellingCards = true;

        List<Integer> cardsToSell = new ArrayList<>();
        List<HeadAction> sellCardsValidActions = new ArrayList<>();
        Map<HeadAction, Card> actionToSellCard = new HashMap<>();

        for (Integer cardToSellId : player.getHand().getCards()) {
            Card cardToSell = cardService.getCard(cardToSellId);
            HeadAction sellAction = ActionInputService.sellDiscardCardAction(cardToSell);
            sellCardsValidActions.add(sellAction);
            actionToSellCard.put(sellAction, cardToSell);
        }

        while (!sellCardsValidActions.isEmpty()) {
            HeadAction bestSellTurn = predict(record, sellCardsValidActions);

            if (bestSellTurn == ActionInputService.passAction()) {
                break;
            }

            Card cardToSell = actionToSellCard.get(bestSellTurn);
            cardsToSell.add(cardToSell.getId());

            // убираем проданную карту из доступных действий
            sellCardsValidActions.remove(bestSellTurn);
            actionToSellCard.remove(bestSellTurn);

            // rollout — карта ушла из руки
            record.removeCardFromHand(cardToSell);

            if (cardsToSell.size() == 1) {
                sellCardsValidActions.add(ActionInputService.passAction());
            }
        }

        record.sellingCards = false;
        aiTurnService.sellCards(player, game, cardsToSell);
    }

    protected int getPaymentType(List<Payment> payments) {
        int type = 0;
        if (payments.size() == 3) {
            type = 3;
        } else if (payments.size() == 2) {
            if (payments.getFirst().getType() == PaymentType.RESTRUCTURED_RESOURCES) {
                type = 1;
            } else if (payments.getFirst().getType() == PaymentType.ANAEROBIC_MICROORGANISMS) {
                type = 2;
            } else {
                throw new IllegalStateException("Invalid payment");
            }
        }
        return type;
    }

    protected void microbeAdded(PolicyRecord record, int count) {
        int total = count;
        if (record.hasPlayedBlueCard(FilterFeeders.class)) {
            total++;
            record.animalResources[0][ActionRegistry.ANIMAL_TARGET_TO_OFFSET.get(FilterFeeders.class)]++;
        }
        record.addAwardProgress(AwardType.COLLECTOR, total);
    }

    protected void rolloutAnimal(Class<?> target, PolicyRecord record, int count) {
        if (target == ArclightCorporation.class || target == BuffedArclightCorporation.class) {
            record.arclightResources[0] += (byte) count;
        } else {
            record.animalResources[0][ActionRegistry.ANIMAL_TARGET_TO_OFFSET.get(target)] += (byte) count;
        }
        record.addAwardProgress(AwardType.COLLECTOR, count);
    }

    protected void generatePhaseUpgrade(PolicyRecord record, Map<Integer, List<Integer>> input, int phaseIndex) {
        List<HeadAction> validActions = new ArrayList<>();

        HeadAction firstUpgrade = ActionInputService.choosePhaseUpgradeAction(phaseIndex * 2);
        HeadAction secondUpgrade = ActionInputService.choosePhaseUpgradeAction(phaseIndex * 2 + 1);

        validActions.add(firstUpgrade);
        validActions.add(secondUpgrade);

        HeadAction best = predict(record, validActions);

        input.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(firstUpgrade == best ? (phaseIndex * 2) : (phaseIndex * 2 + 1)));
        record.setPhaseUpgrade(phaseIndex);
    }

    protected void generatePhaseUpgrade(PolicyRecord record, Map<Integer, List<Integer>> input) {
        List<HeadAction> validActions = new ArrayList<>();
        Map<HeadAction, Integer> choiceToPhaseUpgrade = new HashMap<>();

        int[] phaseUpgrades = record.getPhaseUpgrades();
        for (int i = 0; i < phaseUpgrades.length; i++) {
            int phaseUpgrade = phaseUpgrades[i];
            if (phaseUpgrade == 0) {
                HeadAction firstUpgrade = ActionInputService.choosePhaseUpgradeAction(i * 2);
                HeadAction secondUpgrade = ActionInputService.choosePhaseUpgradeAction(i * 2 + 1);

                validActions.add(firstUpgrade);
                validActions.add(secondUpgrade);

                choiceToPhaseUpgrade.put(firstUpgrade, i * 2);
                choiceToPhaseUpgrade.put(secondUpgrade, i * 2 + 1);
            } else if (phaseUpgrade == 1) {
                HeadAction secondUpgrade = ActionInputService.choosePhaseUpgradeAction(i * 2 + 1);
                validActions.add(secondUpgrade);
                choiceToPhaseUpgrade.put(secondUpgrade, i * 2 + 1);
            } else {
                HeadAction firstUpgrade = ActionInputService.choosePhaseUpgradeAction(i * 2);
                validActions.add(firstUpgrade);
                choiceToPhaseUpgrade.put(firstUpgrade, i * 2);
            }
        }
        if (validActions.size() == phaseUpgrades.length) {
            validActions.add(ActionInputService.passAction());
        }

        HeadAction best = predict(record, validActions);

        if (best == ActionInputService.passAction()) {
            //game doesn't actually allow the pass, so we simulate no action through taking the upgrade we already have
            input.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(phaseUpgrades[0]));
        } else {
            int phaseIndex = choiceToPhaseUpgrade.get(best);
            input.put(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(phaseIndex));
            record.setPhaseUpgrade(phaseIndex);
        }
    }

}
