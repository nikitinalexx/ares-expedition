package com.terraforming.ares.services;

import com.terraforming.ares.dataset.GameResult;
import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.processors.turn.TurnProcessor;
import com.terraforming.ares.services.ai.AiService;
import com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService;
import com.terraforming.ares.services.simulations.CardPickStatistics;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.11.2022
 */
@Service
public class SimulationProcessorService extends BaseProcessorService {
    private final AiService aiService;
    private final StateFactory stateFactory;
    private final WinPointsService winPointsService;
    private final AdvancedAiDataCollectionService advancedAiDataCollectionService;
    private final CardPickStatistics cardPickStatistics;

    public SimulationProcessorService(List<TurnProcessor<?>> turnProcessor,
                                      TurnTypeService turnTypeService,
                                      StateFactory stateFactory,
                                      StateContextProvider stateContextProvider,
                                      AiService aiService, WinPointsService winPointsService,
                                      AdvancedAiDataCollectionService advancedAiDataCollectionService,
                                      CardPickStatistics cardPickStatistics) {
        super(turnTypeService, stateFactory, stateContextProvider, turnProcessor);
        this.aiService = aiService;
        this.stateFactory = stateFactory;
        this.winPointsService = winPointsService;
        this.advancedAiDataCollectionService = advancedAiDataCollectionService;
        this.cardPickStatistics = cardPickStatistics;
    }

    public GameResult runSimulationWithDataset(MarsGame game) {
        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());

        GameResult gameResult = new GameResult();

        while (game.getStateType() != StateType.GAME_END) {
            while (aiService.waitingAiTurns(game)) {
                aiService.makeAiTurns(game);
            }

            while (processFinalTurns(game)) {
                stateFactory.getCurrentState(game).updateState();
            }
            advancedAiDataCollectionService.collectData(gameResult, game, players);
        }

        int firstPlayerPoints = winPointsService.countWinPoints(players.get(0), game);
        int secondPlayerPoints = winPointsService.countWinPoints(players.get(1), game);

        if (firstPlayerPoints == secondPlayerPoints) {
            gameResult.markDraw();
        } else {
            gameResult.markWinner(firstPlayerPoints > secondPlayerPoints ? 1 : 2);
        }

        cardPickStatistics.collectFromGame(firstPlayerPoints == secondPlayerPoints ? 0 : (firstPlayerPoints > secondPlayerPoints ? 1 : 2), players);

        return gameResult;
    }

    public void processSimulation(MarsGame game) {
        processSimulation(game, null);
    }

    /**
     * @param afterStep optional callback invoked after every processed step (used by invariant tests and by the dataset
     *                  recorder). It fires after EVERY AI step, not once per round: the owner logged a post-state after
     *                  each blue action and each discard of phase 5, which is 300-400 states per player per game. A hook
     *                  outside the inner loop collapses them into one per round (measured: 33 per player), and that is a
     *                  different training corpus at the same game count.
     */
    public void processSimulation(MarsGame game, java.util.function.Consumer<MarsGame> afterStep) {
        while (game.getStateType() != StateType.GAME_END) {
            while (aiService.waitingAiTurns(game)) {
                aiService.makeAiTurns(game);
                if (afterStep != null) {
                    afterStep.accept(game);
                }
            }

            while (processFinalTurns(game)) {
                stateFactory.getCurrentState(game).updateState();
                if (afterStep != null) {
                    afterStep.accept(game);
                }
            }
            if (afterStep != null) {
                afterStep.accept(game);
            }
        }

        // The owner's end-game check (22/11): who raised the last parameter, and did that player lose? Logged only when
        // -Dares.endaudit=true, and only when the game was ended while behind.
        EndGameAudit.check(game, winPointsService);

        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        int firstPlayerPoints = winPointsService.countWinPoints(players.get(0), game);
        int secondPlayerPoints = winPointsService.countWinPoints(players.get(1), game);        cardPickStatistics.collectFromGame(firstPlayerPoints == secondPlayerPoints ? 0 : (firstPlayerPoints > secondPlayerPoints ? 1 : 2), players);
    }

}
