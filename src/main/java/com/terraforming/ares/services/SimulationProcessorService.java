package com.terraforming.ares.services;

import com.terraforming.ares.dataset.GameResult;
import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.processors.turn.TurnProcessor;
import com.terraforming.ares.services.ai.AiService;
import com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService;
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

    public SimulationProcessorService(List<TurnProcessor<?>> turnProcessor,
                                      TurnTypeService turnTypeService,
                                      StateFactory stateFactory,
                                      StateContextProvider stateContextProvider,
                                      AiService aiService, WinPointsService winPointsService,
                                      AdvancedAiDataCollectionService advancedAiDataCollectionService) {
        super(turnTypeService, stateFactory, stateContextProvider, turnProcessor);
        this.aiService = aiService;
        this.stateFactory = stateFactory;
        this.winPointsService = winPointsService;
        this.advancedAiDataCollectionService = advancedAiDataCollectionService;
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
        int secondPlayerPoints = winPointsService.countWinPoints(players.get(0), game);

        if (firstPlayerPoints == secondPlayerPoints) {
            gameResult.markDraw();
        } else {
            gameResult.markWinner(firstPlayerPoints > secondPlayerPoints ? 1 : 2);
        }

        return gameResult;
    }

    public void processSimulation(MarsGame game) {
        while (game.getStateType() != StateType.GAME_END) {
            while (aiService.waitingAiTurns(game)) {
                aiService.makeAiTurns(game);
            }

            while (processFinalTurns(game)) {
                stateFactory.getCurrentState(game).updateState();
            }
        }
    }

}
