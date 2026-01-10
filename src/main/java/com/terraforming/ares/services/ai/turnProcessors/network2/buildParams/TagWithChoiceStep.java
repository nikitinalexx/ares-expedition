package com.terraforming.ares.services.ai.turnProcessors.network2.buildParams;

import com.terraforming.ares.cards.blue.Decomposers;
import com.terraforming.ares.cards.corporations.DummyCard;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import com.terraforming.ares.services.ai.turnProcessors.AiUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TagWithChoiceStep implements DecisionStep {

    private final MarsGame game;
    private final Player player;
    private final Player anotherPlayer;
    private final OptimizedInputDecisions decisions;
    private final SharedInputAnalysis analysis;
    private final boolean needTagSimulationWithChoice;

    public TagWithChoiceStep(
            MarsGame game,
            Player player,
            Player anotherPlayer,
            OptimizedInputDecisions decisions,
            SharedInputAnalysis analysis,
            boolean needTagSimulationWithChoice) {
        this.game = game;
        this.player = player;
        this.anotherPlayer = anotherPlayer;
        this.decisions = decisions;
        this.analysis = analysis;
        this.needTagSimulationWithChoice = needTagSimulationWithChoice;
    }

    @Override
    public boolean isApplicable() {
        return needTagSimulationWithChoice;
    }

    @Override
    public void collectSimulations(List<float[]> simulations, DataCollectContext dataCollectContext) {
        simulations.addAll(createTagSimulationsWithChoice(game, player, anotherPlayer, dataCollectContext));
    }

    @Override
    public void applyPredictions(PredictionCursor cursor, OptimizedInputDecisions decisions) {
        addOptimizedTagDecisions(decisions, cursor.next(Tag.values().length - 1));
    }

    private List<float[]> createTagSimulationsWithChoice(MarsGame originalGame, Player originalPlayer, Player originalAnotherPlayer, DataCollectContext dataCollectContext) {
        CardService cardService = dataCollectContext.getCardService();
        AiUtility aiUtility = dataCollectContext.getAiUtility();
        MarsContextProvider marsContextProvider = dataCollectContext.getContextProvider();

        List<Card> applicableCards = originalPlayer.getPlayed().getCards().stream().map(cardService::getCard).filter(Card::onBuiltEffectApplicableToOther).toList();
        int originalPlayerHandSize = originalPlayer.getHand().getCards().size();

        List<float[]> simulationsByTag = new ArrayList<>(Tag.values().length - 1);

        for (Tag value : Tag.values()) {
            if (value == Tag.DYNAMIC) {
                continue;
            }

            Map<Integer, List<Integer>> inputParameters = new HashMap<>(Map.of(InputFlag.TAG_INPUT.getId(), List.of(value.ordinal())));

            if (value == Tag.SCIENCE && analysis.marsUniversityActive) {
                simulationsByTag.add(simulateScienceTagWithMarsUniversity(originalGame, originalPlayer, inputParameters, applicableCards, dataCollectContext));
            } else if ((analysis.viralEnhancersActive || analysis.decomposersActive) && (value == Tag.PLANT || value == Tag.MICROBE || value == Tag.ANIMAL)) {
                simulationsByTag.add(simulateDecomposersWithViralEnhancers(originalGame, originalPlayer, originalAnotherPlayer, inputParameters, applicableCards, decisions, analysis, dataCollectContext));
            } else {
                MarsGame gameCopy = new MarsGame(originalGame);
                Player playerCopy = gameCopy.getPlayerByUuid(originalPlayer.getUuid());

                MarsContext context = marsContextProvider.provide(gameCopy, playerCopy);
                applicableCards.forEach(c -> c.postProjectBuiltEffect(context, new DummyCard(), inputParameters));

                aiUtility.simulateDummyHandInsteadOfNewCards(playerCopy, originalPlayerHandSize);

                simulationsByTag.add(dataCollectContext.getDataCollect().collectData(gameCopy, playerCopy));
            }
        }

        return simulationsByTag;
    }

    private float[] simulateScienceTagWithMarsUniversity(MarsGame game, Player player, Map<Integer, List<Integer>> inputParameters, List<Card> applicableCards, DataCollectContext dataCollectContext) {
        game = new MarsGame(game);
        player = game.getPlayerByUuid(player.getUuid());

        int originalPlayerHandSize = player.getHand().getCards().size();
        boolean pretendMarsUniversityRotatesCard = false;

        inputParameters.put(InputFlag.MARS_UNIVERSITY_CARD.getId(), List.of(InputFlag.SKIP_ACTION.getId()));
        if (originalPlayerHandSize > 0) {
            pretendMarsUniversityRotatesCard = true;
        }

        MarsContext context = dataCollectContext.getContextProvider().provide(game, player);
        applicableCards.forEach(c -> c.postProjectBuiltEffect(context, new DummyCard(), inputParameters));

        dataCollectContext.getAiUtility().simulateDummyHandInsteadOfNewCards(player, originalPlayerHandSize);

        if (pretendMarsUniversityRotatesCard) {
            player.getHand().getCards().add(AiConstants.GENERIC_DUMMY_ID);
            player.setMc(player.getMc() - 3);
        }

        return dataCollectContext.getDataCollect().collectData(game, player);
    }

    private float[] simulateDecomposersWithViralEnhancers(MarsGame game, Player player, Player opponent, Map<Integer, List<Integer>> inputParameters, List<Card> applicableCards, OptimizedInputDecisions decisions, SharedInputAnalysis analysis, DataCollectContext dataCollectContext) {
        if (analysis.decomposersActive) {
            if (player.getCardResourcesCount().getOrDefault(Decomposers.class, 0) > 0) {
                inputParameters.put(InputFlag.DECOMPOSERS_TAKE_CARD.getId(), List.of(1));
            } else {
                inputParameters.put(InputFlag.DECOMPOSERS_TAKE_MICROBE.getId(), List.of(1));
            }
        }

        if (!analysis.viralEnhancersActive) {
            game = new MarsGame(game);
            player = game.getPlayerByUuid(player.getUuid());
            return simulateCardWithInput(game, player, applicableCards, inputParameters, dataCollectContext);
        }

        if (decisions.isViralEnhancersTakePlants()) {
            MarsGame gameCopy = new MarsGame(game);
            Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());

            Map<Integer, List<Integer>> inputParametersCopy = new HashMap<>(inputParameters);
            inputParametersCopy.put(InputFlag.VIRAL_ENHANCERS_TAKE_PLANT.getId(), List.of(1));

            playerCopy.setPlants(playerCopy.getPlants() + 1);
            return simulateCardWithInput(gameCopy, playerCopy, applicableCards, inputParametersCopy, dataCollectContext);
        } else {
            MarsGame gameCopy = new MarsGame(game);
            Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());

            Map<Integer, List<Integer>> inputParametersCopy = new HashMap<>(inputParameters);
            inputParametersCopy.put(InputFlag.VIRAL_ENHANCERS_PUT_RESOURCE.getId(), List.of(decisions.getBestResourceCardPerType().get(decisions.isMicrobeIsBetter() ? InputRequirementType.MICROBE_INPUT : InputRequirementType.ANIMAL_INPUT).value.getId()));

            return simulateCardWithInput(gameCopy, playerCopy, applicableCards, inputParametersCopy, dataCollectContext);
        }
    }

    private float[] simulateCardWithInput(MarsGame game, Player player, List<Card> applicableCards, Map<Integer, List<Integer>> inputParameters, DataCollectContext dataCollectContext) {
        int originalPlayerHandSize = player.getHand().getCards().size();

        MarsContext context = dataCollectContext.getContextProvider().provide(game, player);
        applicableCards.forEach(c -> c.postProjectBuiltEffect(context, new DummyCard(), inputParameters));

        dataCollectContext.getAiUtility().simulateDummyHandInsteadOfNewCards(player, originalPlayerHandSize);

        return dataCollectContext.getDataCollect().collectData(game, player);
    }

    private void addOptimizedTagDecisions(OptimizedInputDecisions decisions, List<Prediction> tagPredictions) {
        Tag[] allTags = Tag.values();
        List<DecisionWithPrediction<Tag>> allDecisions = new ArrayList<>();

        int predictionIdx = 0;
        for (Tag tag : allTags) {
            if (tag == Tag.DYNAMIC) continue;

            // Просто собираем всё в список
            allDecisions.add(new DecisionWithPrediction<>(tag, tagPredictions.get(predictionIdx++)));
        }

        // Сортируем по убыванию вероятности
        allDecisions.sort((a, b) -> Double.compare(b.getPrediction().baseProb, a.getPrediction().baseProb));

        // Берем топ-3 (с проверкой на случай, если тегов вдруг меньше 3)
        List<DecisionWithPrediction<Tag>> top3 = allDecisions.subList(0, Math.min(3, allDecisions.size()));

        decisions.setBestTagDecision(new ArrayList<>(top3));
    }

}

