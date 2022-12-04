package com.terraforming.ares.services.ai;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.mars.MarsGameDataset;
import com.terraforming.ares.mars.MarsGameRow;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.WinPointsService;
import deepnetts.net.FeedForwardNetwork;
import deepnetts.util.FileIO;
import deepnetts.util.Tensor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 27.11.2022
 */
@Service
public class DeepNetwork {
//    private final FeedForwardNetwork marsNetwork;
//    private final WinPointsService winPointsService;
//
//    private static final float[] MAX_INPUTS = new float[]{48.0f, 182.0f, 146.0f, 812.0f, 10.0f, 11.0f, 15.0f, 169.0f, 49.0f, 299.0f, 8.0f, 24.0f, 53.0f, 14.0f, 30.0f, 9.0f, 167.0f, 146.0f, 812.0f, 10.0f, 11.0f, 15.0f, 169.0f, 49.0f, 299.0f, 8.0f, 53.0f};
//    private static final Tensor MAX_INPUTS_TENSOR = new Tensor(MAX_INPUTS);
//
//    public DeepNetwork(WinPointsService winPointsService) throws IOException, ClassNotFoundException {
//        this.winPointsService = winPointsService;
//        marsNetwork = FileIO.createFromFile("marsNet.dnet", FeedForwardNetwork.class);
//    }

    public float testState(MarsGame game, Player player) {
//        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
//        final MarsGameRow marsGameRow = MarsGameDataset.collectPlayerData(
//                winPointsService,
//                game,
//                player,
//                players.get(0) == player
//                        ? players.get(1)
//                        : players.get(0)
//        );
//
//        Tensor someInput = new Tensor(marsGameRow.getAsInput());
//        someInput.div(MAX_INPUTS_TENSOR);
//
//        marsNetwork.setInput(someInput);
//
//        float[] output = marsNetwork.getOutput();
//
//        return output[0];
        return 0.5f;
    }

}
