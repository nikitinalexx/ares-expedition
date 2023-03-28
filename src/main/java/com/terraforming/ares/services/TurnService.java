package com.terraforming.ares.services;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.request.ChooseCorporationRequest;
import com.terraforming.ares.model.request.CrisisDummyHandChoiceRequest;
import com.terraforming.ares.model.request.CrysisChoiceRequest;
import com.terraforming.ares.model.request.DiscardCardsRequest;
import com.terraforming.ares.model.turn.*;
import com.terraforming.ares.repositories.caching.CachingGameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class TurnService {
    private static final Predicate<MarsGame> ASYNC_TURN = game -> false;
    private static final Predicate<MarsGame> SYNC_TURN = game -> true;
    private static final String RESOLVE_OCEAN_DETRIMENT_TURN_ERROR_MESSAGE = "Invalid next turn. Input with Temperature or Oxygen expected";

    private final StateFactory stateFactory;
    private final CachingGameRepository gameRepository;
    private final GameProcessorService gameProcessorService;
    private final CardValidationService cardValidationService;
    private final TerraformingService terraformingService;
    private final StandardProjectService standardProjectService;
    private final CardService cardService;
    private final PaymentValidationService paymentValidationService;
    private final StateContextProvider stateContextProvider;
    private final TurnTypeService turnTypeService;
    private final WinPointsService winPointsService;
    private final CrisisDetrimentService crisisDetrimentService;

    public void chooseCorporationTurn(ChooseCorporationRequest chooseCorporationRequest) {
        String playerUuid = chooseCorporationRequest.getPlayerUuid();
        Integer corporationCardId = chooseCorporationRequest.getCorporationId();
        performTurn(
                new CorporationChoiceTurn(playerUuid, corporationCardId, chooseCorporationRequest.getInputParams()),
                playerUuid,
                game -> {
                    if (!game.getPlayerByUuid(playerUuid).getCorporations().containsCard(corporationCardId)) {
                        return "Can't pick corporation that is not in your choice deck";
                    }
                    return cardValidationService.validateCorporation(
                            game.getPlayerByUuid(playerUuid),
                            game,
                            corporationCardId,
                            chooseCorporationRequest.getInputParams()
                    );
                },
                ASYNC_TURN
        );
    }

    public void choosePhaseTurn(String playerUuid, int phaseId) {
        performTurn(
                new PhaseChoiceTurn(playerUuid, phaseId),
                playerUuid,
                game -> {
                    if (phaseId < 1 || phaseId > 5) {
                        return "Phase is not within [1..5] range";
                    }

                    Player player = game.getPlayerByUuid(playerUuid);
                    if (player.getPreviousChosenPhase() != null && player.getPreviousChosenPhase() == phaseId) {
                        return "This phase already picked in previous round";
                    }

                    if (game.isCrysis()
                            && game.getCrysisData().getForbiddenPhases().getOrDefault(player.getUuid(), -1).equals(phaseId)) {
                        return "This phase is forbidden by Crysis this turn";
                    }

                    if (!crisisDetrimentService.canPlayFirstPhase(game) && phaseId == 1) {
                        return "Oxygen Crisis makes you unable to resolve this phase";
                    }

                    return null;
                },
                ASYNC_TURN
        );
    }

    public void collectIncomeTurn(String playerUuid, Integer cardId) {
        performTurn(
                new CollectIncomeTurn(playerUuid, cardId),
                playerUuid,
                game -> {
                    Player player = game.getPlayerByUuid(playerUuid);

                    if (cardId != null && !player.getPlayed().containsCard(cardId)) {
                        return "Can't collect income from Card you don't own";
                    }

                    if (cardId != null && (player.getChosenPhase() != 4 || !player.hasPhaseUpgrade(Constants.PHASE_4_UPGRADE_DOUBLE_PRODUCE))) {
                        return "Not allowed to collect income twice";
                    }

                    return null;
                },
                ASYNC_TURN
        );
    }

    public void skipTurn(String playerUuid) {
        performTurn(new SkipTurn(playerUuid), playerUuid, game -> null, ASYNC_TURN);
    }

    public void confirmGameEnd(String playerUuid) {
        performTurn(new GameEndConfirmTurn(playerUuid), playerUuid, game -> {
            Player player = game.getPlayerUuidToPlayer().get(playerUuid);
            if (player.getHand().size() != 0) {
                return "This is the last turn, sell all cards";
            }

            return standardProjectService.validateStandardProjectAvailability(game, player);

        }, ASYNC_TURN);
    }

    public void pickExtraBonusTurn(String playerUuid) {
        performTurn(new PickExtraBonusSecondPhase(playerUuid), playerUuid, game -> null, ASYNC_TURN);
    }

    public void crysisImmediateChoiceTurn(CrysisChoiceRequest crysisChoiceRequest) {
        performTurn(
                new CrysisCardImmediateChoiceTurn(crysisChoiceRequest.getPlayerUuid(), crysisChoiceRequest.getCardId(), crysisChoiceRequest.getInputParams(), false),
                crysisChoiceRequest.getPlayerUuid(),
                game -> {
                    final Player player = game.getPlayerUuidToPlayer().get(crysisChoiceRequest.getPlayerUuid());

                    if (player.getNextTurn().getType() != TurnType.RESOLVE_IMMEDIATE_WITH_CHOICE) {
                        return "Invalid next turn. Expected " + player.getNextTurn().getType();
                    }

                    CrysisCardImmediateChoiceTurn expectedTurn = (CrysisCardImmediateChoiceTurn) player.getNextTurn();

                    if (!expectedTurn.getCard().equals(crysisChoiceRequest.getCardId())) {
                        return "Invalid next turn. Expected card " + expectedTurn.getCard();
                    }

                    return cardValidationService.validateCrisisImmediateEffect(game, player, expectedTurn.getCard(), crysisChoiceRequest.getInputParams());
                },
                ASYNC_TURN);
    }

    public void crysisPersistentChoiceTurn(CrysisChoiceRequest crysisChoiceRequest) {
        performTurn(
                new CrysisCardPersistentChoiceTurn(crysisChoiceRequest.getPlayerUuid(), crysisChoiceRequest.getCardId(), crysisChoiceRequest.getInputParams(), false),
                crysisChoiceRequest.getPlayerUuid(),
                game -> {
                    final Player player = game.getPlayerUuidToPlayer().get(crysisChoiceRequest.getPlayerUuid());

                    if (player.getNextTurn().getType() != TurnType.RESOLVE_PERSISTENT_WITH_CHOICE) {
                        return "Invalid next turn. Expected " + player.getNextTurn().getType();
                    }

                    CrysisCardPersistentChoiceTurn expectedTurn = (CrysisCardPersistentChoiceTurn) player.getNextTurn();

                    if (!expectedTurn.getCard().equals(crysisChoiceRequest.getCardId())) {
                        return "Invalid next turn. Expected card " + expectedTurn.getCard();
                    }

                    return cardValidationService.validateCrisisPersistentEffect(game, player, expectedTurn.getCard(), crysisChoiceRequest.getInputParams());
                },
                ASYNC_TURN);
    }

    public void resolveOceanDetrimentTurn(CrysisChoiceRequest request) {
        performTurn(
                new ResolveOceanDetrimentTurn(request.getPlayerUuid(), request.getInputParams(), false),
                request.getPlayerUuid(),
                game -> {

                    final Map<Integer, List<Integer>> inputParams = request.getInputParams();
                    if (CollectionUtils.isEmpty(inputParams)) {
                        return RESOLVE_OCEAN_DETRIMENT_TURN_ERROR_MESSAGE;
                    }

                    final List<Integer> choiceInput = inputParams.get(InputFlag.CRYSIS_INPUT_FLAG.getId());
                    if (CollectionUtils.isEmpty(choiceInput)) {
                        return RESOLVE_OCEAN_DETRIMENT_TURN_ERROR_MESSAGE;
                    }

                    int choice = choiceInput.get(0);
                    if (choice != InputFlag.CRYSIS_INPUT_OPTION_1.getId() && choice != InputFlag.CRYSIS_INPUT_OPTION_2.getId()) {
                        return RESOLVE_OCEAN_DETRIMENT_TURN_ERROR_MESSAGE;
                    }

                    return null;
                },
                ASYNC_TURN);
    }

    public void draftCards(String playerUuid) {
        performTurn(
                new DraftCardsTurn(playerUuid),
                playerUuid,
                game -> null,
                SYNC_TURN
        );
    }

    public void crysisPersistentAllTurn(String playerUuid) {
        performTurn(
                new CrysisPersistentAllTurn(playerUuid),
                playerUuid,
                game -> null,
                ASYNC_TURN
        );
    }

    public void crysisImmediateAllTurn(String playerUuid) {
        performTurn(
                new CrysisImmediateAllTurn(playerUuid, false),
                playerUuid,
                game -> null,
                ASYNC_TURN
        );
    }

    public void crysisDummyHandChoiceTurn(CrisisDummyHandChoiceRequest request) {
        performTurn(
                new CrisisDummyHandChoiceTurn(request.getPlayerUuid(), request.getChoiceOptions()),
                request.getPlayerUuid(),
                game -> {
                    final List<String> choiceOptions = request.getChoiceOptions();
                    if (CollectionUtils.isEmpty(choiceOptions) || choiceOptions.size() != 2) {
                        return "Invalid number of phases chosen";
                    }

                    final List<CrisisDummyCard> crisisDummyExpectedHand = game.getCrysisData().getCurrentDummyCards();

                    if (!(crisisDummyExpectedHand.get(0).validCardPattern(choiceOptions.get(0))
                            && crisisDummyExpectedHand.get(1).validCardPattern(choiceOptions.get(1))
                            || crisisDummyExpectedHand.get(0).validCardPattern(choiceOptions.get(1))
                            && crisisDummyExpectedHand.get(1).validCardPattern(choiceOptions.get(0)))) {
                        return "Invalid phases provided";
                    }

                    return null;
                },
                ASYNC_TURN
        );
    }

    public void plantForest(String playerUuid) {
        performTurn(
                new PlantForestTurn(playerUuid),
                playerUuid,
                game -> {
                    Player player = game.getPlayerByUuid(playerUuid);

                    if (player.getPlants() < paymentValidationService.forestPriceInPlants(player)) {
                        return "Not enough plants to create a forest";
                    }

                    return null;
                },
                SYNC_TURN
        );
    }

    public void increaseTemperature(String playerUuid) {
        performTurn(
                new IncreaseTemperatureTurn(playerUuid),
                playerUuid,
                game -> {
                    Player player = game.getPlayerByUuid(playerUuid);

                    if (player.getHeat() < Constants.TEMPERATURE_HEAT_COST) {
                        return "Not enough heat to raise temperature";
                    }

                    if (!terraformingService.canIncreaseTemperature(game)) {
                        return "Can't increase temperature anymore, already max";
                    }

                    return null;
                },
                SYNC_TURN
        );
    }

    public void plantsIntoCrisisToken(String playerUuid) {
        performTurn(
                new PlantsToCrisisTokenTurn(playerUuid),
                playerUuid,
                game -> {
                    Player player = game.getPlayerByUuid(playerUuid);

                    if (player.getPlants() < Constants.CRISIS_TOKEN_PLANTS_COST) {
                        return "Not enough plants to remove crisis token";
                    }

                    if (game.getCrysisData()
                            .getOpenedCards()
                            .stream()
                            .map(cardService::getCrysisCard)
                            .map(CrysisCard::getActiveCardAction)
                            .noneMatch(action -> action == CrysisActiveCardAction.PLANTS_INTO_TOKENS)) {
                        return "Token was already removed";
                    }

                    return null;
                },
                SYNC_TURN
        );
    }

    public void heatIntoCrisisToken(String playerUuid) {
        performTurn(
                new HeatToCrisisTokenTurn(playerUuid),
                playerUuid,
                game -> {
                    Player player = game.getPlayerByUuid(playerUuid);

                    if (player.getHeat() < Constants.CRISIS_TOKEN_HEAT_COST) {
                        return "Not enough heat to remove crisis token";
                    }

                    if (game.getCrysisData()
                            .getOpenedCards()
                            .stream()
                            .map(cardService::getCrysisCard)
                            .map(CrysisCard::getActiveCardAction)
                            .noneMatch(action -> action == CrysisActiveCardAction.HEAT_INTO_TOKENS)) {
                        return "Token was already removed";
                    }

                    return null;
                },
                SYNC_TURN
        );
    }

    public void cardsIntoCrisisToken(DiscardCardsRequest request) {
        performTurn(
                new CardsToCrisisTokenTurn(request.getPlayerUuid(), request.getCards()),
                request.getPlayerUuid(),
                game -> {
                    Player player = game.getPlayerByUuid(request.getPlayerUuid());

                    if (CollectionUtils.isEmpty(request.getCards())
                            || request.getCards().stream().anyMatch(cardId -> !player.getHand().containsCard(cardId))
                            || new HashSet<>(request.getCards()).size() != 3) {
                        return "You need to discard exactly 3 cards from your hand";
                    }

                    if (game.getCrysisData()
                            .getOpenedCards()
                            .stream()
                            .map(cardService::getCrysisCard)
                            .map(CrysisCard::getActiveCardAction)
                            .noneMatch(action -> action == CrysisActiveCardAction.CARDS_INTO_TOKENS)) {
                        return "Token was already removed";
                    }

                    return null;
                },
                SYNC_TURN
        );
    }

    public void standardProjectTurn(String playerUuid, StandardProjectType type) {
        performTurn(
                new StandardProjectTurn(playerUuid, type),
                playerUuid,
                game -> {
                    Player player = game.getPlayerByUuid(playerUuid);

                    return standardProjectService.validateStandardProject(game, player, type);
                },
                SYNC_TURN
        );
    }

    public void exchangeHeatRequest(String playerUuid, int value) {
        performTurn(
                new ExchangeHeatTurn(playerUuid, value),
                playerUuid,
                game -> {
                    Player player = game.getPlayerByUuid(playerUuid);

                    if (value <= 0) {
                        return "Incorrect heat value provided";
                    }

                    if (player.getHeat() < value) {
                        return "Not enough heat to perform exchange";
                    }

                    if (player.getPlayed()
                            .getCards()
                            .stream()
                            .map(cardService::getCard)
                            .map(Card::getCardMetadata)
                            .filter(Objects::nonNull)
                            .map(CardMetadata::getCardAction)
                            .filter(Objects::nonNull)
                            .noneMatch(CardAction.HELION_CORPORATION::equals)
                    ) {
                        return "Only Helion may perform heat exchange";
                    }

                    return null;
                },
                SYNC_TURN
        );
    }

    public TurnResponse sellCards(String playerUuid, List<Integer> cards) {
        return performTurn(
                new SellCardsTurn(playerUuid, cards),
                playerUuid,
                game -> {
                    if (!game.getPlayerByUuid(playerUuid).getHand().getCards().containsAll(cards)) {
                        return "Can't sell cards that you don't have";
                    }

                    return null;
                },
                SYNC_TURN
        );
    }

    public TurnResponse crisisVpToTokenTurn(String playerUuid, List<Integer> cards) {
        return performTurn(
                new CrisisVpToTokenTurn(playerUuid, cards),
                playerUuid,
                game -> {
                    if (winPointsService.countWinPoints(game.getPlayerByUuid(playerUuid), game) < 2) {
                        return "No more Victory Points left to use";
                    }

                    if (CollectionUtils.isEmpty(cards) || cards.size() != 1) {
                        return "You may only select 1 card";
                    }

                    if (!game.getCrysisData().getOpenedCards().contains(cards.get(0))) {
                        return "The token was already removed";
                    }

                    return null;
                },
                SYNC_TURN
        );
    }

    public TurnResponse sellVp(String playerUuid) {
        return performTurn(
                new SellVpTurn(playerUuid),
                playerUuid,
                game -> {
                    if (!game.isCrysis()
                            || game.getCrysisData().getOpenedCards()
                            .stream()
                            .map(cardService::getCrysisCard)
                            .noneMatch(CrysisCard::endGameCard)) {
                        return "You can't sell Victory Points without a special Crisis Card in play";
                    }

                    if (winPointsService.countWinPoints(game.getPlayerByUuid(playerUuid), game) <= 0) {
                        return "No more Victory Points left to sell";
                    }

                    return null;
                },
                SYNC_TURN
        );
    }

    public TurnResponse mulliganCards(String playerUuid, List<Integer> cards) {
        return performTurn(
                new MulliganTurn(playerUuid, cards),
                playerUuid,
                game -> {
                    if (!game.getPlayerByUuid(playerUuid).getHand().getCards().containsAll(cards)) {
                        return "Can't mulligan cards that you don't have";
                    }

                    return null;
                },
                SYNC_TURN
        );
    }

    public void sellCardsLastRoundTurn(String playerUuid, List<Integer> cards) {
        performTurn(
                new SellCardsLastRoundTurn(playerUuid, cards),
                playerUuid,
                game -> {
                    Player player = game.getPlayerByUuid(playerUuid);

                    if (!player.getHand().getCards().containsAll(cards)) {
                        return "Can't sell cards that you don't have";
                    }

                    if (player.getHand().size() - cards.size() > Constants.MAX_HAND_SIZE_LAST_ROUND) {
                        return "You need to discard at least "
                                + (player.getHand().size() - Constants.MAX_HAND_SIZE_LAST_ROUND)
                                + " cards";
                    }

                    return null;
                },
                ASYNC_TURN
        );
    }

    public TurnResponse discardCards(Turn turn, String playerUuid, List<Integer> cards) {
        Function<MarsGame, String> verifier = game -> {
            Player player = game.getPlayerByUuid(playerUuid);

            if (player.getNextTurn().getType() != TurnType.DISCARD_CARDS) {
                return "Invalid next turn. Expected " + player.getNextTurn().getType();
            }

            DiscardCardsTurn expectedTurn = (DiscardCardsTurn) player.getNextTurn();
            if (expectedTurn.getSize() != cards.size()) {
                return "Incorrect number of cards to discard, expected: " + expectedTurn.getSize();
            }

            if (!player.getHand().getCards().containsAll(cards)) {
                return "Can't discard cards that you don't have";
            }

            if (expectedTurn.isOnlyFromSelectedCards()) {
                List<Integer> expectedCardsToBeRemovedFrom = expectedTurn.getCards();
                for (Integer card : cards) {
                    if (!expectedCardsToBeRemovedFrom.contains(card)) {
                        return "You can't discard cards other than from those that you received";
                    }
                }
            }

            return null;
        };

        return performTurn(turn, playerUuid, verifier, game -> !turnTypeService.isTerminal(turn.getType(), game));
    }

    public TurnResponse unmiRtCorporationTurn(String playerUuid) {
        return performTurn(
                new UnmiRtTurn(playerUuid),
                playerUuid,
                game -> {
                    Player player = game.getPlayerByUuid(playerUuid);

                    if (player.getMc() < 6) {
                        return "Not enough MC to perform the action";
                    }

                    return null;
                },
                game -> !turnTypeService.isTerminal(TurnType.UNMI_RT, game)
        );
    }

    public TurnResponse buildGreenProjectCard(String playerUuid, int projectId, List<Payment> payments, Map<Integer, List<Integer>> inputParams) {
        return performTurn(
                new BuildGreenProjectTurn(playerUuid, projectId, payments, inputParams, false),
                playerUuid,
                game -> {
                    Player player = game.getPlayerByUuid(playerUuid);

                    return cardValidationService.validateCard(player, game, projectId, payments, inputParams);
                },
                game -> game.getCurrentPhase() == 3
        );
    }

    public TurnResponse buildBlueRedProjectCard(String playerUuid, int projectId, List<Payment> payments, Map<Integer, List<Integer>> inputParams) {
        return performTurn(
                new BuildBlueRedProjectTurn(playerUuid, projectId, payments, inputParams, false),
                playerUuid,
                game -> {
                    Player player = game.getPlayerByUuid(playerUuid);

                    return cardValidationService.validateCard(player, game, projectId, payments, inputParams);
                },
                game -> game.getCurrentPhase() == 3
        );
    }

    public TurnResponse performBlueAction(String playerUuid, int projectId, Map<Integer, List<Integer>> inputParams) {
        return performTurn(
                new PerformBlueActionTurn(playerUuid, projectId, inputParams),
                playerUuid,
                game -> {
                    Player player = game.getPlayerByUuid(playerUuid);

                    return cardValidationService.validateBlueAction(player, game, projectId, inputParams);
                },
                SYNC_TURN
        );
    }

    private TurnResponse performTurn(Turn turn,
                                     String playerUuid,
                                     Function<MarsGame, String> turnSpecificValidations,
                                     Predicate<MarsGame> syncTurnDecider) {
        long gameId = gameRepository.getGameIdByPlayerUuid(playerUuid);

        GameUpdateResult<TurnResponse> updateResult = gameProcessorService.performTurn(
                gameId,
                turn,
                playerUuid,
                game -> {
                    if (!stateFactory.getCurrentState(game).getPossibleTurns(stateContextProvider.createStateContext(playerUuid)).contains(turn.getType())) {
                        return "Incorrect game state for a turn " + turn.getType();
                    }

                    return turnSpecificValidations.apply(game);
                },
                syncTurnDecider
        );

        if (updateResult.finishedWithError()) {
            throw new IllegalStateException(updateResult.getError());
        }

        return updateResult.getResult();
    }

    public void pushGame(long gameId) {
        gameProcessorService.registerAsyncGameUpdate(gameId);
    }

}
