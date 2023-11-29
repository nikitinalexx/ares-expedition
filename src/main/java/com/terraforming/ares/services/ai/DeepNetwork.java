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

    private static final float[] MAX_INPUTS_FIRST = new float[]{45.0f, 14.0f, 38.0f, 9.0f, 223.0f, 171.0f, 659.0f, 13.0f, 14.0f, 21.0f, 101.0f, 64.0f, 360.0f, 10.0f, 56.0f, 10.0f, 4.0f, 36.0f, 21.0f, 18.0f, 16.0f, 6.0f, 26.0f, 5.0f, 11.0f, 5.0f, 28.0f, 17.0f, 22.0f, 20.0f, 11.0f, 17.0f, 34.0f, 5.0f, 11.0f, 9.0f, 1.0f, 2.0f, 2.0f, 3.0f, 7.0f, 4.0f, 3.0f, 2.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 5.0f, 5.0f, 5.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 223.0f, 171.0f, 659.0f, 13.0f, 14.0f, 21.0f, 101.0f, 64.0f, 360.0f, 10.0f, 56.0f, 10.0f, 4.0f, 36.0f, 21.0f, 18.0f, 16.0f, 6.0f, 26.0f, 5.0f, 11.0f, 5.0f, 28.0f, 17.0f, 22.0f, 20.0f, 11.0f, 17.0f, 34.0f, 5.0f, 11.0f, 9.0f, 1.0f, 2.0f, 2.0f, 3.0f, 7.0f, 4.0f, 3.0f, 2.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 5.0f, 5.0f, 5.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.78f, 1.38f, 1.74f, 1.0f, 1.0f, 1.0f, 1.0f};
    private static final float[] MAX_INPUTS_SECOND = new float[]{45.0f, 14.0f, 38.0f, 9.0f, 223.0f, 171.0f, 659.0f, 13.0f, 14.0f, 21.0f, 101.0f, 64.0f, 360.0f, 10.0f, 56.0f, 10.0f, 4.0f, 36.0f, 21.0f, 18.0f, 16.0f, 6.0f, 26.0f, 5.0f, 11.0f, 5.0f, 28.0f, 17.0f, 22.0f, 20.0f, 11.0f, 17.0f, 34.0f, 5.0f, 11.0f, 9.0f, 1.0f, 2.0f, 2.0f, 3.0f, 7.0f, 4.0f, 3.0f, 2.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 5.0f, 5.0f, 5.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 223.0f, 171.0f, 659.0f, 13.0f, 14.0f, 21.0f, 101.0f, 64.0f, 360.0f, 10.0f, 56.0f, 10.0f, 4.0f, 36.0f, 21.0f, 18.0f, 16.0f, 6.0f, 26.0f, 5.0f, 11.0f, 5.0f, 28.0f, 17.0f, 22.0f, 20.0f, 11.0f, 17.0f, 34.0f, 5.0f, 11.0f, 9.0f, 1.0f, 2.0f, 2.0f, 3.0f, 7.0f, 4.0f, 3.0f, 2.0f, 1.0f, 1.0f, 1.0f, 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 5.0f, 5.0f, 5.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.78f, 1.38f, 1.74f, 1.0f, 1.0f, 1.0f, 1.0f};

    private static final DataColumn MAX_INPUTS_DATA_FIRST = new DataColumn(MAX_INPUTS_FIRST);
    private static final DataColumn MAX_INPUTS_DATA_SECOND = new DataColumn(MAX_INPUTS_SECOND);


    private final ThreadLocal<Network> firstNetwork;
    private final ThreadLocal<Network> secondNetwork;

    //the same, both very good
    public DeepNetwork(DatasetCollectionService datasetCollectionService) throws IOException, ClassNotFoundException {
        this.datasetCollectionService = datasetCollectionService;

        firstNetwork = ThreadLocal.withInitial(() -> {
            try {
                return initNetworkFromFile("1701242492845_epoch1_result_N_reg4_smart_1.dnet_epoch_1.txt");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        secondNetwork = ThreadLocal.withInitial(() -> {
            try {
//                return initNetworkFromFile("second/26.11.2023_filtered__epoch_8.txt");
                return initNetworkFromFile("1701242492845_epoch1_result_N_reg4_smart_1.dnet_epoch_1.txt");

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void updateNetwork(String file, int network) throws IOException {
        if (network == 1) {
            firstNetwork.set(initNetworkFromFile(file));
        } else {
            secondNetwork.set(initNetworkFromFile(file));
        }
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
        return testState(game, player, player.isFirstBot() ? 1 : 2);
    }

    public float testState(MarsGame game, Player player, int networkId) {
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

        if (networkId == 1) {
            someInput.div(MAX_INPUTS_DATA_FIRST);
        } else {
            someInput.div(MAX_INPUTS_DATA_SECOND);
        }

        Network network = (networkId == 1)
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
