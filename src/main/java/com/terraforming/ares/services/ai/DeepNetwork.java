package com.terraforming.ares.services.ai;

import com.terraforming.ares.dataset.DatasetCollectionService;
import com.terraforming.ares.dataset.MarsGameRow;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
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

//    public float testState(MarsGame game, Player player) {
//        return 0;
//    }
//
//    public float testState(MarsGameRow row, int networknumber) {
//        return 0;
//    }

    private final ThreadLocal<FeedForwardNetwork> firstNetwork;
    private final ThreadLocal<FeedForwardNetwork> secondNetwork;
    private final DatasetCollectionService datasetCollectionService;

    private static final float[] MAX_INPUTS_FIRST = new float[]{44.0f, 14.0f, 38.0f, 9.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 224.5f, 175.0f, 766.0f, 14.0f, 14.0f, 19.0f, 226.0f, 62.0f, 357.0f, 9.0f, 58.0f, 18.0f, 4.0f, 30.0f, 23.0f, 27.0f, 19.0f, 21.0f, 18.0f, 8.0f, 30.0f, 7.0f, 11.0f, 5.0f, 20.0f, 14.0f, 1.0f, 2.0f, 2.0f, 3.0f, 7.0f, 4.0f, 3.0f, 2.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 5.0f, 5.0f, 5.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 224.5f, 175.0f, 766.0f, 14.0f, 14.0f, 19.0f, 226.0f, 62.0f, 357.0f, 9.0f, 58.0f, 18.0f, 4.0f, 30.0f, 23.0f, 27.0f, 19.0f, 21.0f, 18.0f, 8.0f, 30.0f, 7.0f, 11.0f, 5.0f, 20.0f, 14.0f, 1.0f, 2.0f, 2.0f, 3.0f, 7.0f, 4.0f, 3.0f, 2.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 5.0f, 5.0f, 5.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.54f, 1.24f, 1.2f, 1.0f, 1.0667f, 1.0667f};
    private static final float[] MAX_INPUTS_SECOND = new float[]{44.0f, 14.0f, 38.0f, 9.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 224.5f, 175.0f, 766.0f, 14.0f, 14.0f, 19.0f, 226.0f, 62.0f, 357.0f, 9.0f, 58.0f, 18.0f, 4.0f, 30.0f, 23.0f, 27.0f, 19.0f, 21.0f, 18.0f, 8.0f, 30.0f, 7.0f, 11.0f, 5.0f, 20.0f, 14.0f, 1.0f, 2.0f, 2.0f, 3.0f, 7.0f, 4.0f, 3.0f, 2.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 5.0f, 5.0f, 5.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 224.5f, 175.0f, 766.0f, 14.0f, 14.0f, 19.0f, 226.0f, 62.0f, 357.0f, 9.0f, 58.0f, 18.0f, 4.0f, 30.0f, 23.0f, 27.0f, 19.0f, 21.0f, 18.0f, 8.0f, 30.0f, 7.0f, 11.0f, 5.0f, 20.0f, 14.0f, 1.0f, 2.0f, 2.0f, 3.0f, 7.0f, 4.0f, 3.0f, 2.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 5.0f, 5.0f, 5.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.54f, 1.24f, 1.2f, 1.0f, 1.0667f, 1.0667f};

    private static final Tensor MAX_INPUTS_TENSOR_FIRST = new Tensor(MAX_INPUTS_FIRST);
    private static final Tensor MAX_INPUTS_TENSOR_SECOND = new Tensor(MAX_INPUTS_SECOND);

    //the same, both very good
    public DeepNetwork(DatasetCollectionService datasetCollectionService) throws IOException, ClassNotFoundException {
        this.datasetCollectionService = datasetCollectionService;
        firstNetwork = ThreadLocal.withInitial(() -> {
            try {
                return FileIO.createFromFile("result_N_reg_5.dnet", FeedForwardNetwork.class);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        secondNetwork = ThreadLocal.withInitial(() -> {
            try {
                return FileIO.createFromFile("result_N_reg_5.dnet", FeedForwardNetwork.class);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public float testState(MarsGame game, Player player) {
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        final MarsGameRow marsGameRow = datasetCollectionService.collectGameAndPlayers(
                game,
                player,
                players.get(0) == player
                        ? players.get(1)
                        : players.get(0)
        );

        if (marsGameRow == null) {
            return 0.5f;
        }


        Tensor someInput = new Tensor(datasetCollectionService.mapMarsGameToArrayForUse(marsGameRow));

        if (player.isFirstBot()) {
            someInput.div(MAX_INPUTS_TENSOR_FIRST);
        } else {
            someInput.div(MAX_INPUTS_TENSOR_SECOND);
        }


        FeedForwardNetwork network = player.isFirstBot()
                ? firstNetwork.get()
                : secondNetwork.get();

        network.setInput(someInput);

        return network.getOutput()[0];
    }

    public float testState(MarsGameRow row, int networknumber) {
        Tensor someInput = new Tensor(datasetCollectionService.mapMarsGameToArrayForUse(row));

        if (networknumber == 1) {
            someInput.div(MAX_INPUTS_TENSOR_FIRST);
        } else {
            someInput.div(MAX_INPUTS_TENSOR_SECOND);
        }

        FeedForwardNetwork network = (networknumber == 1) ? firstNetwork.get() : secondNetwork.get();

        network.setInput(someInput);

        float[] output = network.getOutput();

        return output[0];
    }

}
