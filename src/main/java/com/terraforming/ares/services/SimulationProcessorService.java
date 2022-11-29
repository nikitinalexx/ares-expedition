package com.terraforming.ares.services;

import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.processors.turn.TurnProcessor;
import com.terraforming.ares.services.ai.AiService;
import com.terraforming.ares.services.dataset.DatasetCollectService;
import com.terraforming.ares.services.dataset.MarsGameDataset;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 26.11.2022
 */
@Service
public class SimulationProcessorService extends BaseProcessorService {
    private final AiService aiService;
    private final StateFactory stateFactory;
    private final WinPointsService winPointsService;
    private final CardService cardService;
    private final DatasetCollectService datasetCollectService;

    public SimulationProcessorService(List<TurnProcessor<?>> turnProcessor,
                                      TurnTypeService turnTypeService,
                                      StateFactory stateFactory,
                                      StateContextProvider stateContextProvider,
                                      AiService aiService, WinPointsService winPointsService, CardService cardService, DatasetCollectService datasetCollectService) {
        super(turnTypeService, stateFactory, stateContextProvider, turnProcessor);
        this.aiService = aiService;
        this.stateFactory = stateFactory;
        this.winPointsService = winPointsService;
        this.cardService = cardService;
        this.datasetCollectService = datasetCollectService;
    }

    public MarsGameDataset runSimulationWithDataset(MarsGame game) {
        MarsGameDataset dataSet = new MarsGameDataset(game.getPlayerUuidToPlayer());

        while (game.getStateType() != StateType.GAME_END) {
            while (aiService.waitingAiTurns(game)) {
                aiService.makeAiTurns(game);
                datasetCollectService.collect(game, dataSet);
            }

            while (processFinalTurns(game)) {
                stateFactory.getCurrentState(game).updateState();
            }
            datasetCollectService.collect(game, dataSet);
        }

        String winner = null;
        int bestPoints = 0;
        boolean singleWinner = false;

        for (Map.Entry<String, Player> entry : game.getPlayerUuidToPlayer().entrySet()) {
            int currentPoints = winPointsService.countWinPoints(entry.getValue(), game);

            if (winner == null) {
                winner = entry.getKey();
                bestPoints = currentPoints;
            } else {
                if (currentPoints != bestPoints) {
                    singleWinner = true;
                }
                if (currentPoints > bestPoints) {
                    winner = entry.getKey();
                }
            }
        }

        if (singleWinner) {
            dataSet.markWinner(winner);
            return dataSet;
        }

        return null;
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
