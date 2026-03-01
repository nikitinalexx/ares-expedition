package com.terraforming.ares.services;

import com.terraforming.ares.dataset.GameResult;
import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.processors.turn.TurnProcessor;
import com.terraforming.ares.services.ai.AiService;
import com.terraforming.ares.services.ai.advanced.IDataCollect;
import com.terraforming.ares.services.policyai.dto.GameRecordArena;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
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
    private final IDataCollect iDataCollect;
    private final CardPickStatistics cardPickStatistics;

    public SimulationProcessorService(List<TurnProcessor<?>> turnProcessor,
                                      TurnTypeService turnTypeService,
                                      StateFactory stateFactory,
                                      StateContextProvider stateContextProvider,
                                      AiService aiService, WinPointsService winPointsService,
                                      IDataCollect advancedAiDataCollectionService,
                                      CardPickStatistics cardPickStatistics) {
        super(turnTypeService, stateFactory, stateContextProvider, turnProcessor);
        this.aiService = aiService;
        this.stateFactory = stateFactory;
        this.winPointsService = winPointsService;
        this.iDataCollect = advancedAiDataCollectionService;
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
            iDataCollect.collectGameData(gameResult, game, players);
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

    public void runSimulationWithGameRecordArena(MarsGame game, GameRecordArena arena) {
        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());

        while (game.getStateType() != StateType.GAME_END) {
            while (aiService.waitingAiTurns(game)) {
                aiService.makeAiTurns(game);
            }

            while (processFinalTurns(game)) {
                stateFactory.getCurrentState(game).updateState();
            }
        }

        int firstPlayerPoints = winPointsService.countWinPoints(players.get(0), game);
        int secondPlayerPoints = winPointsService.countWinPoints(players.get(1), game);

        int winner = ((firstPlayerPoints == secondPlayerPoints) ? 0 : (firstPlayerPoints > secondPlayerPoints ? 1 : 2));

        finalizeGame(arena, winner);

        cardPickStatistics.collectFromGame(firstPlayerPoints == secondPlayerPoints ? 0 : (firstPlayerPoints > secondPlayerPoints ? 1 : 2), players);
    }

    public void finalizeGame(GameRecordArena arena, int winner) {
        float p1;
        float p2;

        if (winner == 0) {
            p1 = 0f;
            p2 = 0f;
        } else {
            p1 = winner == 1 ? 1f : -1f;
            p2 = -p1;
        }


        for (int i = 0; i < arena.size(); i++) {
            PolicyRecord r = arena.records()[i];

            r.outcome = r.isFirstPlayer ? p1 : p2;
        }

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

        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        int firstPlayerPoints = winPointsService.countWinPoints(players.get(0), game);
        int secondPlayerPoints = winPointsService.countWinPoints(players.get(1), game);
        cardPickStatistics.collectFromGame(firstPlayerPoints == secondPlayerPoints ? 0 : (firstPlayerPoints > secondPlayerPoints ? 1 : 2), players);
    }

}
