package com.terraforming.ares.services.ai.network2.buildParams;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;

import java.util.List;

public class OxygenStep implements DecisionStep {

    private final MarsGame game;
    private final Player player;
    private final boolean requiresOxygenCheck;

    public OxygenStep(
            MarsGame game,
            Player player,
            boolean requiresOxygenCheck
    ) {
        this.game = game;
        this.player = player;
        this.requiresOxygenCheck = requiresOxygenCheck;
    }

    @Override
    public boolean isApplicable() {
        return requiresOxygenCheck && !game.getPlanetAtTheStartOfThePhase().isOxygenMax();
    }

    @Override
    public void collectSimulations(List<float[]> simulations, DataCollectContext dataCollectContext) {
        simulations.add(getOxygenCheck(game, player, dataCollectContext));
    }

    @Override
    public void applyPredictions(PredictionCursor cursor, OptimizedInputDecisions decisions) {
        decisions.setOxygenPrediction(cursor.next());
    }

    private float[] getOxygenCheck(MarsGame game, Player player, DataCollectContext dataCollectContext) {
        game = new MarsGame(game);
        player = game.getPlayerByUuid(player.getUuid());

        game.getPlanet().increaseOxygen();
        game.getPlanetAtTheStartOfThePhase().increaseOxygen();
        player.setTerraformingRating(player.getTerraformingRating() + 1);

        return dataCollectContext.getDataCollect().collectData(game, player);
    }

}

