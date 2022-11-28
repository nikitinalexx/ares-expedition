package com.terraforming.ares.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.processors.turn.TurnProcessor;
import com.terraforming.ares.services.*;
import com.terraforming.ares.services.ai.helpers.AiCardActionHelper;
import com.terraforming.ares.services.ai.helpers.AiCardBuildParamsHelper;
import com.terraforming.ares.services.ai.helpers.AiPaymentService;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 27.11.2022
 */
@Service
public class AiProjectionService extends BaseProcessorService {
    private final AiTurnService aiTurnService;
    private final AiPaymentService aiPaymentService;
    private final AiCardBuildParamsHelper aiCardBuildParamsHelper;
    private final ObjectMapper objectMapper;
    private final StateFactory stateFactory;
    private final CardValidationService cardValidationService;
    private final DeepNetwork deepNetwork;
    private final PaymentValidationService paymentValidationService;
    private final CardService cardService;
    private final AiCardActionHelper aiCardActionHelper;
    private final StandardProjectService standardProjectService;

    protected AiProjectionService(TurnTypeService turnTypeService, StateFactory stateFactory, StateContextProvider stateContextProvider, List<TurnProcessor<?>> turnProcessor, AiTurnService aiTurnService, AiPaymentService aiPaymentService, AiCardBuildParamsHelper aiCardBuildParamsHelper, ObjectMapper objectMapper, CardValidationService cardValidationService, DeepNetwork deepNetwork, PaymentValidationService paymentValidationService, CardService cardService, AiCardActionHelper aiCardActionHelper, StandardProjectService standardProjectService) {
        super(turnTypeService, stateFactory, stateContextProvider, turnProcessor);
        this.aiTurnService = aiTurnService;
        this.aiPaymentService = aiPaymentService;
        this.aiCardBuildParamsHelper = aiCardBuildParamsHelper;
        this.objectMapper = objectMapper;
        this.stateFactory = stateFactory;
        this.cardValidationService = cardValidationService;
        this.deepNetwork = deepNetwork;
        this.paymentValidationService = paymentValidationService;
        this.cardService = cardService;
        this.aiCardActionHelper = aiCardActionHelper;
        this.standardProjectService = standardProjectService;
    }

    public MarsGame projectBuildCard(MarsGame game, Player player, Card card, ProjectionStrategy projectionStrategy) {
        //todo consider -3 discount in phase 1.
        game = copyMars(game);
        player = game.getPlayerByUuid(player.getUuid());

        if (projectionStrategy == ProjectionStrategy.FROM_PICK_PHASE) {
            getAnotherPlayer(game, player).setPreviousChosenPhase(null);
            if (game.getStateType() == StateType.PICK_PHASE) {
                game.getPlayerUuidToPlayer().values().forEach(
                        p -> aiTurnService.choosePhaseTurn(p, card.getColor() == CardColor.GREEN ? 1 : 2)
                );
                while (processFinalTurns(game)) {
                    stateFactory.getCurrentState(game).updateState();
                }
            }
            String errorMessage = cardValidationService.validateCard(
                    player, game, card.getId(),
                    aiPaymentService.getCardPayments(player, card),
                    aiCardBuildParamsHelper.getInputParamsForValidation(player, card)
            );
            if (errorMessage != null) {
                return null;
            }
        }

        if (card.getColor() == CardColor.GREEN) {
            aiTurnService.buildGreenProjectSync(
                    game,
                    player,
                    card.getId(),
                    aiPaymentService.getCardPayments(player, card),
                    aiCardBuildParamsHelper.getInputParamsForBuild(player, card)
            );
        } else {
            aiTurnService.buildBlueRedProjectSync(
                    game,
                    player,
                    card.getId(),
                    aiPaymentService.getCardPayments(player, card),
                    aiCardBuildParamsHelper.getInputParamsForBuild(player, card)
            );
        }

        return game;
    }

    public MarsGame projectTakeExtraCard(MarsGame game, Player player) {
        game = copyMars(game);
        player = game.getPlayerByUuid(player.getUuid());

        aiTurnService.pickExtraCardTurnSync(game, player);

        return game;
    }

    public MarsGame projectUnmiTurn(MarsGame game, Player player) {
        game = copyMars(game);
        player = game.getPlayerByUuid(player.getUuid());

        aiTurnService.unmiRtCorporationTurnSync(game, player);

        return game;
    }

    public MarsGame projectPhaseThree(MarsGame game, Player player, ProjectionStrategy projectionStrategy) {
        game = copyMars(game);
        player = game.getPlayerByUuid(player.getUuid());

        final Player anotherPlayer = getAnotherPlayer(game, player);

        if (projectionStrategy == ProjectionStrategy.FROM_PICK_PHASE) {
            anotherPlayer.setPreviousChosenPhase(null);
            if (game.getStateType() == StateType.PICK_PHASE) {
                game.getPlayerUuidToPlayer().values().forEach(
                        p -> aiTurnService.choosePhaseTurn(p, p.getUuid().equals(anotherPlayer.getUuid()) ? 4 : 3)
                );
                while (processFinalTurns(game)) {
                    stateFactory.getCurrentState(game).updateState();
                }
            }
        }

        game.getPlayerUuidToPlayer().put(anotherPlayer.getUuid(), projectThirdPhasePlayer(game, anotherPlayer));
        game.getPlayerUuidToPlayer().put(player.getUuid(), projectThirdPhasePlayer(game, player));

        return game;
    }

    public MarsGame projectPhaseFour(MarsGame game, Player player, ProjectionStrategy projectionStrategy) {
        game = copyMars(game);
        player = game.getPlayerByUuid(player.getUuid());

        final Player anotherPlayer = getAnotherPlayer(game, player);

        if (projectionStrategy == ProjectionStrategy.FROM_PICK_PHASE) {
            anotherPlayer.setPreviousChosenPhase(null);
            if (game.getStateType() == StateType.PICK_PHASE) {
                game.getPlayerUuidToPlayer().values().forEach(
                        p -> aiTurnService.choosePhaseTurn(p, p.getUuid().equals(anotherPlayer.getUuid()) ? 5 : 4)
                );
                while (processFinalTurns(game)) {
                    stateFactory.getCurrentState(game).updateState();
                }
            }
        }

        aiTurnService.collectIncomeTurnSync(game, player);
        aiTurnService.collectIncomeTurnSync(game, anotherPlayer);

        return game;
    }

    public static final int NO_BEST_TURN = -100;
    public static final int TEMPERATURE_TURN = -99;
    public static final int FOREST_TURN = -98;
    public static final int STANDARD_TEMPERATURE = -97;
    public static final int STANDARD_FOREST = -96;
    public static final int STANDARD_OCEAN = -95;


    public boolean doBestThirdPhaseTurn(List<TurnType> possibleTurns, MarsGame originalGame, Player originalPlayer) {
        float bestState = deepNetwork.testState(originalGame, originalPlayer);

        MarsGame game = copyMars(originalGame);
        Player player = game.getPlayerByUuid(originalPlayer.getUuid());

        int bestTurn = NO_BEST_TURN;

        if (player.getHeat() >= Constants.TEMPERATURE_HEAT_COST && !game.getPlanetAtTheStartOfThePhase().isTemperatureMax()) {
            MarsGame copyMars = copyMars(game);
            Player copyPlayer = copyMars.getPlayerByUuid(player.getUuid());
            aiTurnService.increaseTemperature(copyMars, copyPlayer);

            float newState = deepNetwork.testState(copyMars, copyPlayer);
            if (newState > bestState) {
                bestTurn = TEMPERATURE_TURN;
            }
        }

        if (player.getPlants() >= paymentValidationService.forestPriceInPlants(player)) {
            MarsGame copyMars = copyMars(game);
            Player copyPlayer = copyMars.getPlayerByUuid(player.getUuid());
            aiTurnService.plantForest(copyMars, copyPlayer);

            float newState = deepNetwork.testState(copyMars, copyPlayer);
            if (newState > bestState) {
                bestTurn = FOREST_TURN;
            }
        }


        if (!game.getPlanetAtTheStartOfThePhase().isTemperatureMax()) {
            String validationResult = standardProjectService.validateStandardProject(game, player, StandardProjectType.TEMPERATURE);
            if (validationResult == null) {
                MarsGame copyMars = copyMars(game);
                Player copyPlayer = copyMars.getPlayerByUuid(player.getUuid());
                aiTurnService.standardProjectTurn(copyMars, copyPlayer, StandardProjectType.TEMPERATURE);

                float newState = deepNetwork.testState(copyMars, copyPlayer);
                if (newState > bestState) {
                    bestTurn = STANDARD_TEMPERATURE;
                }
            }
        }

        if (!game.getPlanetAtTheStartOfThePhase().isOceansMax()) {
            String validationResult = standardProjectService.validateStandardProject(game, player, StandardProjectType.OCEAN);
            if (validationResult == null) {
                MarsGame copyMars = copyMars(game);
                Player copyPlayer = copyMars.getPlayerByUuid(player.getUuid());
                aiTurnService.standardProjectTurn(copyMars, copyPlayer, StandardProjectType.OCEAN);

                float newState = deepNetwork.testState(copyMars, copyPlayer);
                if (newState > bestState) {
                    bestTurn = STANDARD_OCEAN;
                }
            }
        }

        String validationResult = standardProjectService.validateStandardProject(game, player, StandardProjectType.FOREST);
        if (validationResult == null) {
            MarsGame copyMars = copyMars(game);
            Player copyPlayer = copyMars.getPlayerByUuid(player.getUuid());
            aiTurnService.standardProjectTurn(copyMars, copyPlayer, StandardProjectType.FOREST);

            float newState = deepNetwork.testState(copyMars, copyPlayer);
            if (newState > bestState) {
                bestTurn = STANDARD_FOREST;
            }
        }

        Deck activatedBlueCards = player.getActivatedBlueCards();

        boolean mayActivateTwice = player.getChosenPhase() == 3 && !player.isActivatedBlueActionTwice();

        List<Card> notUsedBlueCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(Card::isActiveCard)
                .filter(c -> !activatedBlueCards.containsCard(c.getId()) || mayActivateTwice)
                .collect(Collectors.toList());

        List<Integer> bestActionParameters = List.of();

        for (Card notUsedBlueCard : notUsedBlueCards) {
            MarsGame copyMars = copyMars(game);
            Player copyPlayer = copyMars.getPlayerByUuid(player.getUuid());

            if (aiCardActionHelper.validateAction(copyMars, copyPlayer, notUsedBlueCard) == null) {
                List<Integer> actionParameters = aiCardActionHelper.getActionInputParams(copyMars, copyPlayer, notUsedBlueCard);
                aiTurnService.performBlueAction(
                        copyMars,
                        copyPlayer,
                        notUsedBlueCard.getId(),
                        actionParameters
                );

                float newState = deepNetwork.testState(copyMars, copyPlayer);
                if (newState > bestState) {
                    bestTurn = notUsedBlueCard.getId();
                    bestActionParameters = actionParameters;
                }
            }
        }

        if (bestTurn == NO_BEST_TURN) {
            return false;
        } else if (bestTurn == TEMPERATURE_TURN) {
            aiTurnService.increaseTemperature(originalGame, originalPlayer);
            return true;
        } else if (bestTurn == FOREST_TURN) {
            aiTurnService.plantForest(originalGame, originalPlayer);
            return true;
        } else if (bestTurn == STANDARD_OCEAN) {
            aiTurnService.standardProjectTurn(originalGame, originalPlayer, StandardProjectType.OCEAN);
            return true;
        } else if (bestTurn == STANDARD_FOREST) {
            aiTurnService.standardProjectTurn(originalGame, originalPlayer, StandardProjectType.FOREST);
            return true;
        } else if (bestTurn == STANDARD_TEMPERATURE) {
            aiTurnService.standardProjectTurn(originalGame, originalPlayer, StandardProjectType.TEMPERATURE);
            return true;
        } else {
            aiTurnService.performBlueAction(
                    originalGame,
                    originalPlayer,
                    bestTurn,
                    bestActionParameters
            );
            return true;
        }
    }

    private Player projectThirdPhasePlayer(MarsGame game, Player player) {
        float bestState = deepNetwork.testState(game, player);

        game = copyMars(game);
        player = game.getPlayerByUuid(player.getUuid());

        int bestTurn = -1;

        if (player.getHeat() >= Constants.TEMPERATURE_HEAT_COST && !game.getPlanetAtTheStartOfThePhase().isTemperatureMax()) {
            MarsGame copyMars = copyMars(game);
            Player copyPlayer = copyMars.getPlayerByUuid(player.getUuid());
            aiTurnService.increaseTemperature(copyMars, copyPlayer);

            float newState = deepNetwork.testState(copyMars, copyPlayer);
            if (newState > bestState) {
                bestTurn = 0;
            }
        }

        if (player.getPlants() >= paymentValidationService.forestPriceInPlants(player)) {
            MarsGame copyMars = copyMars(game);
            Player copyPlayer = copyMars.getPlayerByUuid(player.getUuid());
            aiTurnService.plantForest(copyMars, copyPlayer);

            float newState = deepNetwork.testState(copyMars, copyPlayer);
            if (newState > bestState) {
                bestTurn = 1;
            }
        }

        Deck activatedBlueCards = player.getActivatedBlueCards();

        boolean mayActivateTwice = player.getChosenPhase() == 3 && !player.isActivatedBlueActionTwice();

        List<Card> notUsedBlueCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(Card::isActiveCard)
                .filter(c -> !activatedBlueCards.containsCard(c.getId()) || mayActivateTwice)
                .collect(Collectors.toList());

        for (Card notUsedBlueCard : notUsedBlueCards) {
            MarsGame copyMars = copyMars(game);
            Player copyPlayer = copyMars.getPlayerByUuid(player.getUuid());

            if (aiCardActionHelper.validateAction(copyMars, copyPlayer, notUsedBlueCard) == null) {
                aiTurnService.performBlueAction(
                        copyMars,
                        copyPlayer,
                        notUsedBlueCard.getId(),
                        aiCardActionHelper.getActionInputParams(copyMars, copyPlayer, notUsedBlueCard)
                );

                float newState = deepNetwork.testState(copyMars, copyPlayer);
                if (newState > bestState) {
                    bestTurn = notUsedBlueCard.getId();
                }
            }
        }

        if (bestTurn == -1) {
            return player;
        } else if (bestTurn == 0) {
            MarsGame copyMars = copyMars(game);
            Player copyPlayer = copyMars.getPlayerByUuid(player.getUuid());
            aiTurnService.increaseTemperature(copyMars, copyPlayer);

            game = copyMars;
            player = copyPlayer;
        } else if (bestTurn == 1) {
            MarsGame copyMars = copyMars(game);
            Player copyPlayer = copyMars.getPlayerByUuid(player.getUuid());
            aiTurnService.plantForest(copyMars, copyPlayer);

            game = copyMars;
            player = copyPlayer;
        } else {
            MarsGame copyMars = copyMars(game);
            Player copyPlayer = copyMars.getPlayerByUuid(player.getUuid());
            aiTurnService.performBlueAction(
                    copyMars,
                    copyPlayer,
                    bestTurn,
                    aiCardActionHelper.getActionInputParams(copyMars, copyPlayer, cardService.getCard(bestTurn))
            );

            game = copyMars;
            player = copyPlayer;
        }

        return projectThirdPhasePlayer(game, player);
    }

    private Player getAnotherPlayer(MarsGame game, Player player) {
        return game.getPlayerUuidToPlayer().values().stream().filter(p -> !p.getUuid().equals(player.getUuid()))
                .findFirst().orElseThrow(() -> new IllegalStateException("Another player not found"));
    }

    private MarsGame copyMars(MarsGame game) {
        return safeDeserialize(safeSerialize(game));
    }

    private String safeSerialize(MarsGame marsGame) {
        try {
            return objectMapper.writeValueAsString(marsGame);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializing the game");
        }
    }

    private MarsGame safeDeserialize(String gameJson) {
        try {
            return objectMapper.readValue(gameJson, MarsGame.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error deserializing the game");
        }
    }
}
