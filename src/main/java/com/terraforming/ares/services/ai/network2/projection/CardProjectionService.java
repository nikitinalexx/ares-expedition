package com.terraforming.ares.services.ai.network2.projection;

import com.terraforming.ares.cards.red.SyntheticCatastrophe;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService;
import com.terraforming.ares.services.ai.dl4j.NNService;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import com.terraforming.ares.services.ai.network2.buildParams.OptimizedInputDecisions;
import com.terraforming.ares.services.ai.network2.buildParams.SharedInputAnalysis;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import com.terraforming.ares.services.ai.turnProcessors.AiUtility;
import com.terraforming.ares.services.ai.network2.Network2PaymentService;
import com.terraforming.ares.services.ai.network2.Network2ProjectBuildService;
import com.terraforming.ares.services.ai.network2.buildParams.AiOptimalBuildService;
import com.terraforming.ares.services.ai.network2.buildParams.CardWithInputParams;
import com.terraforming.ares.services.ai.network2.dto.CardWithChanceAndInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardProjectionService {
    private final Network2ProjectBuildService network2ProjectBuildService;
    private final AiOptimalBuildService aiOptimalBuildService;
    private final Network2PaymentService network2PaymentService;
    private final CardValidationService cardValidationService;
    private final AdvancedAiDataCollectionService advancedAiDataCollectionService;
    private final AiTurnService aiTurnService;
    private final AiUtility aiUtility;
    private final CardService cardService;

    public List<CardWithChanceAndInput> getAvailableProjectsSync(MarsGame game, Player player, Map<SharedInputAnalysis, OptimizedInputDecisions> optimizedInputs) {
        List<Card> filteredCards = getPotentiallyPlayableCards(game, player);
        if (filteredCards.isEmpty()) {
            return List.of();
        }

        List<CardWithInputParams> optimalInputsForCards = aiOptimalBuildService.getOptimalInputsForCards(game, player, filteredCards, optimizedInputs);

        List<CardWithChanceAndInput> cardWithChanceAndInputs = new ArrayList<>();

        for (CardWithInputParams cardWithInputParams : optimalInputsForCards) {
            Card card = cardWithInputParams.getCard();
            List<Map<Integer, List<Integer>>> inputParamsVariations = cardWithInputParams.getInputParamsVariations();
            List<List<Payment>> allPossibleCardPayments = network2PaymentService.getAllPossibleCardPayments(game, player, card, inputParamsVariations.isEmpty() ? Map.of() : inputParamsVariations.getFirst());

            for (Map<Integer, List<Integer>> inputParams : inputParamsVariations) {
                for (List<Payment> possiblePayments : allPossibleCardPayments) {
                    cardWithChanceAndInputs.add(CardWithChanceAndInput.builder()
                            .card(card)
                            .inputParameters(inputParams)
                            .payments(possiblePayments)
                            .build());
                }
            }
        }


        return cardWithChanceAndInputs.stream().filter(cardWithChanceAndInput -> {
            Map<Integer, List<Integer>> inputParameters = cardWithChanceAndInput.getInputParameters();
            if (inputParameters.containsKey(InputFlag.MARS_UNIVERSITY_DUMMY_INPUT.getId())) {
                inputParameters = new HashMap<>(inputParameters);
                inputParameters.put(InputFlag.MARS_UNIVERSITY_CARD.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
            }

            return cardValidationService.validateCard(player, game, cardWithChanceAndInput.getCard().getId(), cardWithChanceAndInput.getPayments(), inputParameters) == null;
        }).toList();
    }

    private List<Card> getPotentiallyPlayableCards(MarsGame game, Player player) {
        boolean canBuildGreen = player.canBuildGreen();
        boolean canBuildBlueRed = player.canBuildBlueRed();

        List<Card> filteredCards = player.getHand().getCards().stream()
                .map(cardService::getCard)
                .filter(card -> !card.isBlankCard() //blank cards only simulate cards that come to hand but aren't playable in reality
                        && (canBuildGreen && card.getColor() == CardColor.GREEN) || (canBuildBlueRed && (card.getColor() == CardColor.BLUE || card.getColor() == CardColor.RED)))
                .collect(Collectors.toCollection(ArrayList::new));
        filteredCards.removeAll(network2ProjectBuildService.getCardsWithStrongRestrictions(game, player, filteredCards));
        return filteredCards;
    }

    public MarsGame projectBuildCardWithRequirements(MarsGame game, Player player, CardWithChanceAndInput cardWithChanceAndInput) {
        game = new MarsGame(game);
        player = game.getPlayerByUuid(player.getUuid());
        Card card = cardWithChanceAndInput.getCard();

        Map<Integer, List<Integer>> inputParameters = cardWithChanceAndInput.getInputParameters();

        List<Integer> marsDummyInput = inputParameters.get(InputFlag.MARS_UNIVERSITY_DUMMY_INPUT.getId());
        int marsUniversityCardsToSimulate = 0;
        if (!CollectionUtils.isEmpty(marsDummyInput)) {
            marsUniversityCardsToSimulate = marsDummyInput.getFirst();
            inputParameters = new HashMap<>(inputParameters);
            inputParameters.put(InputFlag.MARS_UNIVERSITY_CARD.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
        }

        int originalPlayerHandSize = player.getHand().size() - 1;//-1 because we build 1 card

        if (card.getColor() == CardColor.GREEN) {
            aiTurnService.buildGreenProjectSync(game, player, card.getId(), cardWithChanceAndInput.getPayments(), inputParameters);
        } else {
            aiTurnService.buildBlueRedProjectSync(game, player, card.getId(), cardWithChanceAndInput.getPayments(), inputParameters);
        }

        aiUtility.simulateDummyHandInsteadOfNewCards(player, originalPlayerHandSize);
        aiUtility.addDummyCards(player, marsUniversityCardsToSimulate);

        if (card.getClass() == SyntheticCatastrophe.class) {
            int redCardInput = inputParameters.get(InputFlag.SYNTHETIC_CATASTROPHE_CARD.getId()).getFirst();
            Integer dummyInsteadOfRedCard = player.getHand().getCards().removeLast();
            assert dummyInsteadOfRedCard == AiConstants.GENERIC_DUMMY_ID;
            player.getHand().addCard(redCardInput);
        }

        return game;
    }


}
