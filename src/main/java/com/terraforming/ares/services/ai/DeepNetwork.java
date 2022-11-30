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
    private final FeedForwardNetwork marsNetworkNoTags;
    private final DatasetCollectService datasetCollectService;
    private static final float[] MAX_INPUTS_NO_TAGS = new float[]{59.0f, 193.0f, 58.0f, 790.0f, 11.0f, 10.0f, 24.0f, 170.0f, 49.0f, 370.0f, 9.0f, 30.0f, 63.0f, 27.0f, 4.0f, 5.0f, 26.666666f, 14.0f, 30.0f, 9.0f, 193.0f, 58.0f, 790.0f, 11.0f, 10.0f, 24.0f, 170.0f, 49.0f, 370.0f, 9.0f, 63.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
    private static final Tensor MAX_INPUTS_TENSOR_NO_TAGS = new Tensor(MAX_INPUTS_NO_TAGS);

    public DeepNetwork(DatasetCollectService datasetCollectService) throws IOException, ClassNotFoundException {
        this.datasetCollectService = datasetCollectService;
        marsNetworkNoTags = FileIO.createFromFile("marsNet-no-tags.dnet", FeedForwardNetwork.class);
    }

    public float testState(MarsGame game, Player player) {
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        final MarsGameRow marsGameRow = datasetCollectService.collectPlayerDataWithoutTags(
                game,
                player,
                players.get(0) == player
                        ? players.get(1)
                        : players.get(0)
        );

        Tensor someInput = new Tensor(marsGameRow.getAsInput());
        someInput.div(MAX_INPUTS_TENSOR_NO_TAGS);

        marsNetworkNoTags.setInput(someInput);

        float[] output = marsNetworkNoTags.getOutput();

        //System.out.println(output[0]);

        return output[0];

    }

    public float testState(MarsGameRow row) {
        Tensor someInput = new Tensor(row.getAsInput());
        someInput.div(MAX_INPUTS_TENSOR_NO_TAGS);

        marsNetworkNoTags.setInput(someInput);

        float[] output = marsNetworkNoTags.getOutput();

        return output[0];
    }


}
