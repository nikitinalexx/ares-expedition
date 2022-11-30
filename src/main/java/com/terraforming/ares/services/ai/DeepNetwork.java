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
    private final FeedForwardNetwork marsNetworkWithTags;
    private final DatasetCollectService datasetCollectService;
    private static final float[] MAX_INPUTS_NO_TAGS = new float[]{58.0f, 195.0f, 59.0f, 459.0f, 11.0f, 10.0f, 27.0f, 236.0f, 43.0f, 263.0f, 9.0f, 35.0f, 58.0f, 4.0f, 4.0f, 22.666666f, 14.0f, 30.0f, 9.0f, 195.0f, 59.0f, 459.0f, 11.0f, 10.0f, 27.0f, 236.0f, 43.0f, 263.0f, 9.0f, 58.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};

    private static final float[] MAX_INPUTS_WITH_TAGS = new float[]{67.0f, 206.0f, 88.0f, 428.0f, 9.0f, 9.0f, 21.0f, 350.0f, 44.0f, 241.0f, 10.0f, 25.0f, 65.0f, 17.0f, 13.0f, 20.0f, 18.0f, 12.0f, 12.0f, 23.0f, 6.0f, 7.0f, 12.0f, 4.0f, 5.0f, 27.666666f, 14.0f, 30.0f, 9.0f, 206.0f, 88.0f, 428.0f, 9.0f, 9.0f, 21.0f, 350.0f, 44.0f, 241.0f, 10.0f, 65.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
    private static final Tensor MAX_INPUTS_TENSOR_WITH_TAGS = new Tensor(MAX_INPUTS_WITH_TAGS);

    public DeepNetwork(DatasetCollectService datasetCollectService) throws IOException, ClassNotFoundException {
        this.datasetCollectService = datasetCollectService;
        marsNetworkWithTags = FileIO.createFromFile("marsNet-tags.dnet", FeedForwardNetwork.class);
    }

    public float testState(MarsGame game, Player player) {
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        final MarsGameRow marsGameRow = datasetCollectService.collectPlayerDataWithTags(
                game,
                player,
                players.get(0) == player
                        ? players.get(1)
                        : players.get(0)
        );

        Tensor someInput = new Tensor(marsGameRow.getAsInput());
        someInput.div(MAX_INPUTS_TENSOR_WITH_TAGS);

        marsNetworkWithTags.setInput(someInput);

        float[] output = marsNetworkWithTags.getOutput();

        //System.out.println(output[0]);

        return output[0];

    }

    public float testState(MarsGameRow row) {
        Tensor someInput = new Tensor(row.getAsInput());
        someInput.div(MAX_INPUTS_TENSOR_WITH_TAGS);

        marsNetworkWithTags.setInput(someInput);

        float[] output = marsNetworkWithTags.getOutput();

        return output[0];
    }


}
