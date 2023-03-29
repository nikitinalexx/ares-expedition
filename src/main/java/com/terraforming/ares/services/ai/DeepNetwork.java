package com.terraforming.ares.services.ai;

import com.terraforming.ares.dataset.DatasetCollectionService;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.dataset.MarsGameRow;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
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
    private final ThreadLocal<FeedForwardNetwork> firstNetwork;
    private final ThreadLocal<FeedForwardNetwork>  secondNetwork;
    private final WinPointsService winPointsService;
    private final CardService cardService;
    private final DatasetCollectionService datasetCollectionService;


    private static final float[] MAX_INPUTS_FIRST = new float[]{42.0f, 14.0f, 30.0f, 9.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 153.48f, 118.0f, 615.0f, 13.0f, 14.0f, 15.0f, 164.0f, 61.0f, 252.0f, 10.0f, 59.0f, 22.0f, 12.0f, 15.0f, 15.0f, 21.0f, 10.0f, 8.0f, 18.0f, 7.0f, 10.0f, 22.0f, 5.0f, 4.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 153.48f, 118.0f, 615.0f, 13.0f, 14.0f, 15.0f, 164.0f, 61.0f, 252.0f, 10.0f, 59.0f, 22.0f, 12.0f, 15.0f, 15.0f, 21.0f, 10.0f, 8.0f, 18.0f, 7.0f, 10.0f, 22.0f, 5.0f, 4.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};

    private static final float[] MAX_INPUTS_SECOND = new float[]{42.0f, 14.0f, 30.0f, 9.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 153.48f, 118.0f, 615.0f, 13.0f, 14.0f, 15.0f, 164.0f, 61.0f, 252.0f, 10.0f, 59.0f, 22.0f, 12.0f, 15.0f, 15.0f, 21.0f, 10.0f, 8.0f, 18.0f, 7.0f, 10.0f, 22.0f, 5.0f, 4.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 153.48f, 118.0f, 615.0f, 13.0f, 14.0f, 15.0f, 164.0f, 61.0f, 252.0f, 10.0f, 59.0f, 22.0f, 12.0f, 15.0f, 15.0f, 21.0f, 10.0f, 8.0f, 18.0f, 7.0f, 10.0f, 22.0f, 5.0f, 4.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};



    private static final Tensor MAX_INPUTS_TENSOR_FIRST = new Tensor(MAX_INPUTS_FIRST);
    private static final Tensor MAX_INPUTS_TENSOR_SECOND = new Tensor(MAX_INPUTS_SECOND);


    //the same, both very good
    public DeepNetwork(WinPointsService winPointsService, CardService cardService, DatasetCollectionService datasetCollectionService) throws IOException, ClassNotFoundException {
        this.winPointsService = winPointsService;
        this.cardService = cardService;
        this.datasetCollectionService = datasetCollectionService;
        firstNetwork = ThreadLocal.withInitial(() -> {
            try {
                return FileIO.createFromFile("first.dnet", FeedForwardNetwork.class);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        secondNetwork = ThreadLocal.withInitial(() -> {
            try {
                return FileIO.createFromFile("second.dnet", FeedForwardNetwork.class);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public float testState(MarsGame game, Player player) {
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        final MarsGameRow marsGameRow = datasetCollectionService.collectGameData(
                game,
                player,
                players.get(0) == player
                        ? players.get(1)
                        : players.get(0)
        );

        if (marsGameRow == null) {
            return 0.5f;
        }


        Tensor someInput = new Tensor(datasetCollectionService.getMarsGameRowForUse(marsGameRow));

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
        Tensor someInput = new Tensor(datasetCollectionService.getMarsGameRowForUse(row));
        if (networknumber == 1) {
            someInput.div(MAX_INPUTS_TENSOR_FIRST);
        } else {
            someInput.div(MAX_INPUTS_TENSOR_SECOND);
        }

        FeedForwardNetwork network = firstNetwork.get();
        if (networknumber == 2) {
            network = secondNetwork.get();
        }

        network.setInput(someInput);

        float[] output = network.getOutput();

        return output[0];
    }

}
