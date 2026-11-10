package com.terraforming.ares.services;

import com.terraforming.ares.mars.CrysisData;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.model.turn.*;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.dl4j.PolicyPrediction;
import com.terraforming.ares.services.policyai.GameArenaContext;
import com.terraforming.ares.services.policyai.dto.GameRecordArena;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.encoder.PolicyTableAndHandEncoder;
import com.terraforming.ares.services.policyai.service.PolicyActionSelector;
import com.terraforming.ares.services.policyai.service.PolicyLogitDeltaHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 27.02.2023
 */
@Service
@RequiredArgsConstructor
public class StateTransitionService {
    private final CardService cardService;
    private final ShuffleService shuffleService;
    private final CrisisDetrimentService crisisDetrimentService;
    private final WinPointsService winPointsService;
    private final MarsContextProvider contextProvider;
    private final PolicyActionSelector policyActionSelector;
    private final PolicyTableAndHandEncoder encoder;

    public void performStateTransferIntoResolveDetrimentTokens(MarsGame game) {
        if (game.getStateType() == StateType.GAME_END) {
            return;
        }

        final MarsContext context = contextProvider.provide(game);

        final Planet planet = game.getPlanet();
        if (planet.getOxygenColor() == ParameterColor.P
                || planet.getTemperatureColor() == ParameterColor.P
                || planet.isValidOcean(ParameterColor.P)
                || game.getCrysisData().getCrysisCards().getCards().isEmpty()) {
            game.setStateType(StateType.GAME_END, context,
                    game.getCrysisData().getCrysisCards().getCards().isEmpty()
                            ? "Crisis deck became empty"
                            : "One of the global parameters became Purple");
            return;
        }

        crisisDetrimentService.updateDetrimentTokens(game);

        if (crisisDetrimentService.detrimentFromOceanIsPresent(game)) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());

            final Player player = players.get(random.nextInt(players.size()));
            player.addNextTurn(new ResolveOceanDetrimentTurn(player.getUuid(), Map.of(), true));

            game.setStateTypeWithFinalize(StateType.RESOLVE_OCEAN_DETRIMENT, context);
        } else {
            performStateTransferIntoResolveCrysis(game);
        }
    }

    public void performStateTransferIntoResolveCrysis(MarsGame game) {
        if (game.getStateType() == StateType.GAME_END) {
            return;
        }

        final MarsContext context = contextProvider.provide(game);

        final List<CrysisCard> crysisCards = game.getCrysisData()
                .getOpenedCards()
                .stream()
                .map(cardService::getCrysisCard)
                .collect(Collectors.toList());

        if (crysisCards.stream().anyMatch(card -> !card.persistentEffectRequiresChoice()
                && card.cardAction() != CrysisCardAction.PLAY_AND_DISCARD_CRYSIS)) {
            game.setStateTypeWithFinalize(StateType.RESOLVE_PERSISTENT_ALL, context);
        } else {
            performStateTransferIntoResolveCrysisWithChoice(game);
        }
    }


    public void performStateTransferIntoCrisisDrawDummyHand(MarsGame game) {
        if (game.getStateType() == StateType.GAME_END) {
            return;
        }

        final CrysisData crysisData = game.getCrysisData();

        List<CrisisDummyCard> result = new ArrayList<>();

        List<CrisisDummyCard> availableCards = new ArrayList<>(CrisisDummyCard.ALL_SOLO_DUMMY_CARDS);
        if (!CollectionUtils.isEmpty(crysisData.getUsedDummyCards())) {
            availableCards.removeAll(crysisData.getUsedDummyCards());
        }

        shuffleService.shuffle(availableCards);

        if (availableCards.isEmpty()) {
            availableCards = new ArrayList<>(CrisisDummyCard.ALL_SOLO_DUMMY_CARDS);
            shuffleService.shuffle(availableCards);
            crysisData.setUsedDummyCards(new ArrayList<>());
        } else if (availableCards.size() == 1) {
            CrisisDummyCard availableCard = availableCards.get(0);
            availableCards = new ArrayList<>(CrisisDummyCard.ALL_SOLO_DUMMY_CARDS);
            shuffleService.shuffle(availableCards);
            availableCards.remove(availableCard);
            crysisData.setUsedDummyCards(new ArrayList<>());
            result.add(availableCard);
        }

        for (int i = result.size(); i < 2; i++) {
            result.add(availableCards.get(0));
            availableCards.remove(0);
        }

        crysisData.getUsedDummyCards().addAll(result);
        crysisData.setCurrentDummyCards(result);
        crysisData.setChosenDummyPhases(List.of());

        game.setStateTypeWithFinalize(StateType.CRISIS_DRAW_DUMMY_HAND, contextProvider.provide(game));
    }

    public void performStateTransferIntoResolveCrysisWithChoice(MarsGame game) {
        if (game.getStateType() == StateType.GAME_END) {
            return;
        }

        final List<CrysisCard> crysisCards = game.getCrysisData()
                .getOpenedCards()
                .stream()
                .map(cardService::getCrysisCard)
                .collect(Collectors.toList());

        if (crysisCards.stream().anyMatch(CrysisCard::persistentEffectRequiresChoice)) {
            List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
            shuffleService.shuffle(players);

            final List<CrysisCard> cardsWithChoice = crysisCards.stream()
                    .filter(CrysisCard::persistentEffectRequiresChoice)
                    .collect(Collectors.toList());

            for (int i = 0; i < cardsWithChoice.size(); i++) {
                final CrysisCard crysisCard = cardsWithChoice.get(i);
                final Player player = players.get(i % players.size());
                player.addNextTurn(new CrysisCardPersistentChoiceTurn(player.getUuid(), crysisCard.getId(), Map.of(), true));
            }
            game.setStateTypeWithFinalize(StateType.RESOLVE_CRYSIS_WITH_CHOICE, contextProvider.provide(game));
        } else {
            performStateTransferIntoTakeNextCrysisCard(game);
        }
    }

    public void performStateTransferIntoTakeNextCrysisCard(MarsGame game) {
        if (game.getStateType() == StateType.GAME_END) {
            return;
        }

        final CrysisData crysisData = game.getCrysisData();
        if (crysisData.getEasyModeTurnsLeft() != 0) {
            crysisData.setEasyModeTurnsLeft(crysisData.getEasyModeTurnsLeft() - 1);
            performStateTransferIntoCrisisDrawDummyHand(game);
            return;
        }

        final List<Integer> openedCrysisCards = crysisData.getCrysisCards().dealCards(1);

        final CrysisCard crysisCard = cardService.getCrysisCard(openedCrysisCards.get(0));

        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());

        for (Player player : players) {
            if (crysisCard.immediateEffectRequiresChoice()) {
                player.addNextTurn(new CrysisCardImmediateChoiceTurn(player.getUuid(), crysisCard.getId(), Map.of(), true));
            } else {
                player.addNextTurn(new CrysisImmediateAllTurn(player.getUuid(), true));
            }
        }
        game.setStateTypeWithFinalize(StateType.DRAW_CRYSIS_CARD, contextProvider.provide(game));

        crysisData.getOpenedCards().add(crysisCard.getId());
        crysisData.getCardToTokensCount().put(crysisCard.getId(), crysisCard.initialTokens());
    }

    public void performStateTransferFromPhase(MarsGame game, int fromState) {
        if (game.gameEndCondition()) {
            game.finalizePhase(StateType.GAME_END, contextProvider.provide(game));
            fillDeltaForRecordsWithoutDelta(StateType.GAME_END, game);
            game.setStateType(StateType.GAME_END);
            return;
        }

        if (fromState == 0 && game.isDummyHandMode()) {
            game.updateDummyHand(shuffleService);
        }

        Set<Integer> pickedPhases = game.getPlayerUuidToPlayer()
                .values()
                .stream()
                .map(Player::getChosenPhase)
                .collect(Collectors.toSet());
        if (game.isDummyHandMode() && fromState >= 0) {
            pickedPhases.add(game.getCurrentDummyHand());
        }
        if (game.isCrysis() && fromState >= 0) {
            pickedPhases.addAll(game.getCrysisData().getChosenDummyPhases());
        }

        if (!crisisDetrimentService.canPlayFirstPhase(game)) {
            pickedPhases.remove(1);
        }

        if (fromState <= 1 && pickedPhases.contains(1)) {
            transferToPhaseOne(game);
        } else if (fromState <= 2 && pickedPhases.contains(2)) {
            transferToPhaseTwo(game);
        } else if (fromState <= 3 && pickedPhases.contains(3)) {
            transferToPhaseThree(game);
        } else if (fromState <= 4 && pickedPhases.contains(4)) {
            transferToPhaseFour(game);
        } else if (fromState <= 5 && pickedPhases.contains(5)) {
            transferToPhaseFive(game);
        } else if (game.getPlayerUuidToPlayer().values().stream().anyMatch(player -> player.getHand().size() > Constants.MAX_HAND_SIZE_LAST_ROUND)) {
            transferToSellCards(game);
        } else {
            if (game.isCrysis()) {
                final Planet planet = game.getPlanet();
                if (game.getCrysisData()
                        .getOpenedCards()
                        .stream()
                        .map(cardService::getCrysisCard)
                        .anyMatch(CrysisCard::endGameCard)
                        && planet.isOceansMax() && planet.isTemperatureMax() && planet.isOxygenMax()) {
                    game.getCrysisData().setWonGame(true);
                    game.setStateType(StateType.GAME_END, contextProvider.provide(game),
                            "Congratulations! You have successfully dealt with the crisis and saved the Mars. " +
                                    "Your quick thinking, strategic planning, and unwavering determination have paid off, and the Mars is a safer place because of your efforts. " +
                                    "Well done, and thank you for playing!");
                } else {
                    if (winPointsService.countCrysisWinPoints(game) >= 2
                            && game.getCrysisData().getCardToTokensCount().values().stream().anyMatch(value -> value != 0)) {
                        game.setStateTypeWithFinalize(StateType.CRISIS_END_STEP, contextProvider.provide(game));
                    } else {
                        performStateTransferIntoResolveDetrimentTokens(game);
                    }
                }
            } else {
                transferToPickPhase(game);
            }
        }
    }

    private void transferToPhaseOne(MarsGame game) {
        game.setStateTypeWithFinalize(StateType.BUILD_GREEN_PROJECTS, contextProvider.provide(game));

        for (Player player : game.getPlayerUuidToPlayer().values()) {
            List<BuildDto> builds = new ArrayList<>();

            if (player.getChosenPhase() != 1 || !crisisDetrimentService.canGetFirstPhaseBonus(game)) {
                builds.add(new BuildDto(BuildType.GREEN, 0));
            } else {
                if (player.hasPhaseUpgrade(Constants.PHASE_1_UPGRADE_DISCOUNT)) {
                    builds.add(new BuildDto(BuildType.GREEN, 6));
                } else if (player.hasPhaseUpgrade(Constants.PHASE_1_UPGRADE_BUILD_EXTRA)) {
                    builds.add(new BuildDto(BuildType.GREEN, 3));
                    builds.add(new BuildDto(BuildType.GREEN, 0, 12));
                } else {
                    builds.add(new BuildDto(BuildType.GREEN, 3));
                }
            }
            player.setBuilds(builds);
        }
    }

    private void transferToPhaseTwo(MarsGame game) {
        int previousPhase = game.getCurrentPhase();
        game.finalizePhase(StateType.BUILD_BLUE_RED_PROJECTS, contextProvider.provide(game));
        if (previousPhase == 1) {
            fillDeltaForRecordsWithoutDelta(StateType.BUILD_BLUE_RED_PROJECTS, game);
        }
        game.setStateType(StateType.BUILD_BLUE_RED_PROJECTS);


        for (Player player : game.getPlayerUuidToPlayer().values()) {
            List<BuildDto> builds = new ArrayList<>();
            builds.add(new BuildDto(BuildType.BLUE_RED));

            if (player.getChosenPhase() == 2) {
                if (player.hasPhaseUpgrade(Constants.PHASE_2_UPGRADE_PROJECT_AND_MC)) {
                    builds.add(new BuildDto(BuildType.BLUE_RED_OR_MC));
                } else if (player.hasPhaseUpgrade(Constants.PHASE_2_UPGRADE_PROJECT_AND_CARD)) {
                    player.getHand().addCards(cardService.dealCards(game, 1));
                    builds.add(new BuildDto(BuildType.BLUE_RED));
                } else {
                    builds.add(new BuildDto(BuildType.BLUE_RED_OR_CARD));
                }
            }
            player.setBuilds(builds);
        }
    }

    private void transferToPhaseThree(MarsGame game) {
        int previousPhase = game.getCurrentPhase();
        game.finalizePhase(StateType.PERFORM_BLUE_ACTION, contextProvider.provide(game));
        if (previousPhase == 1 || previousPhase == 2) {
            fillDeltaForRecordsWithoutDelta(StateType.PERFORM_BLUE_ACTION, game);
        }
        game.setStateType(StateType.PERFORM_BLUE_ACTION);


        game.getPlayerUuidToPlayer().values().forEach(
                player -> {
                    if (player.getChosenPhase() == 3 && player.hasPhaseUpgrade(Constants.PHASE_3_UPGRADE_REVEAL_CARDS)) {
                        final List<Integer> cards = cardService.dealCards(game, 3);
                        for (Integer cardId : cards) {
                            final Card card = cardService.getCard(cardId);
                            if (card.getColor() == CardColor.BLUE || card.getColor() == CardColor.RED) {
                                player.getHand().addCard(cardId);
                            }
                        }
                    }
                    if (player.getProjectInspection() > 0) {
                        player.setProjectInspection(player.getProjectInspection() - 1);
                        player.setBlueActionExtraActivationsLeft(player.getBlueActionExtraActivationsLeft() + 1);
                    }
                });
    }

    private void transferToPhaseFour(MarsGame game) {
        int previousPhase = game.getCurrentPhase();
        game.finalizePhase(StateType.COLLECT_INCOME, contextProvider.provide(game));
        if (previousPhase >= 1 && previousPhase <= 3) {
            fillDeltaForRecordsWithoutDelta(StateType.COLLECT_INCOME, game);
        }
        game.setStateType(StateType.COLLECT_INCOME);
    }

    private void transferToPhaseFive(MarsGame game) {
        int previousPhase = game.getCurrentPhase();
        game.finalizePhase(StateType.DRAFT_CARDS, contextProvider.provide(game));
        if (previousPhase >= 1 && previousPhase <= 4) {
            fillDeltaForRecordsWithoutDelta(StateType.DRAFT_CARDS, game);
        }
        game.setStateType(StateType.DRAFT_CARDS);
        game.getPlayerUuidToPlayer().values().forEach(player -> player.addNextTurn(new DraftCardsTurn(player.getUuid())));
    }

    private void transferToSellCards(MarsGame game) {
        game.finalizePhase(StateType.SELL_EXTRA_CARDS, contextProvider.provide(game));
        fillDeltaForRecordsWithoutDelta(StateType.SELL_EXTRA_CARDS, game);
        game.setStateType(StateType.SELL_EXTRA_CARDS);
    }

    public void transferToPickPhase(MarsGame game) {
        game.finalizePhase(StateType.PICK_PHASE, contextProvider.provide(game));
        fillDeltaForRecordsWithoutDelta(StateType.PICK_PHASE, game);
        game.setStateType(StateType.PICK_PHASE);
    }


    private void fillDeltaForRecordsWithoutDelta(StateType stateType, MarsGame game) {
        if (!GameArenaContext.isArenaAvailable()) {
            return;
        }

        if (!AiConstants.POLICY_AVAILABLE) {
            return;
        }

        boolean needsFirstPlayerState = false;
        boolean needsSecondPlayerState = false;

        boolean stopBackPropagationFirst = false;
        boolean stopBackPropagationSecond = false;

        GameRecordArena arena = GameArenaContext.current();

        // ---- первый проход: определяем нужно ли считать ----
        for (int i = arena.size() - 1; i >= 0; i--) {
            if (stopBackPropagationFirst && stopBackPropagationSecond) {
                break;
            }

            PolicyRecord record = arena.records()[i];

            if (record.isBackwardPropagationStop) {
                if (record.isFirstPlayer) stopBackPropagationFirst = true;
                else stopBackPropagationSecond = true;
                continue;
            }

            if (record.finalizedDelta) {
                continue;
            }

            if (record.isFirstPlayer) {
                needsFirstPlayerState = true;
            } else {
                needsSecondPlayerState = true;
            }
        }

        if (!needsFirstPlayerState && !needsSecondPlayerState) {
            return;
        }

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player firstBot = players.getFirst().isFirstBot() ? players.getFirst() : players.getLast();
        Player secondBot = players.getFirst().isFirstBot() ? players.getLast() : players.getFirst();

        int firstWinPoints = 0;
        int secondWinPoints = 0;
        if (stateType == StateType.GAME_END) {
            firstWinPoints = winPointsService.countWinPoints(firstBot, game);
            secondWinPoints = winPointsService.countWinPoints(secondBot, game);
        }

        Planet planetNextPhase = new Planet(game.getPlanet());
        Planet planetAtTheStartOfThePhase = game.getPlanetAtTheStartOfThePhase();
        game.setPlanetAtTheStartOfThePhase(planetNextPhase);

        float firstPlayerPrediction = 0f;
        float secondPlayerPrediction = 0f;

        if (stateType == StateType.GAME_END) {
            if (firstWinPoints > secondWinPoints) {
                firstPlayerPrediction = 1;
                secondPlayerPrediction = -1;
            } else if (secondWinPoints > firstWinPoints) {
                firstPlayerPrediction = -1;
                secondPlayerPrediction = 1;
            }
        } else {
            if (needsFirstPlayerState && needsSecondPlayerState) {

                PolicyRecord firstRecord = new PolicyRecord();
                encoder.encode(game, firstBot, secondBot, firstRecord);

                PolicyRecord secondRecord = new PolicyRecord();
                encoder.encode(game, secondBot, firstBot, secondRecord);

                List<PolicyPrediction> predictions = policyActionSelector.predict(List.of(firstRecord, secondRecord));

                firstPlayerPrediction = predictions.getFirst().outcome;
                secondPlayerPrediction = predictions.getLast().outcome;

            } else if (needsFirstPlayerState) {
                PolicyRecord firstRecord = new PolicyRecord();
                encoder.encode(game, firstBot, secondBot, firstRecord);
                List<PolicyPrediction> predictions = policyActionSelector.predict(List.of(firstRecord));
                firstPlayerPrediction = predictions.getFirst().outcome;
            } else {
                PolicyRecord secondRecord = new PolicyRecord();
                encoder.encode(game, secondBot, firstBot, secondRecord);
                List<PolicyPrediction> predictions = policyActionSelector.predict(List.of(secondRecord));
                secondPlayerPrediction = predictions.getLast().outcome;
            }
        }


        stopBackPropagationFirst = false;
        stopBackPropagationSecond = false;

        for (int i = arena.size() - 1; i >= 0; i--) {
            if (stopBackPropagationFirst && stopBackPropagationSecond) {
                break;
            }

            PolicyRecord record = arena.records()[i];

            if (record.isBackwardPropagationStop) {
                if (record.isFirstPlayer) {
                    stopBackPropagationFirst = true;
                } else {
                    stopBackPropagationSecond = true;
                }
                continue; // якорь не обрабатываем
            }

            // --- если для игрока уже стоп — пропускаем ---
            if (record.isFirstPlayer) {
                if (stopBackPropagationFirst) {
                    continue;
                }
            } else {
                if (stopBackPropagationSecond) {
                    continue;
                }
            }

            // --- если уже finalized — пропускаем ---
            if (record.finalizedDelta) {
                continue;
            }

            // --- считаем ---
            if (record.isFirstPlayer) {

                if (record.marsReference != null) {
                    MarsGame oldGameCopy = record.marsReference;
                    MarsGame currentGameCopy = new MarsGame(game);

                    Player currentPlayerOldVersion = oldGameCopy.getPlayerByUuid(firstBot.getUuid());
                    Planet oldPlanetCopy = oldGameCopy.getPlanet();

                    currentGameCopy.getPlayerUuidToPlayer().put(currentPlayerOldVersion.getUuid(), currentPlayerOldVersion);

                    for (int j = 0; j < record.opponentDidOxygen; j++) {
                        oldPlanetCopy.increaseOxygen();
                    }
                    for (int j = 0; j < record.opponentDidTemperature; j++) {
                        oldPlanetCopy.increaseTemperature();
                    }
                    for (int j = 0; j < record.opponentOpenedOceans; j++) {
                        oldPlanetCopy.revealOcean();
                    }

                    currentGameCopy.setPlanetAtTheStartOfThePhase(oldPlanetCopy);
                    currentGameCopy.setPlanet(oldPlanetCopy);
                    currentGameCopy.setMilestones(oldGameCopy.getMilestones());
                    currentGameCopy.setAwards(oldGameCopy.getAwards());

                    currentGameCopy.finalizePhase(stateType, contextProvider.provide(currentGameCopy));

                    PolicyRecord newRecord = new PolicyRecord();
                    encoder.encode(currentGameCopy,
                            currentGameCopy.getPlayerByUuid(firstBot.getUuid()),
                            currentGameCopy.getPlayerByUuid(secondBot.getUuid()),
                            newRecord);



                    record.policyDelta = PolicyLogitDeltaHelper.calculateLogitAdvantage(firstPlayerPrediction, policyActionSelector.predict(List.of(newRecord)).getFirst().outcome);

                } else {

                    record.policyDelta = PolicyLogitDeltaHelper.calculateLogitAdvantage(firstPlayerPrediction, record.policyDelta);
                }

                record.finalizedDelta = true;
                record.isBackwardPropagationStop = true;
                record.marsReference = null;
            } else {

                if (record.marsReference != null) {
                    MarsGame oldGameCopy = record.marsReference;
                    MarsGame currentGameCopy = new MarsGame(game);

                    Player currentPlayerOldVersion = oldGameCopy.getPlayerByUuid(secondBot.getUuid());
                    Planet oldPlanetCopy = oldGameCopy.getPlanet();

                    currentGameCopy.getPlayerUuidToPlayer().put(currentPlayerOldVersion.getUuid(), currentPlayerOldVersion);

                    for (int j = 0; j < record.opponentDidOxygen; j++) {
                        oldPlanetCopy.increaseOxygen();
                    }
                    for (int j = 0; j < record.opponentDidTemperature; j++) {
                        oldPlanetCopy.increaseTemperature();
                    }
                    for (int j = 0; j < record.opponentOpenedOceans; j++) {
                        oldPlanetCopy.revealOcean();
                    }

                    currentGameCopy.setPlanetAtTheStartOfThePhase(oldPlanetCopy);
                    currentGameCopy.setPlanet(oldPlanetCopy);
                    currentGameCopy.setMilestones(oldGameCopy.getMilestones());
                    currentGameCopy.setAwards(oldGameCopy.getAwards());

                    currentGameCopy.finalizePhase(stateType, contextProvider.provide(currentGameCopy));

                    PolicyRecord newRecord = new PolicyRecord();
                    encoder.encode(currentGameCopy,
                            currentGameCopy.getPlayerByUuid(secondBot.getUuid()),
                            currentGameCopy.getPlayerByUuid(firstBot.getUuid()),
                            newRecord);

                    record.policyDelta = PolicyLogitDeltaHelper.calculateLogitAdvantage(secondPlayerPrediction, policyActionSelector.predict(List.of(newRecord)).getFirst().outcome);

                } else {
                    record.policyDelta = PolicyLogitDeltaHelper.calculateLogitAdvantage(secondPlayerPrediction, record.policyDelta);
                }

                record.finalizedDelta = true;
                record.isBackwardPropagationStop = true;
                record.marsReference = null;
            }
        }

        game.setPlanetAtTheStartOfThePhase(planetAtTheStartOfThePhase);

        for (int i = 0; i < arena.size(); i++) {
            PolicyRecord record = arena.records()[i];
            if (!record.finalizedDelta) {
                throw new IllegalStateException("Arena backpropagation broke");
            }
            arena.records()[i] = null;
            try {
                GameArenaContext.RECORD_QUEUE.put(record);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        arena.setSize(0);
    }

}
