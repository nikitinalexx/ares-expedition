package com.terraforming.ares.services.ai.network2;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.corporations.HyperionSystemsCorporation;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.StandardProjectService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.AiEndgameService;
import com.terraforming.ares.services.ai.advanced.IDataCollect;
import com.terraforming.ares.services.ai.dl4j.NNService;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import com.terraforming.ares.services.ai.network2.buildParams.*;
import com.terraforming.ares.services.ai.network2.dto.ScoredNode;
import com.terraforming.ares.services.ai.network2.dto.StateWithVector;
import com.terraforming.ares.services.ai.network2.projection.SelfReplicatingBacteriaService;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import com.terraforming.ares.services.ai.turnProcessors.frontier.*;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;
import lombok.RequiredArgsConstructor;
import org.nd4j.common.io.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class Network2ThirdPhaseActionProjector {
    private static final double EPS = 5e-4;
    private static final int BEST_NODES_TO_CHECK_FROM_PLAYER = 4;

    private final CardService cardService;
    private final AiTurnService aiTurnService;
    private final AiInputOptimizer aiInputOptimizer;
    private final ActionProjectionDataCollector actionProjectionDataCollector;
    private final IDataCollect iDataCollect;
    private final NNService nnService;
    private final AiOptimalBuildService aiOptimalBuildService;
    private final AiCardBuildInputAnalyzer aiCardBuildInputAnalyzer;
    private final Network2DraftCardsProjectionService network2DraftCardsProjectionService;
    private final StandardProjectService standardProjectService;
    private final Frontier frontier;
    private final SelfReplicatingBacteriaService selfReplicatingBacteriaService;
    private final AiEndgameService aiEndgameService;

    private final Set<Class<?>> DO_IMMEDIATELY_UNCONDITIONALLY = Set.of(
            HyperionSystemsCorporation.class,
            AiCentral.class,
            Birds.class,
            CircuitBoardFactory.class,
            CommunityGardens.class,
            Tardigrades.class,
            CityCouncil.class,
            DroneAssistedConstruction.class
    );

    public double processTurn(MarsGame game, Player player, Player anotherPlayer) {
        NNService.ModelType modelType = player.isFirstBot() ? NNService.ModelType.FIRST : NNService.ModelType.SECOND;
        Map<Class<?>, Card> playerBlueCards = applyPlayerActions(game, player, modelType);
        applyPlayerActions(game, anotherPlayer, modelType);

        List<ScoredNode> bestRegularFutureOptions = getBestFutureOptions(game, player, modelType);
        List<ScoredNode> bestOpponentRegularFutureOptions = getBestFutureOptions(game, anotherPlayer, modelType);

        if (playerBlueCards.containsKey(SelfReplicatingBacteria.class)) {
            MarsGame potentialMarsAfterDoingSelfReplicatingBacteria = selfReplicatingBacteriaService.simulateSelfReplicatingBacteriaFinalActions(playerBlueCards, game, player);

            if (potentialMarsAfterDoingSelfReplicatingBacteria != null) {
                List<ScoredNode> bestFutureOptionsAfterReplicatingBacteria = getBestFutureOptions(potentialMarsAfterDoingSelfReplicatingBacteria, potentialMarsAfterDoingSelfReplicatingBacteria.getPlayerByUuid(player.getUuid()), modelType);

                boolean chooseStateAfterSelfReplicatingBacteria = false;

                if (!bestRegularFutureOptions.isEmpty() && !bestFutureOptionsAfterReplicatingBacteria.isEmpty()) {
                    chooseStateAfterSelfReplicatingBacteria = (bestFutureOptionsAfterReplicatingBacteria.getFirst().getScore() > bestRegularFutureOptions.getFirst().getScore());
                } else {
                    List<float[]> baseProbToCheck = new ArrayList<>();
                    if (bestRegularFutureOptions.isEmpty()) {
                        baseProbToCheck.add(iDataCollect.collectData(game, player));
                    }
                    if (bestFutureOptionsAfterReplicatingBacteria.isEmpty()) {
                        baseProbToCheck.add(iDataCollect.collectData(potentialMarsAfterDoingSelfReplicatingBacteria, potentialMarsAfterDoingSelfReplicatingBacteria.getPlayerByUuid(player.getUuid())));
                    }
                    List<Prediction> predictions = nnService.predictBatch(baseProbToCheck, player);

                    double bestRegularPrediction = (bestRegularFutureOptions.isEmpty() ? predictions.removeFirst().baseProb : bestRegularFutureOptions.getFirst().getScore());
                    double bestSelfReplicatingPrediction = (bestFutureOptionsAfterReplicatingBacteria.isEmpty() ? predictions.removeFirst().baseProb : bestFutureOptionsAfterReplicatingBacteria.getFirst().getScore());

                    chooseStateAfterSelfReplicatingBacteria = bestSelfReplicatingPrediction > bestRegularPrediction;
                }

                if (chooseStateAfterSelfReplicatingBacteria) {
                    bestRegularFutureOptions = bestFutureOptionsAfterReplicatingBacteria;
                    game = potentialMarsAfterDoingSelfReplicatingBacteria;
                    player = potentialMarsAfterDoingSelfReplicatingBacteria.getPlayerByUuid(player.getUuid());
                    anotherPlayer = potentialMarsAfterDoingSelfReplicatingBacteria.getPlayerByUuid(anotherPlayer.getUuid());
                }
            }
        }

        State bestOpponentStateAfterStandardProjects = getBestOpponentStateAfterStandardProjects(bestOpponentRegularFutureOptions, game, player, anotherPlayer);
        if (bestRegularFutureOptions.isEmpty()) {
            StateContext stateContext = createStateContext(game, player);
            State initialState = stateContext.getInitialState();

            MarsGame gameCopy = new MarsGame(game);
            Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());
            Player opponentCopy = gameCopy.getPlayerByUuid(anotherPlayer.getUuid());

            applyDeltaState(opponentCopy, bestOpponentStateAfterStandardProjects);

            float[] data = iDataCollect.collectData(gameCopy, playerCopy);
            iDataCollect.modifyOpponentHandSize(data, bestOpponentStateAfterStandardProjects.cards);
            iDataCollect.modifyOpponentWinPoints(data, (float) (bestOpponentStateAfterStandardProjects.wp + bestOpponentStateAfterStandardProjects.sacrificialWp) / AiConstants.WP_DENOMINATOR_FOR_FRONTIER);

            ScoredNode scoredNode = new ScoredNode(initialState, nnService.predictBatch(List.of(data), modelType).getFirst().baseProb);
            bestRegularFutureOptions = List.of(scoredNode);
        }

        return getBestChanceFromOptions(bestRegularFutureOptions, game, player, anotherPlayer, bestOpponentStateAfterStandardProjects);
    }

    private State getBestOpponentStateAfterStandardProjects(List<ScoredNode> bestRegularFutureOptions, MarsGame game, Player player, Player opponent) {
        StateContext stateContext = createStateContext(game, opponent);

        if (bestRegularFutureOptions.isEmpty()) {
            return stateContext.getInitialState();
        } else {
            List<float[]> playerStatesAfterStandardProjects = new ArrayList<>();
            List<List<StateWithVector>> allStatesWithVectors = new ArrayList<>();

            for (int i = 0; i < BEST_NODES_TO_CHECK_FROM_PLAYER && i < bestRegularFutureOptions.size(); i++) {
                ScoredNode scoredNode = bestRegularFutureOptions.get(i);

                State state = scoredNode.getState();

                List<StateWithVector> stateWithVectors = projectStateWithStandardProjects(stateContext, state, game, opponent);
                allStatesWithVectors.add(stateWithVectors);
                playerStatesAfterStandardProjects.addAll(stateWithVectors.stream().map(StateWithVector::getVector).toList());
            }

            List<Prediction> bestPredictions = nnService.predictBatch(playerStatesAfterStandardProjects, player);


            State bestState = stateContext.getInitialState();
            double bestStateChance = bestRegularFutureOptions.getFirst().getScore();
            int predictionIndex = 0;
            for (List<StateWithVector> stateWithVector : allStatesWithVectors) {
                for (StateWithVector stateVector : stateWithVector) {
                    Prediction prediction = bestPredictions.get(predictionIndex++);
                    if (prediction.baseProb > bestStateChance) {
                        bestStateChance = prediction.baseProb;
                        bestState = stateVector.getState();
                    }
                }
            }

            return bestState;
        }
    }

    private double getBestChanceFromOptions(List<ScoredNode> bestRegularFutureOptions, MarsGame game, Player player, Player anotherPlayer, State bestOpponentState) {
        StateContext stateContext = createStateContext(game, player);

        List<float[]> playerStatesAfterStandardProjects = new ArrayList<>();

        for (int i = 0; i < BEST_NODES_TO_CHECK_FROM_PLAYER && i < bestRegularFutureOptions.size(); i++) {
            ScoredNode scoredNode = bestRegularFutureOptions.get(i);

            State state = scoredNode.getState();

            List<StateWithVector> stateWithVectors = projectStateWithStandardProjects(stateContext, state, game, player, anotherPlayer, bestOpponentState);

            playerStatesAfterStandardProjects.addAll(stateWithVectors.stream().map(StateWithVector::getVector).toList());
        }

        List<Prediction> bestPredictions = nnService.predictBatch(playerStatesAfterStandardProjects, player);

        double bestStateChance = bestRegularFutureOptions.getFirst().getScore();

        for (Prediction bestPrediction : bestPredictions) {
            if (bestPrediction.baseProb > bestStateChance) {
                bestStateChance = bestPrediction.baseProb;
            }
        }

        return bestStateChance;
    }

    private StateContext createStateContext(MarsGame game, Player player) {
        Map<Class<?>, Card> cardsPlayed = player.getPlayed().getCards().stream().map(cardService::getCard).collect(Collectors.toMap(Card::getClass, Function.identity()));

        return new StateContext(cardService, cardsPlayed, game.getPlanetAtTheStartOfThePhase(), player, game);
    }


    private Map<Class<?>, Card> applyPlayerActions(MarsGame game, Player player, NNService.ModelType modelType) {
        Deck activatedBlueCards = player.getActivatedBlueCards();

        Map<Class<?>, Card> neverActivatedBlueCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(Card::isActiveCard)
                .filter(card -> !activatedBlueCards.containsCard(card.getId()))
                .collect(Collectors.toMap(Card::getClass, Function.identity()));

        doImmediateUnconditionalActions(game, player, neverActivatedBlueCards);
        removeCompletelyUnusableCards(game, player, neverActivatedBlueCards);
        doActionsWhereChoiceHasNoChoice(game, player, neverActivatedBlueCards);
        processConservedBiomeAndResearchGrant(game, player, neverActivatedBlueCards);
        boolean phaseUpgradesDone;
        do {
            phaseUpgradesDone = processPhaseUpgradeCards(game, player, neverActivatedBlueCards, modelType);
        } while (phaseUpgradesDone);


        int blueActionExtraActivationsLeft = player.getBlueActionExtraActivationsLeft();
        Deck activatedBlueCardsTwice = player.getActivatedBlueCardsTwice();

        Map<Class<?>, Card> blueCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(Card::isActiveCard)
                .filter(card -> !activatedBlueCards.containsCard(card.getId()) || blueActionExtraActivationsLeft > 0 && !activatedBlueCardsTwice.containsCard(card.getId()))
                .collect(Collectors.toMap(Card::getClass, Function.identity()));

        removeCompletelyUnusableCards(game, player, blueCards);

        return blueCards;
    }

    private List<StateWithVector> projectStateWithStandardProjects(StateContext stateContext, State deltaState, MarsGame game, Player player, Player anotherPlayer, State opponentState) {
        MarsGame gameCopy = new MarsGame(game);
        Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());
        Player anotherPlayerCopy = gameCopy.getPlayerByUuid(anotherPlayer.getUuid());
        List<StateWithVector> results = new ArrayList<>();

        // Океаны
        if (!stateContext.isOceansMax()) {
            projectTypeLoop(results,  deltaState, player, anotherPlayerCopy, gameCopy, playerCopy, anotherPlayerCopy,
                    StandardProjectType.OCEAN, stateContext::oceanBuilt, opponentState);
        }

        // Температура
        if (!stateContext.isTemperatureMax()) {
            projectTypeLoop(results,  deltaState, player, anotherPlayerCopy, gameCopy, playerCopy, anotherPlayerCopy,
                    StandardProjectType.TEMPERATURE, stateContext::temperatureBuilt, opponentState);
        }

        // Леса
        projectTypeLoop(results,  deltaState, player, anotherPlayerCopy, gameCopy, playerCopy, anotherPlayerCopy,
                StandardProjectType.FOREST, stateContext::forestBuilt, opponentState);

        return results;
    }

    private List<StateWithVector> projectStateWithStandardProjects(StateContext stateContext, State deltaState, MarsGame game, Player player) {
        MarsGame gameCopy = new MarsGame(game);
        Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());
        List<StateWithVector> results = new ArrayList<>();

        // Океаны
        if (!stateContext.isOceansMax()) {
            projectTypeLoop(results,  deltaState, player, gameCopy, playerCopy,
                    StandardProjectType.OCEAN, stateContext::oceanBuilt);
        }

        // Температура
        if (!stateContext.isTemperatureMax()) {
            projectTypeLoop(results,  deltaState, player, gameCopy, playerCopy,
                    StandardProjectType.TEMPERATURE, stateContext::temperatureBuilt);
        }

        // Леса
        projectTypeLoop(results,  deltaState, player, gameCopy, playerCopy,
                StandardProjectType.FOREST, stateContext::forestBuilt);

        return results;
    }

    /**
     * Универсальный метод для прокрутки цикла по конкретному стандартному проекту
     */
    private void projectTypeLoop(
            List<StateWithVector> results,
            State deltaState,
            Player originalPlayer,
            Player originalOpponent,
            MarsGame gameCopy,
            Player playerCopy,
            Player opponentCopy,
            StandardProjectType type,
            Consumer<State> stateUpdateAction,
            State opponentState) {

        State stateCopy = deltaState.copy();
        int price = standardProjectService.getProjectPrice(originalPlayer, type);

        while (stateCopy.mc >= price) {
            stateCopy.mc -= price;
            stateUpdateAction.accept(stateCopy); // Вызов oceanBuilt, temperatureBuilt или forestBuilt

            // Применяем изменения к временному игроку для генерации вектора
            applyDeltaState(playerCopy, stateCopy);
            applyDeltaState(opponentCopy, opponentState);

            float[] data = iDataCollect.collectData(gameCopy, playerCopy);
            iDataCollect.modifyPlayerHandSize(data, stateCopy.cards);
            iDataCollect.modifyPlayerWinPoints(data, (float) (stateCopy.wp + stateCopy.sacrificialWp) / AiConstants.WP_DENOMINATOR_FOR_FRONTIER);
            iDataCollect.modifyOpponentHandSize(data, opponentState.cards);
            iDataCollect.modifyOpponentWinPoints(data, (float) (opponentState.wp + opponentState.sacrificialWp) / AiConstants.WP_DENOMINATOR_FOR_FRONTIER);

            results.add(new StateWithVector(stateCopy.copy(), data));

            // Откатываем состояние игрока в копии игры для следующей итерации/типа проекта
            restorePlayerState(playerCopy, originalPlayer);
            restorePlayerState(opponentCopy, originalOpponent);
        }
    }

    /**
     * Универсальный метод для прокрутки цикла по конкретному стандартному проекту
     */
    private void projectTypeLoop(
            List<StateWithVector> results,
            State deltaState,
            Player originalPlayer,
            MarsGame gameCopy,
            Player playerCopy,
            StandardProjectType type,
            Consumer<State> stateUpdateAction) {

        State stateCopy = deltaState.copy();
        int price = standardProjectService.getProjectPrice(originalPlayer, type);

        while (stateCopy.mc >= price) {
            stateCopy.mc -= price;
            stateUpdateAction.accept(stateCopy); // Вызов oceanBuilt, temperatureBuilt или forestBuilt

            // Применяем изменения к временному игроку для генерации вектора
            applyDeltaState(playerCopy, stateCopy);

            float[] data = iDataCollect.collectData(gameCopy, playerCopy);
            iDataCollect.modifyPlayerHandSize(data, stateCopy.cards);
            iDataCollect.modifyPlayerWinPoints(data,
                    (float) (stateCopy.wp + stateCopy.sacrificialWp) / AiConstants.WP_DENOMINATOR_FOR_FRONTIER);

            results.add(new StateWithVector(stateCopy.copy(), data));

            // Откатываем состояние игрока в копии игры для следующей итерации/типа проекта
            restorePlayerState(playerCopy, originalPlayer);
        }
    }

    private List<ScoredNode> getBestFutureOptions(MarsGame game, Player player, NNService.ModelType modelType) {
        Map<State, Node> stateNodeMap = frontier.doFrontier(game, player);
        if (stateNodeMap.isEmpty()) return List.of();

        return projectStateAndGetBestFutureOptions(stateNodeMap, game, player, modelType);
    }


    private List<ScoredNode> projectStateAndGetBestFutureOptions(Map<State, Node> stateNodeMap, MarsGame game, Player player, NNService.ModelType modelType) {

        MarsGame gameCopy = new MarsGame(game);
        Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());

        List<Map.Entry<State, Node>> stateNodeList = new ArrayList<>();
        List<float[]> states = new ArrayList<>();

        states.add(iDataCollect.collectData(game, player));

        for (Map.Entry<State, Node> entryNode : stateNodeMap.entrySet()) {
            stateNodeList.add(entryNode);
            State deltaState = entryNode.getKey();
            applyDeltaState(playerCopy, deltaState);


            float[] data = iDataCollect.collectData(gameCopy, playerCopy);
            iDataCollect.modifyPlayerHandSize(data, deltaState.cards);
            iDataCollect.modifyPlayerWinPoints(data, (float) (deltaState.wp + deltaState.sacrificialWp) / AiConstants.WP_DENOMINATOR_FOR_FRONTIER);
            states.add(data);
            restorePlayerState(playerCopy, player);
        }

        List<Prediction> predictions = nnService.predictBatch(states, modelType);
        double initialProbability = predictions.removeFirst().baseProb;


        List<ScoredNode> scoredNodes = new ArrayList<>(stateNodeList.size());

        for (int i = 0; i < stateNodeList.size(); i++) {
            double newProbability = predictions.get(i).baseProb;
            if (newProbability + EPS > initialProbability) {
                scoredNodes.add(new ScoredNode(stateNodeList.get(i).getKey(), newProbability));
            }
        }

        if (scoredNodes.isEmpty()) {
            return List.of();
        }

        // 2. Сортируем (O(N log N)) - используем примитивное сравнение для скорости
        scoredNodes.sort((a, b) -> Double.compare(b.score, a.score));

        return scoredNodes;
    }

    private void applyDeltaState(Player copyPlayer, State state) {
        copyPlayer.setMc((int) (state.mc + state.extraMcValue));
        copyPlayer.setHeat(state.heat);
        copyPlayer.setPlants(state.plants);
        copyPlayer.setTerraformingRating(state.tr);
        copyPlayer.setForests(copyPlayer.getForests() + state.extraForests);

        Map<Class<?>, Integer> cardResourcesCount = copyPlayer.getCardResourcesCount();
        if (cardResourcesCount.containsKey(GhgProductionBacteria.class)) {
            cardResourcesCount.put(GhgProductionBacteria.class, state.ghgBacteriaCount);
        }
        if (cardResourcesCount.containsKey(NitriteReductingBacteria.class)) {
            cardResourcesCount.put(NitriteReductingBacteria.class, state.nitriteReductingBacteria);
        }
        if (cardResourcesCount.containsKey(RegolithEaters.class)) {
            cardResourcesCount.put(RegolithEaters.class, state.regolithEaters);
        }
        if (cardResourcesCount.containsKey(SelfReplicatingBacteria.class)) {
            cardResourcesCount.put(SelfReplicatingBacteria.class, state.selfReplicatingBacteria);
        }
        if (cardResourcesCount.containsKey(AnaerobicMicroorganisms.class)) {
            cardResourcesCount.put(AnaerobicMicroorganisms.class, state.anaerobicMicroorganisms);
        }
        if (cardResourcesCount.containsKey(BacterialAggregates.class)) {
            cardResourcesCount.put(BacterialAggregates.class, state.bacterialAggregates);
        }
        if (cardResourcesCount.containsKey(Decomposers.class)) {
            cardResourcesCount.put(Decomposers.class, state.decomposers);
        }
        if (cardResourcesCount.containsKey(DecomposingFungus.class)) {
            cardResourcesCount.put(DecomposingFungus.class, state.decomposingFungus);
        }
    }

    private void restorePlayerState(Player copyPlayer, Player originalPlayer) {
        copyPlayer.setMc(originalPlayer.getMc());
        copyPlayer.setHeat(originalPlayer.getHeat());
        copyPlayer.setPlants(originalPlayer.getPlants());
        copyPlayer.setTerraformingRating(originalPlayer.getTerraformingRating());
        copyPlayer.setForests(originalPlayer.getForests());

        Map<Class<?>, Integer> originalResourceCount = originalPlayer.getCardResourcesCount();
        if (!originalResourceCount.isEmpty()) {
            copyPlayer.setCardResourcesCount(new HashMap<>(originalResourceCount));
        }
    }


    private void processConservedBiomeAndResearchGrant(MarsGame game, Player player, Map<Class<?>, Card> blueCards) {
        // Мгновенный поиск вместо цикла
        Card conservedBiome = blueCards.get(ConservedBiome.class);
        Card researchGrant = blueCards.get(ResearchGrant.class);

        if (conservedBiome == null && researchGrant == null) {
            return;
        }

        SharedInputAnalysis sharedInputAnalysis = new SharedInputAnalysis();

        if (conservedBiome != null) {
            Set<CardCollectableResource> resources = player.getPlayed().getCards().stream()
                    .map(cardService::getCard)
                    .map(Card::getCollectableResource)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // Логика: нужны животные, если они есть на столе, а микробов нет
            sharedInputAnalysis.setRequiresAnimalInput(resources.contains(CardCollectableResource.ANIMAL) && !resources.contains(CardCollectableResource.MICROBE));
        }

        if (researchGrant != null) {
            sharedInputAnalysis.setRequiresTagChoice(true);
            aiCardBuildInputAnalyzer.analyzeActiveEffectRequirements(player, List.of(AiConstants.RESEARCH_GRANT_DUMMY_CARD), sharedInputAnalysis);
        }

        OptimizedInputDecisions decisions = aiInputOptimizer.optimizeInputDecisions(game, player, sharedInputAnalysis);

        // Обработка Research Grant
        if (sharedInputAnalysis.isRequiresTagChoice() && researchGrant != null) {
            List<Tag> tagsAlreadyOnCard = player.getCardToTag().getOrDefault(ResearchGrant.class, List.of());

            // Ищем лучший тег из топа, которого еще нет на карте
            DecisionWithPrediction<Tag> bestNewTag = decisions.getBestTagDecision().stream()
                    .filter(d -> !tagsAlreadyOnCard.contains(d.getValue()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No suitable tags found for Research Grant"));

            decisions.setBestTagDecision(List.of(bestNewTag));

            CardWithInputParams cardWithInputParams = aiOptimalBuildService.generateInputBasedOnOptimizedDecisions(player, AiConstants.RESEARCH_GRANT_DUMMY_CARD, decisions);
            Map<Integer, List<Integer>> inputParams = cardWithInputParams.getInputParamsVariations().getFirst();

            if (inputParams.containsKey(InputFlag.MARS_UNIVERSITY_DUMMY_INPUT.getId())) {
                inputParams.put(InputFlag.MARS_UNIVERSITY_CARD.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
            }

            aiTurnService.performBlueAction(game, player, researchGrant.getId(), inputParams);
            blueCards.remove(researchGrant.getClass());
        }

        // Обработка Conserved Biome
        if (sharedInputAnalysis.isRequiresAnimalInput() && conservedBiome != null) {
            var animalDecision = decisions.getBestResourceCardPerType().get(InputRequirementType.ANIMAL_INPUT);
            if (animalDecision != null) {
                Card bestAnimalCard = animalDecision.getValue();
                aiTurnService.performBlueAction(game, player, conservedBiome.getId(), Map.of(InputFlag.CARD_CHOICE.getId(), List.of(bestAnimalCard.getId())));
                blueCards.remove(conservedBiome.getClass());
            }
        }
    }

    private boolean processPhaseUpgradeCards(MarsGame game, Player player, Map<Class<?>, Card> blueCards, NNService.ModelType modelType) {
        // Используем входную мапу напрямую
        Card experimentalTech = player.getTerraformingRating() > 0 ? blueCards.get(ExperimentalTechnology.class) : null;
        Card virtualEmployee = blueCards.get(VirtualEmployeeDevelopment.class);
        // Для FibrousComposite проверяем наличие 3+ ресурсов
        Card fibrousComposite = player.getCardResourcesCount().getOrDefault(FibrousCompositeMaterial.class, 0) >= 3
                ? blueCards.get(FibrousCompositeMaterial.class) : null;

        if (experimentalTech == null && virtualEmployee == null && fibrousComposite == null) {
            return false;
        }

        SharedInputAnalysis sharedInputAnalysis = new SharedInputAnalysis();
        sharedInputAnalysis.setRequiresPhaseUpgrade(true);

        OptimizedInputDecisions optimizedDecisions = aiInputOptimizer.optimizeInputDecisions(game, player, sharedInputAnalysis);

        // Тут берем лучший апгрейд фазы из оптимизатора
        var bestPhaseUpgrade = optimizedDecisions.getBestPhaseUpgradeOverall().getValue();

        List<float[]> simulationData = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();

        // Собираем данные для нейронки: текущее состояние + варианты действий
        simulationData.add(iDataCollect.collectData(game, player));

        if (experimentalTech != null) {
            simulationData.add(actionProjectionDataCollector.experimentalTechnology(game, player, bestPhaseUpgrade));
            actions.add(() -> {
                aiTurnService.performBlueAction(game, player, experimentalTech.getId(), Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(bestPhaseUpgrade)));
                blueCards.remove(experimentalTech.getClass());
            });
        }

        if (virtualEmployee != null) {
            simulationData.add(actionProjectionDataCollector.virtualEmployee(game, player, bestPhaseUpgrade));
            actions.add(() -> {
                aiTurnService.performBlueAction(game, player, virtualEmployee.getId(), Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(bestPhaseUpgrade)));
                blueCards.remove(virtualEmployee.getClass());
            });
        }

        if (fibrousComposite != null) {
            simulationData.add(actionProjectionDataCollector.fibrousCompositeMaterial(game, player, bestPhaseUpgrade));
            actions.add(() -> {
                aiTurnService.performBlueAction(game, player, fibrousComposite.getId(), Map.of(
                        InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(3),
                        InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(bestPhaseUpgrade)));
                blueCards.remove(fibrousComposite.getClass());
            });
        }

        // Запрос в модель (Batch)
        List<Prediction> allPredictions = nnService.predictBatch(simulationData, modelType);

        double bestWinChance = allPredictions.removeFirst().baseProb; // Базовый шанс без действий
        int bestActionIdx = -1;

        for (int i = 0; i < actions.size(); i++) {
            double currentProb = allPredictions.get(i).baseProb;
            if (currentProb > bestWinChance) {
                bestWinChance = currentProb;
                bestActionIdx = i;
            }
        }

        if (bestActionIdx != -1) {
            actions.get(bestActionIdx).run();
            return true;
        }

        // Если ни одно действие не улучшило винрейт — помечаем карты как "отработанные"
        if (virtualEmployee != null) {
            markCardAsUsed(player, virtualEmployee);
            blueCards.remove(virtualEmployee.getClass());
        }
        if (experimentalTech != null) {
            markCardAsUsed(player, experimentalTech);
            blueCards.remove(experimentalTech.getClass());
        }
        if (fibrousComposite != null) {
            // Особенность Fibrous: если не выгодно тратить 3 ресурса на апгрейд, просто добавляем 1 ресурс
            aiTurnService.performBlueAction(game, player, fibrousComposite.getId(),
                    Map.of(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(1)));
            blueCards.remove(fibrousComposite.getClass());
            return true;
        }

        return false;
    }

    private void markCardAsUsed(Player player, Card card) {
        player.getActivatedBlueCards().addCard(card.getId());
        player.getActivatedBlueCardsTwice().addCard(card.getId());
    }

    private void removeCompletelyUnusableCards(MarsGame game, Player player, Map<Class<?>, Card> blueCards) {
        Set<Class<?>> completelyUnusableCards = new HashSet<>();
        if (game.getPlanetAtTheStartOfThePhase().isOceansMax()) {
            completelyUnusableCards.add(AquiferPumping.class);
            completelyUnusableCards.add(VolcanicPools.class);
            completelyUnusableCards.add(WaterImportFromEuropa.class);
        }
        if (game.getPlanetAtTheStartOfThePhase().isTemperatureMax()) {
            completelyUnusableCards.add(DevelopedInfrastructure.class);
            completelyUnusableCards.add(WoodBurningStoves.class);
            completelyUnusableCards.add(GasCooledReactors.class);
        }
        if (game.getPlanetAtTheStartOfThePhase().isOxygenMax()) {
            completelyUnusableCards.add(IronWorks.class);
            completelyUnusableCards.add(ProgressivePolicies.class);
        }
        Set<CardCollectableResource> collectableResources = player.getPlayed().getCards().stream().map(cardService::getCard).map(Card::getCollectableResource).filter(Objects::nonNull).collect(Collectors.toSet());
        if (!collectableResources.contains(CardCollectableResource.MICROBE)) {
            completelyUnusableCards.add(SymbioticFungus.class);
            if (!collectableResources.contains(CardCollectableResource.ANIMAL)) {
                completelyUnusableCards.add(ConservedBiome.class);
            }
        }

        final List<Tag> cardTags = player.getCardToTag().getOrDefault(ResearchGrant.class, List.of());
        if (!CollectionUtils.isEmpty(cardTags) && cardTags.stream().noneMatch(tag -> tag == Tag.DYNAMIC)) {
            completelyUnusableCards.add(ResearchGrant.class);
        }

        blueCards.forEach((key, unusableCard) -> {
            if (completelyUnusableCards.contains(key)) {
                player.getActivatedBlueCards().addCard(unusableCard.getId());
                player.getActivatedBlueCardsTwice().addCard(unusableCard.getId());
            }
        });

        for (Class<?> completelyUnusableCard : completelyUnusableCards) {
            blueCards.remove(completelyUnusableCard);
        }
    }

    private void doImmediateUnconditionalActions(MarsGame game, Player player, Map<Class<?>, Card> availableCards) {
        Set<Class<?>> processedBlueCards = new HashSet<>();
        for (Map.Entry<Class<?>, Card> cardEntry : availableCards.entrySet()) {
            if (DO_IMMEDIATELY_UNCONDITIONALLY.contains(cardEntry.getKey())) {
                aiTurnService.performBlueAction(game, player, cardEntry.getValue().getId(), Map.of());
                processedBlueCards.add(cardEntry.getKey());
            }
        }
        for (Class<?> processedBlueCard : processedBlueCards) {
            availableCards.remove(processedBlueCard);
        }
    }

    private void doActionsWhereChoiceHasNoChoice(MarsGame game, Player player, Map<Class<?>, Card> availableCards) {
        // 1. Extreme Cold Fungus
        Card fungus = availableCards.getOrDefault(ExtremeColdFungus.class, availableCards.get(BuffedExtremeColdFungus.class));
        if (fungus != null) {
            if (player.getPlayed().getCards().stream().map(cardService::getCard).noneMatch(c -> c.getCollectableResource() == CardCollectableResource.MICROBE)) {
                aiTurnService.performBlueAction(game, player, fungus.getId(), Map.of(InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.getId(), List.of(0)));
                availableCards.remove(fungus.getClass());
            }
        }

        // 2. Matter Manufacturing & Think Tank
        Card matter = availableCards.get(MatterManufactoring.class);
        if (matter != null && player.getMc() > 0) {
            aiTurnService.performBlueAction(game, player, matter.getId(), Map.of());
            availableCards.remove(matter.getClass());
        }
        Card thinkTank = availableCards.get(ThinkTank.class);
        if (thinkTank != null && player.getMc() > 1) {
            aiTurnService.performBlueAction(game, player, thinkTank.getId(), Map.of());
            availableCards.remove(thinkTank.getClass());
        }

        // 3. Aquifer Pumping & Solar Punk
        Card aquifer = availableCards.get(AquiferPumping.class);
        if (aquifer != null && player.getSteelIncome() >= 5) {
            aiTurnService.performBlueAction(game, player, aquifer.getId(), Map.of());
            availableCards.remove(aquifer.getClass());
        }
        Card solarPunk = availableCards.get(SolarPunk.class);
        if (solarPunk != null && player.getTitaniumIncome() >= 8) {
            aiTurnService.performBlueAction(game, player, solarPunk.getId(), Map.of());
            availableCards.remove(solarPunk.getClass());
        }

        // 4. Bacteria & Microbe management
        performMicrobeAction(game, player, availableCards, NitriteReductingBacteria.class, game.getPlanetAtTheStartOfThePhase().isOceansMax());
        performMicrobeAction(game, player, availableCards, GhgProductionBacteria.class, game.getPlanetAtTheStartOfThePhase().isTemperatureMax());
        performMicrobeAction(game, player, availableCards, BuffedGhgProductionBacteria.class, game.getPlanetAtTheStartOfThePhase().isTemperatureMax());
        performMicrobeAction(game, player, availableCards, RegolithEaters.class, game.getPlanetAtTheStartOfThePhase().isOxygenMax());
        performMicrobeAction(game, player, availableCards, BuffedRegolithEaters.class, game.getPlanetAtTheStartOfThePhase().isOxygenMax());

        // 5. Fibrous Composite Material
        Card fibrous = availableCards.get(FibrousCompositeMaterial.class);
        if (fibrous != null && player.getCardResourcesCount().getOrDefault(FibrousCompositeMaterial.class, 0) < 3) {
            aiTurnService.performBlueAction(game, player, fibrous.getId(), Map.of(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(1)));
            availableCards.remove(fibrous.getClass());
        }
    }

    // Вспомогательный метод, чтобы не дублировать логику по бактериям
    private void performMicrobeAction(MarsGame game, Player player, Map<Class<?>, Card> availableCards, Class<?> clazz, boolean condition) {
        Card card = availableCards.get(clazz);
        if (card != null && condition) {
            aiTurnService.performBlueAction(game, player, card.getId(), Map.of(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(1)));
            availableCards.remove(card.getClass());
        }
    }

}
