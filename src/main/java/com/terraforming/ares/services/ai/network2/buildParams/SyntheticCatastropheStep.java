package com.terraforming.ares.services.ai.network2.buildParams;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

public class SyntheticCatastropheStep implements DecisionStep {

    private final MarsGame game;
    private final Player player;
    private final List<Integer> redCardsToCheck;

    public SyntheticCatastropheStep(
            MarsGame game,
            Player player,
            List<Integer> redCardsToCheck
    ) {
        this.game = game;
        this.player = player;
        this.redCardsToCheck = redCardsToCheck;
    }

    @Override
    public boolean isApplicable() {
        return CollectionUtils.isNotEmpty(redCardsToCheck);
    }

    @Override
    public void collectSimulations(List<float[]> simulations, DataCollectContext dataCollectContext) {
        MarsGame gameCopy = new MarsGame(game);
        Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());
        playerCopy.getPlayed().addCard(AiConstants.SYNTHETIC_CATASTROPHY_CARD_ID);
        playerCopy.getHand().removeCard(AiConstants.SYNTHETIC_CATASTROPHY_CARD_ID);

        for (Integer redCardToCheck : redCardsToCheck) {
            playerCopy.getPlayed().removeCard(redCardToCheck);
            playerCopy.getHand().addCard(redCardToCheck);

            simulations.add(dataCollectContext.getDataCollect().collectData(gameCopy, playerCopy));

            playerCopy.getPlayed().addCard(redCardToCheck);
            playerCopy.getHand().removeCard(redCardToCheck);
        }
    }

    @Override
    public void applyPredictions(PredictionCursor cursor, OptimizedInputDecisions decisions) {
        List<Prediction> predictions = cursor.next(redCardsToCheck.size());
        Prediction firstPrediction = predictions.getFirst();
        Integer bestCard = redCardsToCheck.getFirst();
        double bestChance = firstPrediction.baseProb;

        for (int i = 1; i < redCardsToCheck.size(); i++) {
            Integer redCard = redCardsToCheck.get(i);
            Prediction prediction = predictions.get(i);

            if (prediction.baseProb > bestChance) {
                bestCard = redCard;
                bestChance = prediction.baseProb;
            }
        }
        decisions.setBestRedCardToRollback(bestCard);
    }

}

