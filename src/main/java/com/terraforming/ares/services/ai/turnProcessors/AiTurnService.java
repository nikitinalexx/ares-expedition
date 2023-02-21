package com.terraforming.ares.services.ai.turnProcessors;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.request.ChooseCorporationRequest;
import com.terraforming.ares.model.turn.*;
import com.terraforming.ares.processors.turn.TurnProcessor;
import com.terraforming.ares.services.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 24.11.2022
 */
@Component
public class AiTurnService {
    private final CardValidationService cardValidationService;
    private final Map<TurnType, TurnProcessor<?>> turnProcessors;
    private final PaymentValidationService paymentValidationService;
    private final TerraformingService terraformingService;
    private final TurnTypeService turnTypeService;
    private final StandardProjectService standardProjectService;
    private final CardService cardService;

    public AiTurnService(List<TurnProcessor<?>> turnProcessor,
                         CardValidationService cardValidationService,
                         PaymentValidationService paymentValidationService, TerraformingService terraformingService, TurnTypeService turnTypeService, StandardProjectService standardProjectService, CardService cardService) {
        this.cardValidationService = cardValidationService;
        this.paymentValidationService = paymentValidationService;

        turnProcessors = turnProcessor.stream().collect(Collectors.toMap(
                TurnProcessor::getType, Function.identity()
        ));
        this.terraformingService = terraformingService;
        this.turnTypeService = turnTypeService;
        this.standardProjectService = standardProjectService;
        this.cardService = cardService;
    }

    public void chooseCorporationTurn(MarsGame game, ChooseCorporationRequest chooseCorporationRequest) {
        String playerUuid = chooseCorporationRequest.getPlayerUuid();
        Player player = game.getPlayerUuidToPlayer().get(playerUuid);
        Integer corporationCardId = chooseCorporationRequest.getCorporationId();

        if (!game.getPlayerByUuid(playerUuid).getCorporations().containsCard(corporationCardId)) {
            throw new IllegalStateException("Can't pick corporation that is not in your choice deck");
        }

        makeAsyncTurn(player, new CorporationChoiceTurn(playerUuid, corporationCardId));
    }

    public void choosePhaseTurn(Player player, int phaseId) {
        if (phaseId < 1 || phaseId > 5) {
            throw new IllegalStateException("Phase is not within [1..5] range");
        }

        if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == phaseId) {
            throw new IllegalStateException("This phase already picked in previous round");
        }

        makeAsyncTurn(player, new PhaseChoiceTurn(player.getUuid(), phaseId));
    }

    public void buildProject(MarsGame game, Player player, int projectId, List<Payment> payments, Map<Integer, List<Integer>> inputParams) {
        buildProject(
                game, player, projectId, payments, inputParams,
                cardService.getCard(projectId).getColor() == CardColor.GREEN
                        ? new BuildGreenProjectTurn(player.getUuid(), projectId, payments, inputParams)
                        : new BuildBlueRedProjectTurn(player.getUuid(), projectId, payments, inputParams),
                game.getCurrentPhase() == 3
        );
    }

    public void buildGreenProjectSync(MarsGame game, Player player, int projectId, List<Payment> payments, Map<Integer, List<Integer>> inputParams) {
        buildProject(
                game, player, projectId, payments, inputParams,
                new BuildGreenProjectTurn(player.getUuid(), projectId, payments, inputParams),
                true
        );
    }

    public void buildGreenProject(MarsGame game, Player player, int projectId, List<Payment> payments, Map<Integer, List<Integer>> inputParams) {
        buildProject(
                game, player, projectId, payments, inputParams,
                new BuildGreenProjectTurn(player.getUuid(), projectId, payments, inputParams),
                game.getCurrentPhase() == 3
        );
    }

    public void buildBlueRedProjectSync(MarsGame game, Player player, int projectId, List<Payment> payments, Map<Integer, List<Integer>> inputParams) {
        buildProject(
                game, player, projectId, payments, inputParams,
                new BuildBlueRedProjectTurn(player.getUuid(), projectId, payments, inputParams),
                true
        );
    }

    public void buildBlueRedProject(MarsGame game, Player player, int projectId, List<Payment> payments, Map<Integer, List<Integer>> inputParams) {
        buildProject(
                game, player, projectId, payments, inputParams,
                new BuildBlueRedProjectTurn(player.getUuid(), projectId, payments, inputParams),
                game.getCurrentPhase() == 3
        );
    }

    private void buildProject(MarsGame game, Player player, int projectId, List<Payment> payments, Map<Integer, List<Integer>> inputParams, Turn turn, boolean syncTurn) {
        String errorMessage = cardValidationService.validateCard(player, game, projectId, payments, inputParams);
        if (errorMessage != null) {
            throw new IllegalStateException(errorMessage);
        }

        if (syncTurn) {
            makeSyncTurn(player, game, turn);
        } else {
            makeAsyncTurn(player, turn);
        }
    }

    public void pickExtraCardTurnAsync(Player player) {
        makeAsyncTurn(player, new PickExtraBonusSecondPhase(player.getUuid()));
    }

    public void pickExtraCardTurnSync(MarsGame game, Player player) {
        makeSyncTurn(player, game, new PickExtraBonusSecondPhase(player.getUuid()));
    }

    public void collectIncomeTurn(Player player) {
        makeAsyncTurn(player, new CollectIncomeTurn(player.getUuid(), null));
    }

    public void collectIncomeTurnSync(MarsGame game, Player player) {
        makeSyncTurn(player, game, new CollectIncomeTurn(player.getUuid(), null));
    }

    public void skipTurn(Player player) {
        makeAsyncTurn(player, new SkipTurn(player.getUuid()));
    }

    public void plantForest(MarsGame game, Player player) {
        if (player.getPlants() < paymentValidationService.forestPriceInPlants(player)) {
            throw new IllegalStateException("Not enough plants to create a forest");
        }

        makeSyncTurn(player, game, new PlantForestTurn(player.getUuid()));
    }

    public void increaseTemperature(MarsGame game, Player player) {
        if (player.getHeat() < Constants.TEMPERATURE_HEAT_COST) {
            throw new IllegalStateException("Not enough heat to raise temperature");
        }

        if (!terraformingService.canIncreaseTemperature(game)) {
            throw new IllegalStateException("Can't increase temperature anymore, already max");
        }

        makeSyncTurn(player, game, new IncreaseTemperatureTurn(player.getUuid()));
    }

    public void discardCards(MarsGame game, Player player, Turn turn, List<Integer> cards) {
        if (player.getNextTurn().getType() != TurnType.DISCARD_CARDS) {
            throw new IllegalStateException("Invalid next turn. Expected " + player.getNextTurn().getType());
        }

        DiscardCardsTurn expectedTurn = (DiscardCardsTurn) player.getNextTurn();
        if (expectedTurn.getSize() != cards.size()) {
            throw new IllegalStateException("Incorrect number of cards to discard, expected: " + expectedTurn.getSize());
        }

        if (!player.getHand().getCards().containsAll(cards)) {
            throw new IllegalStateException("Can't discard cards that you don't have");
        }

        if (expectedTurn.isOnlyFromSelectedCards()) {
            List<Integer> expectedCardsToBeRemovedFrom = expectedTurn.getCards();
            for (Integer card : cards) {
                if (!expectedCardsToBeRemovedFrom.contains(card)) {
                    throw new IllegalStateException("You can't discard cards other than from those that you received");
                }
            }
        }
        if (!(game.getCurrentPhase() == Constants.PICK_CORPORATIONS_PHASE ||
                game.getCurrentPhase() == Constants.DRAFT_CARDS_PHASE)) {
            makeSyncTurn(player, game, turn);
        } else {
            makeAsyncTurn(player, turn);
        }

    }

    public void unmiRtCorporationTurnSync(MarsGame game, Player player) {
        if (player.getMc() < 6) {
            throw new IllegalArgumentException("Not enough MC to perform the action");
        }

        makeSyncTurn(player, game, new UnmiRtTurn(player.getUuid()));
    }

    public void unmiRtCorporationTurn(MarsGame game, Player player) {
        if (player.getMc() < 6) {
            throw new IllegalArgumentException("Not enough MC to perform the action");
        }
        if (!turnTypeService.isTerminal(TurnType.UNMI_RT, game)) {
            makeSyncTurn(player, game, new UnmiRtTurn(player.getUuid()));
        } else {
            makeAsyncTurn(player, new UnmiRtTurn(player.getUuid()));
        }
    }

    public void sellCardsLastRoundTurn(Player player, List<Integer> cards) {
        if (!player.getHand().getCards().containsAll(cards)) {
            throw new IllegalStateException("Can't sell cards that you don't have");
        }

        if (player.getHand().size() - cards.size() > Constants.MAX_HAND_SIZE_LAST_ROUND) {
            throw new IllegalStateException("You need to discard at least "
                    + (player.getHand().size() - Constants.MAX_HAND_SIZE_LAST_ROUND)
                    + " cards");
        }

        makeAsyncTurn(player, new SellCardsLastRoundTurn(player.getUuid(), cards));
    }

    public void mulliganCards(MarsGame game, Player player, List<Integer> cards) {
        if (!player.getHand().getCards().containsAll(cards)) {
            throw new IllegalStateException("Can't mulligan cards that you don't have");
        }
        makeSyncTurn(player, game, new MulliganTurn(player.getUuid(), cards));
    }

    public void performBlueAction(MarsGame game, Player player, int projectId, Map<Integer, List<Integer>> inputParams) {
        String errorMessage = cardValidationService.validateBlueAction(player, game, projectId, inputParams);

        if (errorMessage != null) {
            throw new IllegalStateException(errorMessage);
        }

        makeSyncTurn(player, game, new PerformBlueActionTurn(player.getUuid(), projectId, inputParams));
    }

    public void sellAllCards(Player player, MarsGame game, List<Integer> cards) {
        if (!player.getHand().getCards().containsAll(cards)) {
            throw new IllegalStateException("Can't sell cards that you don't have");
        }
        makeSyncTurn(player, game, new SellCardsTurn(player.getUuid(), cards));
    }

    public void standardProjectTurn(MarsGame game, Player player, StandardProjectType type) {
        String validationResult = standardProjectService.validateStandardProject(game, player, type);
        if (validationResult != null) {
            throw new IllegalStateException(validationResult);
        }
        makeSyncTurn(player, game, new StandardProjectTurn(player.getUuid(), type));
    }

    public void confirmGameEnd(MarsGame game, Player player) {
        if (player.getHand().size() != 0) {
            throw new IllegalStateException("This is the last turn, sell all cards");
        }
        String validationResult = standardProjectService.validateStandardProjectAvailability(game, player);
        if (validationResult != null) {
            throw new IllegalStateException(validationResult);
        }

        makeAsyncTurn(player, new GameEndConfirmTurn(player.getUuid()));
    }

    private void makeAsyncTurn(Player player, Turn turn) {
        if (player.getNextTurn() != null && player.getNextTurn().expectedAsNextTurn()) {
            player.removeNextTurn();
        }
        player.addFirstTurn(turn);
    }

    private void makeSyncTurn(Player player, MarsGame game, Turn turn) {
        //sync update
        if (player.getNextTurn() != null && player.getNextTurn().expectedAsNextTurn()) {
            player.removeNextTurn();
        }

        boolean oxygenMaxBefore = game.getPlanet().isOxygenMax();
        boolean temperatureMaxBefore = game.getPlanet().isTemperatureMax();
        boolean oceansMaxBefore = game.getPlanet().isOceansMax();

        processTurn(turn, game);

        boolean oxygenMaxAfter = game.getPlanet().isOxygenMax();
        boolean temperatureMaxAfter = game.getPlanet().isTemperatureMax();
        boolean oceansMaxAfter = game.getPlanet().isOceansMax();

        if (game.getCurrentPhase() == 3 &&
                (!oxygenMaxBefore && oxygenMaxAfter
                        || !temperatureMaxBefore && temperatureMaxAfter
                        || !oceansMaxBefore && oceansMaxAfter)) {
            game.getPlayerUuidToPlayer()
                    .values()
                    .stream().filter(p -> !p.getUuid().equals(turn.getPlayerUuid()))
                    .filter(p -> p.getNextTurn() != null && p.getNextTurn().getType() == TurnType.SKIP_TURN)
                    .forEach(Player::removeNextTurn);
        }
    }

    private TurnResponse processTurn(Turn turn, MarsGame game) {
        if (turn == null) {
            return null;
        }

        TurnProcessor<Turn> turnProcessor = (TurnProcessor<Turn>) turnProcessors.get(turn.getType());

        return turnProcessor.processTurn(turn, game);
    }

}
