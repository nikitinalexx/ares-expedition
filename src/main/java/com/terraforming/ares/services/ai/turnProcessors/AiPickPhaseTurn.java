package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.ai.helpers.AiCardActionHelper;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsHelper;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
@RequiredArgsConstructor
public class AiPickPhaseTurn implements AiTurnProcessor {
    private final Random random = new Random();
    private final AiTurnService aiTurnService;
    private final CardService cardService;
    private final AiCardActionHelper aiCardActionHelper;
    private final CardValidationService cardValidationService;
    private final AiPaymentService aiPaymentHelper;
    private final AiCardBuildParamsHelper aiCardParamsHelper;
    private final SpecialEffectsService specialEffectsService;
    private final DraftCardsService draftCardsService;


    @Override
    public TurnType getType() {
        return TurnType.PICK_PHASE;
    }

    @Override
    public boolean processTurn(MarsGame game, Player player) {
        Integer previousChosenPhase = player.getPreviousChosenPhase();


        List<Integer> possiblePhases = new ArrayList<>();

        if (mayPlayPhaseOne(game, player)) {
            possiblePhases.add(1);
        }

        if (mayPlayPhaseTwo(game, player)) {
            possiblePhases.add(2);
        }

        if (mayPlayPhaseThree(game, player)) {
            possiblePhases.add(3);
        }

        if (mayPlayPhaseFour(game, player)) {
            possiblePhases.add(4);
        }

        if (mayPlayPhaseFive(game, player)) {
            possiblePhases.add(5);
        }

        int chosenPhase;

        if (possiblePhases.isEmpty()) {
            chosenPhase = random.nextInt(previousChosenPhase != null ? 4 : 5) + 1;
            if (previousChosenPhase != null && chosenPhase == previousChosenPhase) {
                chosenPhase++;
            }
        } else {
            chosenPhase = possiblePhases.get(random.nextInt(possiblePhases.size()));
        }

        aiTurnService.choosePhaseTurn(player, chosenPhase);

        return true;
    }

    private boolean mayPlayPhaseOne(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 1) {
            return false;
        }

        return player.getHand()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .filter(card -> card.getColor() == CardColor.GREEN)
                .filter(card ->
                {
                    String errorMessage = cardValidationService.validateCard(
                            player, game, card.getId(),
                            aiPaymentHelper.getCardPayments(player, card),
                            aiCardParamsHelper.getInputParamsForValidation(player, card)
                    );
                    return errorMessage == null;
                })
                .limit(2)
                .count() == 2;
    }

    private boolean mayPlayPhaseTwo(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 2) {
            return false;
        }

        return player.getHand()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .filter(card -> card.getColor() == CardColor.BLUE || card.getColor() == CardColor.RED)
                .filter(card ->
                {
                    String errorMessage = cardValidationService.validateCard(
                            player, game, card.getId(),
                            aiPaymentHelper.getCardPayments(player, card),
                            aiCardParamsHelper.getInputParamsForValidation(player, card)
                    );
                    return errorMessage == null;
                })
                .limit(2)
                .count() == 2;
    }

    private boolean mayPlayPhaseFour(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 4) {
            return false;
        }

        if (player.getCardIncome() >= 3) {//when can get a lot of card
            return true;
        }

        if (player.getMc() <= 5) {//when running really low unable to do anything
            return true;
        }

        //when have a good income
        if (player.getMcIncome() > 20 || player.getHeatIncome() > 15 || player.getPlantsIncome() > 5) {
            return true;
        }

        //when need and can fill restructured resources
        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.RESTRUCTURED_RESOURCES)
                && player.getPlantsIncome() > 0
                && player.getPlants() == 0) {
            return true;
        }

        return false;
    }

    private boolean mayPlayPhaseThree(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 3) {
            return false;
        }

        return player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(Card::isActiveCard)
                .filter(card -> aiCardActionHelper.validateAction(game, player, card) == null)
                .limit(3)
                .count() == 3;
    }

    private boolean mayPlayPhaseFive(MarsGame game, Player player) {
        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == 5) {
            return false;
        }

        if (draftCardsService.countExtraCardsToTake(player) >= 2
                || draftCardsService.countExtraCardsToDraft(player) >= 2) {
            return true;
        }

        boolean hasCardsToBuild = player.getHand()
                .getCards()
                .stream()
                .map(cardService::getCard)
                .filter(card -> card.getColor() == CardColor.GREEN)
                .filter(card ->
                {
                    String errorMessage = cardValidationService.validateCard(
                            player, game, card.getId(),
                            aiPaymentHelper.getCardPayments(player, card),
                            aiCardParamsHelper.getInputParamsForValidation(player, card)
                    );
                    return errorMessage == null;
                })
                .limit(2)
                .count() == 2;

        if (!hasCardsToBuild) {
            hasCardsToBuild = player.getHand()
                    .getCards()
                    .stream()
                    .map(cardService::getCard)
                    .filter(card -> card.getColor() == CardColor.BLUE || card.getColor() == CardColor.RED)
                    .filter(card ->
                    {
                        String errorMessage = cardValidationService.validateCard(
                                player, game, card.getId(),
                                aiPaymentHelper.getCardPayments(player, card),
                                aiCardParamsHelper.getInputParamsForValidation(player, card)
                        );
                        return errorMessage == null;
                    })
                    .limit(2)
                    .count() == 2;
        }

        if (!hasCardsToBuild) {
            return true;
        }

        return false;
    }


}
