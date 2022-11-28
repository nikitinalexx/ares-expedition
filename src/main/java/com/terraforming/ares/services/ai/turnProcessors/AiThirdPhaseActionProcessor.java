package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import com.terraforming.ares.services.StandardProjectService;
import com.terraforming.ares.services.ai.AiProjectionService;
import com.terraforming.ares.services.ai.helpers.AiCardActionHelper;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 23.11.2022
 */
@Component
public class AiThirdPhaseActionProcessor {
    private final Random random = new Random();
    private final AiTurnService aiTurnService;
    private final CardService cardService;
    private final CardValidationService cardValidationService;
    private final AiPaymentService aiPaymentHelper;
    private final AiCardActionHelper aiCardActionHelper;
    private final StandardProjectService standardProjectService;
    private final AiProjectionService aiProjectionService;

    public AiThirdPhaseActionProcessor(AiTurnService aiTurnService,
                                       CardService cardService,
                                       CardValidationService cardValidationService,
                                       AiPaymentService aiPaymentHelper, AiCardActionHelper aiCardActionHelper, StandardProjectService standardProjectService, AiProjectionService aiProjectionService) {

        this.aiTurnService = aiTurnService;
        this.cardService = cardService;
        this.cardValidationService = cardValidationService;
        this.aiPaymentHelper = aiPaymentHelper;
        this.aiCardActionHelper = aiCardActionHelper;
        this.standardProjectService = standardProjectService;
        this.aiProjectionService = aiProjectionService;
    }

    public boolean processTurn(List<TurnType> possibleTurns, MarsGame game, Player player) {
        if (player.getUuid().endsWith("0") && Constants.FIRST_BOT_IS_RANDOM) {
            boolean madeATurn = oldRandomProcessTurn(game, player);
            if (madeATurn) {
                return true;
            } else {
                if (doStandardActionsIfAvailable(possibleTurns, game, player)) {
                    return true;
                }
            }
        } else {
            boolean didBestThirdPhaseTurn = aiProjectionService.doBestThirdPhaseTurn(possibleTurns, game, player);
            if (didBestThirdPhaseTurn) {
                return true;
            }
            if (doStandardActionsIfAvailable(possibleTurns, game, player)) {
                return true;
            }
        }

        if (game.gameEndCondition()) {
            if (player.getHand().size() != 0) {
                aiTurnService.sellAllCards(player, game, player.getHand().getCards());
                return true;
            } else {
                if (!game.getPlanetAtTheStartOfThePhase().isOxygenMax()) {
                    String validationResult = standardProjectService.validateStandardProject(game, player, StandardProjectType.FOREST);
                    if (validationResult == null) {
                        aiTurnService.standardProjectTurn(game, player, StandardProjectType.FOREST);
                        return true;
                    }
                }
                String validationResult = standardProjectService.validateStandardProject(game, player, StandardProjectType.OCEAN);
                if (validationResult == null) {
                    aiTurnService.standardProjectTurn(game, player, StandardProjectType.OCEAN);
                    return true;
                }
                validationResult = standardProjectService.validateStandardProject(game, player, StandardProjectType.TEMPERATURE);
                if (validationResult == null) {
                    aiTurnService.standardProjectTurn(game, player, StandardProjectType.TEMPERATURE);
                    return true;
                }
                validationResult = standardProjectService.validateStandardProject(game, player, StandardProjectType.FOREST);
                if (validationResult == null) {
                    aiTurnService.standardProjectTurn(game, player, StandardProjectType.FOREST);
                    return true;
                }
            }

            aiTurnService.confirmGameEnd(game, player);
            return true;
        }

        aiTurnService.skipTurn(player);
        return true;
    }


    private boolean oldRandomProcessTurn(MarsGame game, Player player) {
        Deck activatedBlueCards = player.getActivatedBlueCards();

        List<Card> notUsedBlueCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(Card::isActiveCard)
                .filter(c -> !activatedBlueCards.containsCard(c.getId()))
                .collect(Collectors.toList());

        boolean actionPerformed = performAction(game, player, notUsedBlueCards);

        if (actionPerformed) {
            return true;
        }

        if (player.getChosenPhase() == 3 && !player.isActivatedBlueActionTwice()) {
            List<Card> actionBlueCards = player.getPlayed().getCards().stream()
                    .map(cardService::getCard)
                    .filter(Card::isActiveCard)
                    .collect(Collectors.toList());

            actionPerformed = performAction(game, player, actionBlueCards);
        }

        if (actionPerformed) {
            return true;
        }

        Set<CardAction> playedActions = player.getPlayed().getCards().stream().map(cardService::getCard)
                .map(Card::getCardMetadata)
                .filter(Objects::nonNull)
                .map(CardMetadata::getCardAction)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        if (!game.getPlanetAtTheStartOfThePhase().isOceansMax()) {
            if ((playedActions.contains(CardAction.ARCTIC_ALGAE)
                    || playedActions.contains(CardAction.FISH))
                    && player.getMc() > 30) {
                aiTurnService.standardProjectTurn(game, player, StandardProjectType.OCEAN);
                return true;
            }
        }
        if (!game.getPlanetAtTheStartOfThePhase().isTemperatureMax()) {
            if ((playedActions.contains(CardAction.LIVESTOCK))
                    && player.getMc() > 30) {
                aiTurnService.standardProjectTurn(game, player, StandardProjectType.TEMPERATURE);
                return true;
            }
            if ((playedActions.contains(CardAction.PHYSICS_COMPLEX))
                    && player.getMc() > 70) {
                aiTurnService.standardProjectTurn(game, player, StandardProjectType.TEMPERATURE);
                return true;
            }
        }

        if (!game.gameEndCondition() && canFinishGame(game, player)) {
            if (player.getHand().size() != 0) {
                aiTurnService.sellAllCards(player, game, player.getHand().getCards());
                return true;
            }
            if (game.getPlanet().temperatureLeft() > 0) {
                aiTurnService.standardProjectTurn(game, player, StandardProjectType.TEMPERATURE);
                return true;
            }
            if (game.getPlanet().oxygenLeft() > 0) {
                aiTurnService.standardProjectTurn(game, player, StandardProjectType.FOREST);
                return true;
            }
            if (game.getPlanet().oceansLeft() > 0) {
                aiTurnService.standardProjectTurn(game, player, StandardProjectType.OCEAN);
                return true;
            }
        }

        return false;
    }

    private boolean doStandardActionsIfAvailable(List<TurnType> possibleTurns, MarsGame game, Player player) {
        if (possibleTurns.contains(TurnType.INCREASE_TEMPERATURE)) {
            aiTurnService.increaseTemperature(game, player);
            return true;
        }
        if (possibleTurns.contains(TurnType.PLANT_FOREST)) {
            aiTurnService.plantForest(game, player);
            return true;
        }
        return false;
    }

    private boolean canFinishGame(MarsGame game, Player player) {
        int mc = player.getMc();
        mc += player.getHand().size() * 3;

        mc -= game.getPlanet().oceansLeft() * standardProjectService.getProjectPrice(player, StandardProjectType.OCEAN);

        if (mc < 0) {
            return false;
        }

        mc -= game.getPlanet().temperatureLeft() * standardProjectService.getProjectPrice(player, StandardProjectType.TEMPERATURE);

        if (mc < 0) {
            return false;
        }

        mc -= game.getPlanet().oxygenLeft() * standardProjectService.getProjectPrice(player, StandardProjectType.FOREST);

        return mc >= 0;
    }

    private boolean performAction(MarsGame game, Player player, List<Card> cards) {
        while (!cards.isEmpty()) {
            int selectedIndex = random.nextInt(cards.size());
            Card selectedCard = cards.get(selectedIndex);

            if (aiCardActionHelper.validateAction(game, player, selectedCard) == null) {
                aiTurnService.performBlueAction(
                        game,
                        player,
                        selectedCard.getId(),
                        aiCardActionHelper.getActionInputParams(game, player, selectedCard)
                );
                return true;
            } else {
                cards.remove(selectedIndex);
            }
        }
        return false;
    }

}