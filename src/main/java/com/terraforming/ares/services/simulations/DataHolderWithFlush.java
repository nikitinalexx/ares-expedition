package com.terraforming.ares.services.simulations;

import com.terraforming.ares.dataset.GameResult;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DataHolderWithFlush {
    static final int TARGET_FILE_ROWS = 400; // кратно 200


    private List<float[]> featureBuffer = new ArrayList<>();
    private List<Float> labelBuffer = new ArrayList<>();

    private String parentDirectory;
    private int fileIndex = 0;

    public DataHolderWithFlush(int dirIndex) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String timestamp = now.format(formatter);

        this.parentDirectory = String.format("sim/%s_%s", timestamp, dirIndex);
    }

    public void addResult(GameResult gameResult) {
        float firstLabel = gameResult.isDraw() ? 0.5f : (gameResult.getWinner() == 1 ? 1 : 0);
        float secondLabel = gameResult.isDraw() ? 0.5f : (gameResult.getWinner() == 2 ? 1 : 0);

        for (float[] state : gameResult.getFirstPlayerStates()) {
            featureBuffer.add(state);
            labelBuffer.add(firstLabel);
        }

        for (float[] state : gameResult.getSecondPlayerStates()) {
            featureBuffer.add(state);
            labelBuffer.add(secondLabel);
        }

        if (featureBuffer.size() >= TARGET_FILE_ROWS) {

            // 2.1 Шафлим
            shuffleInUnison(featureBuffer, labelBuffer);

            // 2.2 Обрезаем до кратности batch size
            int usableSize = TARGET_FILE_ROWS;

            // 2.3 Формируем INDArrays
            INDArray features = Nd4j.create(usableSize, featureBuffer.get(0).length);
            INDArray labels = Nd4j.create(usableSize, 1);

            for (int i = 0; i < usableSize; i++) {
                features.putRow(i, Nd4j.create(featureBuffer.get(i)));
                labels.putScalar(i, 0, labelBuffer.get(i));
            }

            DataSet ds = new DataSet(features, labels);
//            scaler.preProcess(ds); TODO

            // 2.4 Сохраняем
            File dir = new File(parentDirectory);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File out = new File(dir, String.format("dataset_%s.bin", fileIndex++));
            ds.save(out);

            System.out.println("Saved " + out.getName()
                    + " rows=" + usableSize);

            // 2.5 Удаляем использованные данные
            featureBuffer.subList(0, usableSize).clear();
            labelBuffer.subList(0, usableSize).clear();
        }
    }

    static void shuffleInUnison(
            List<float[]> features,
            List<Float> labels
    ) {
        Random rnd = new Random();
        for (int i = features.size() - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);

            float[] tmpF = features.get(i);
            features.set(i, features.get(j));
            features.set(j, tmpF);

            float tmpL = labels.get(i);
            labels.set(i, labels.get(j));
            labels.set(j, tmpL);
        }
    }

}
