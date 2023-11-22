package com.terraforming.ares.services.ai;

import com.terraforming.ares.dataset.DatasetCollectionService;
import com.terraforming.ares.dataset.MarsGameRow;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.network.DataColumn;
import com.terraforming.ares.services.ai.network.Network;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 27.11.2022
 */
@Service
public class DeepNetwork {
    private final DatasetCollectionService datasetCollectionService;

    private static final float[] MAX_INPUTS_FIRST = new float[]{45.0f, 14.0f, 38.0f, 9.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 258.0f, 196.0f, 1013.0f, 16.0f, 17.0f, 34.0f, 255.0f, 73.0f, 346.0f, 16.0f, 66.0f, 25.0f, 4.0f, 38.0f, 25.0f, 28.0f, 16.0f, 19.0f, 17.0f, 10.0f, 24.0f, 8.0f, 15.0f, 5.0f, 24.0f, 13.0f, 1.0f, 2.0f, 2.0f, 3.0f, 7.0f, 4.0f, 3.0f, 2.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 5.0f, 5.0f, 5.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 258.0f, 196.0f, 1013.0f, 16.0f, 17.0f, 34.0f, 255.0f, 73.0f, 346.0f, 16.0f, 66.0f, 25.0f, 4.0f, 38.0f, 25.0f, 28.0f, 16.0f, 19.0f, 17.0f, 10.0f, 24.0f, 8.0f, 15.0f, 5.0f, 24.0f, 13.0f, 1.0f, 2.0f, 2.0f, 3.0f, 7.0f, 4.0f, 3.0f, 2.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 5.0f, 5.0f, 5.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 2.58f, 1.88f, 1.68f, 1.0f, 1.0f, 1.0667f};
    private static final float[] MAX_INPUTS_SECOND = new float[]{45.0f, 14.0f, 38.0f, 9.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 258.0f, 196.0f, 1013.0f, 16.0f, 17.0f, 34.0f, 255.0f, 73.0f, 346.0f, 16.0f, 66.0f, 25.0f, 4.0f, 38.0f, 25.0f, 28.0f, 16.0f, 19.0f, 17.0f, 10.0f, 24.0f, 8.0f, 15.0f, 5.0f, 24.0f, 13.0f, 1.0f, 2.0f, 2.0f, 3.0f, 7.0f, 4.0f, 3.0f, 2.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 5.0f, 5.0f, 5.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 258.0f, 196.0f, 1013.0f, 16.0f, 17.0f, 34.0f, 255.0f, 73.0f, 346.0f, 16.0f, 66.0f, 25.0f, 4.0f, 38.0f, 25.0f, 28.0f, 16.0f, 19.0f, 17.0f, 10.0f, 24.0f, 8.0f, 15.0f, 5.0f, 24.0f, 13.0f, 1.0f, 2.0f, 2.0f, 3.0f, 7.0f, 4.0f, 3.0f, 2.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 5.0f, 5.0f, 5.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 2.58f, 1.88f, 1.68f, 1.0f, 1.0f, 1.0667f};

    private static final DataColumn MAX_INPUTS_DATA_FIRST = new DataColumn(MAX_INPUTS_FIRST);
    private static final DataColumn MAX_INPUTS_DATA_SECOND = new DataColumn(MAX_INPUTS_SECOND);


    private final ThreadLocal<Network> firstNetwork;
    private final ThreadLocal<Network> secondNetwork;

    //the same, both very good
    public DeepNetwork(DatasetCollectionService datasetCollectionService) throws IOException, ClassNotFoundException {
        this.datasetCollectionService = datasetCollectionService;

        firstNetwork = ThreadLocal.withInitial(() -> {
            try {
                return initNetworkFromFile("second/result_N_reg_5_smart_net.txt");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        secondNetwork = ThreadLocal.withInitial(() -> {
            try {
                return initNetworkFromFile("second/result_N_reg_5_smart_net.txt");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Network initNetworkFromFile(String fileName) throws IOException {
        int networkSize;

        int firstLayerCols;
        int firstLayerRows;
        float[] firstValues;
        float[] firstBiases;

        int secondLayerCols;
        int secondLayerRows;
        float[] secondValues;
        float[] secondBiases;

        try (FileReader fr = new FileReader(fileName);
             BufferedReader reader = new BufferedReader(fr)) {
            networkSize = Integer.parseInt(reader.readLine());
            int totalLayers = Integer.parseInt(reader.readLine());

            if (totalLayers != 3) {
                throw new IllegalArgumentException("Unable to create network of size 3 from file");
            }

            firstLayerCols = Integer.parseInt(reader.readLine());
            firstLayerRows = Integer.parseInt(reader.readLine());
            firstValues = readFloatArrayFromLine(reader.readLine());
            firstBiases = readFloatArrayFromLine(reader.readLine());

            secondLayerCols = Integer.parseInt(reader.readLine());
            secondLayerRows = Integer.parseInt(reader.readLine());
            secondValues = readFloatArrayFromLine(reader.readLine());
            secondBiases = readFloatArrayFromLine(reader.readLine());

            return new Network(
                    networkSize,
                    new DataColumn(
                            firstLayerCols,
                            firstLayerRows,
                            firstValues
                    ), firstBiases,
                    new DataColumn(
                            secondLayerCols,
                            secondLayerRows,
                            secondValues
                    ),
                    secondBiases
            );
        }
    }

    private float[] readFloatArrayFromLine(String line) {
        line = line.replaceAll("\\[", "").replaceAll("\\]", "");

        String[] floatStrings = line.split(",");
        float[] result = new float[floatStrings.length];

        for (int i = 0; i < floatStrings.length; i++) {
            result[i] = Float.parseFloat(floatStrings[i]);
        }
        return result;

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


        DataColumn someInput = new DataColumn(datasetCollectionService.mapMarsGameToArrayForUse(marsGameRow));

        if (player.isFirstBot()) {
            someInput.div(MAX_INPUTS_DATA_FIRST);
        } else {
            someInput.div(MAX_INPUTS_DATA_SECOND);
        }

        Network network = player.isFirstBot()
                ? firstNetwork.get()
                : secondNetwork.get();


        network.setInput(someInput);


        return network.getOutput()[0];
    }

    public float testState(MarsGameRow row, int networknumber) {
        DataColumn someInput = new DataColumn(datasetCollectionService.mapMarsGameToArrayForUse(row));

        if (networknumber == 1) {
            someInput.div(MAX_INPUTS_DATA_FIRST);
        } else {
            someInput.div(MAX_INPUTS_DATA_SECOND);
        }

        Network network = (networknumber == 1) ? firstNetwork.get() : secondNetwork.get();

        network.setInput(someInput);

        float[] output = network.getOutput();

        return output[0];
    }

}
