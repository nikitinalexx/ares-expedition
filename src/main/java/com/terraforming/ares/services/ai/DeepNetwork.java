package com.terraforming.ares.services.ai;

import com.terraforming.ares.dataset.DatasetCollectionService;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.dataset.MarsGameRow;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.WinPointsService;
import deepnetts.data.norm.MinMaxScaler;
import deepnetts.net.FeedForwardNetwork;
import deepnetts.util.FileIO;
import deepnetts.util.Tensor;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
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

    private static final float[] MAX_INPUTS_FIRST = new float[]{48.0f, 14.0f, 30.0f, 9.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 187.98f, 127.0f, 501.0f, 12.0f, 10.0f, 24.0f, 159.0f, 49.0f, 205.0f, 9.0f, 56.0f, 22.0f, 12.0f, 14.0f, 23.0f, 34.0f, 31.0f, 11.0f, 31.0f, 13.0f, 11.0f, 19.0f, 5.0f, 4.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 187.98f, 127.0f, 501.0f, 12.0f, 10.0f, 24.0f, 159.0f, 49.0f, 205.0f, 9.0f, 56.0f, 22.0f, 12.0f, 14.0f, 23.0f, 34.0f, 31.0f, 11.0f, 31.0f, 13.0f, 11.0f, 19.0f, 5.0f, 4.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 2.7f, 1.66f, 1.42f, 1.0f, 1.0f, 1.0f};
    private static final float[] MAX_INPUTS_SECOND = new float[]{48.0f, 14.0f, 30.0f, 9.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 187.98f, 127.0f, 501.0f, 12.0f, 10.0f, 24.0f, 159.0f, 49.0f, 205.0f, 9.0f, 56.0f, 22.0f, 12.0f, 14.0f, 23.0f, 34.0f, 31.0f, 11.0f, 31.0f, 13.0f, 11.0f, 19.0f, 5.0f, 4.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 187.98f, 127.0f, 501.0f, 12.0f, 10.0f, 24.0f, 159.0f, 49.0f, 205.0f, 9.0f, 56.0f, 22.0f, 12.0f, 14.0f, 23.0f, 34.0f, 31.0f, 11.0f, 31.0f, 13.0f, 11.0f, 19.0f, 5.0f, 4.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 2.7f, 1.66f, 1.42f, 1.0f, 1.0f, 1.0f};



    private static final Tensor MAX_INPUTS_TENSOR_FIRST = new Tensor(MAX_INPUTS_FIRST);
    private static final Tensor MAX_INPUTS_TENSOR_SECOND = new Tensor(MAX_INPUTS_SECOND);


    //the same, both very good
    public DeepNetwork(WinPointsService winPointsService, CardService cardService, DatasetCollectionService datasetCollectionService) throws IOException, ClassNotFoundException {
        this.winPointsService = winPointsService;
        this.cardService = cardService;
        this.datasetCollectionService = datasetCollectionService;
        firstNetwork = ThreadLocal.withInitial(() -> {
            try {
                //result0 0.6099290780141844
                //result1 0.5524475524475524
                //result2 0.5909090909090909
                //result3 0.5950704225352113

                return FileIO.createFromFile("result_N^2_reg_5.dnet", FeedForwardNetwork.class);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        secondNetwork = ThreadLocal.withInitial(() -> {
            try {
                return FileIO.createFromFile("result_N^2_reg_5.dnet", FeedForwardNetwork.class);
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

        FeedForwardNetwork network =(networknumber == 1) ? firstNetwork.get() : secondNetwork.get();

        network.setInput(someInput);

        float[] output = network.getOutput();

        return output[0];
    }

    public static MinMaxScaler readObjectFromFile(String fileName) {
        MinMaxScaler obj = null;
        try {
            FileInputStream fileIn = new FileInputStream(fileName);
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);
            obj = (MinMaxScaler) objectIn.readObject();
            objectIn.close();
            fileIn.close();
            System.out.println("Object deserialized from file.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }

}
