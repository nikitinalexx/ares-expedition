package com.terraforming.ares.services.ai;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.dataset.DatasetCollectService;
import com.terraforming.ares.services.dataset.MarsGameRow;
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
    private final FeedForwardNetwork marsNetwork;
    private final DatasetCollectService datasetCollectService;

    private static final float[] MAX_INPUTS = new float[]{65.0f, 241.0f, 126.0f, 924.0f, 10.0f, 12.0f, 26.0f, 161.0f, 87.0f, 718.0f, 8.0f, 32.0f, 69.0f, 14.0f, 30.0f, 9.0f, 238.0f, 126.0f, 924.0f, 10.0f, 12.0f, 26.0f, 161.0f, 87.0f, 718.0f, 8.0f, 69.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f};
    private static final Tensor MAX_INPUTS_TENSOR = new Tensor(MAX_INPUTS);

    public DeepNetwork(DatasetCollectService datasetCollectService) throws IOException, ClassNotFoundException {
        this.datasetCollectService = datasetCollectService;
        marsNetwork = FileIO.createFromFile("marsNet.dnet", FeedForwardNetwork.class);
    }

    public float testState(MarsGame game, Player player) {
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        final MarsGameRow marsGameRow = datasetCollectService.collectPlayerData(
                game,
                player,
                players.get(0) == player
                        ? players.get(1)
                        : players.get(0)
        );

        Tensor someInput = new Tensor(marsGameRow.getAsInput());
        someInput.div(MAX_INPUTS_TENSOR);

        marsNetwork.setInput(someInput);

        float[] output = marsNetwork.getOutput();

        //System.out.println(output[0]);

        return output[0];

    }

    public float testState(MarsGameRow row) {
        Tensor someInput = new Tensor(row.getAsInput());
        someInput.div(MAX_INPUTS_TENSOR);

        marsNetwork.setInput(someInput);

        float[] output = marsNetwork.getOutput();

        return output[0];
    }


}
