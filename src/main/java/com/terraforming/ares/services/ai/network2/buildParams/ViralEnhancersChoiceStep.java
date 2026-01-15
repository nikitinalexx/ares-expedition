package com.terraforming.ares.services.ai.network2.buildParams;

import com.terraforming.ares.cards.corporations.DummyCard;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.dl4j.Prediction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ViralEnhancersChoiceStep implements DecisionStep {
    private final MarsGame game;
    private final Player player;
    private final OptimizedInputDecisions decisions;
    private int simulationsSize;
    private final boolean viralEnhancersActive;


    public ViralEnhancersChoiceStep(MarsGame game, Player player, OptimizedInputDecisions decisions, boolean viralEnhancersActive) {
        this.game = game;
        this.player = player;
        this.decisions = decisions;
        this.viralEnhancersActive = viralEnhancersActive;
    }

    @Override
    public boolean isApplicable() {
        return viralEnhancersActive;
    }

    @Override
    public void collectSimulations(List<float[]> simulations, DataCollectContext dataCollectContext) {
        List<float[]> newSimulations = simulateDecomposersWithViralEnhancers(game, player, decisions, dataCollectContext);
        simulationsSize = newSimulations.size();
        simulations.addAll(newSimulations);
    }

    @Override
    public void applyPredictions(PredictionCursor cursor, OptimizedInputDecisions decisions) {
        if (simulationsSize == 0) {
            decisions.setViralEnhancersTakePlants(true);
        } else {
            List<Prediction> predictions = cursor.next(simulationsSize);
            if (predictions.get(0).baseProb > predictions.get(1).baseProb) {
                decisions.setViralEnhancersTakePlants(true);
            }
        }
    }

    private List<float[]> simulateDecomposersWithViralEnhancers(MarsGame game, Player player, OptimizedInputDecisions decisions, DataCollectContext dataCollectContext) {
        if (!decisions.isAtLeastMicrobeOrAnimalPresent()) {
            return List.of();
        }
        CardService cardService = dataCollectContext.getCardService();

        List<Card> applicableCards = player.getPlayed().getCards().stream().map(cardService::getCard).filter(Card::onBuiltEffectApplicableToOther).toList();

        List<float[]> result = new ArrayList<>();
        {
            MarsGame gameCopy = new MarsGame(game);
            Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());
            Map<Integer, List<Integer>> inputParameters = Map.of(InputFlag.VIRAL_ENHANCERS_TAKE_PLANT.getId(), List.of(1));

            playerCopy.setPlants(playerCopy.getPlants() + 1);
            result.add(simulateCardWithInput(gameCopy, playerCopy, applicableCards, inputParameters, dataCollectContext));
        }

        {
            MarsGame gameCopy = new MarsGame(game);
            Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());

            Map<Integer, List<Integer>> inputParameters = Map.of(InputFlag.VIRAL_ENHANCERS_PUT_RESOURCE.getId(), List.of(
                    decisions.isMicrobeIsBetter()
                            ? decisions.getBestResourceCardPerType().get(InputRequirementType.MICROBE_INPUT).value.getId()
                            : decisions.getBestResourceCardPerType().get(InputRequirementType.ANIMAL_INPUT).value.getId()
            ));

            result.add(simulateCardWithInput(gameCopy, playerCopy, applicableCards, inputParameters, dataCollectContext));
        }

        return result;
    }

    private float[] simulateCardWithInput(MarsGame game, Player player, List<Card> applicableCards, Map<Integer, List<Integer>> inputParameters, DataCollectContext dataCollectContext) {
        int originalPlayerHandSize = player.getHand().getCards().size();

        MarsContext context = dataCollectContext.getContextProvider().provide(game, player);
        applicableCards.forEach(c -> c.postProjectBuiltEffect(context, new DummyCard(), inputParameters));

        dataCollectContext.getAiUtility().simulateDummyHandInsteadOfNewCards(player, originalPlayerHandSize);

        return dataCollectContext.getDataCollect().collectData(game, player);
    }


}
