package com.terraforming.ares.services.simulations;

import com.terraforming.ares.services.ai.AiConstants;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DatasetWriter implements Runnable {
    public static final SampleBatch POISON = new SampleBatch(List.of(), List.of());


    static final int TARGET_FILE_ROWS = 200_000;
    static final DataType DTYPE = DataType.FLOAT16;

    private final BlockingQueue<SampleBatch> queue;

    private final List<float[]> featureBuffer = new ArrayList<>(TARGET_FILE_ROWS);
    private final List<Float> labelBuffer = new ArrayList<>(TARGET_FILE_ROWS);

    private final Random rnd = new Random();
    private static final AtomicInteger FILE_INDEX = new AtomicInteger(300);

    private static final String FULL_DATA_DIR;


    static {
        Nd4j.setDefaultDataTypes(DTYPE, DTYPE);

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        FULL_DATA_DIR = "sim/full/" + ts;

        new File(FULL_DATA_DIR).mkdirs();
    }

    public DatasetWriter(BlockingQueue<SampleBatch> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            while (true) {
                SampleBatch batch = queue.take();
                if (batch == POISON) {
                    break;
                }
                featureBuffer.addAll(batch.features());
                labelBuffer.addAll(batch.labels());

                if (featureBuffer.size() >= TARGET_FILE_ROWS) {
                    flush();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void flush() {
        shuffleInUnison(featureBuffer, labelBuffer);

        int usableSize = TARGET_FILE_ROWS;
        int totalFeatureSize = featureBuffer.getFirst().length;

        // Подготовка плоских массивов
        float[] flatFeatures = new float[usableSize * totalFeatureSize];
        float[] flatLabels   = new float[usableSize];

        for (int i = 0; i < usableSize; i++) {
            System.arraycopy(
                    featureBuffer.get(i), 0,
                    flatFeatures, i * totalFeatureSize,
                    totalFeatureSize
            );
            flatLabels[i] = labelBuffer.get(i);
        }

        // Создание единого DataSet (TABLE / FULL)
        INDArray tableFeatures = Nd4j.create(
                DataType.FLOAT16,
                new long[]{usableSize, totalFeatureSize},
                'c'
        ).assign(
                Nd4j.create(flatFeatures, new long[]{usableSize, totalFeatureSize}, 'c')
        );

        INDArray labels = Nd4j.create(
                DataType.FLOAT16,
                new long[]{usableSize, 1},
                'c'
        ).assign(
                Nd4j.create(flatLabels, new long[]{usableSize, 1}, 'c')
        );

        // Проверка на валидность данных перед сохранением
        if (tableFeatures.isNaN().any() || tableFeatures.isInfinite().any()) {
            throw new IllegalStateException("NaN or Inf detected in features during flush");
        }

        DataSet fullDs = new DataSet(tableFeatures, labels);

        // =========================================================
        // SAVE (Единый выходной файл)
        // =========================================================
        int idx = FILE_INDEX.getAndIncrement();
        File outputFile = new File(FULL_DATA_DIR, "dataset_" + idx + ".bin");
        fullDs.save(outputFile);

        // =========================================================
        // CLEANUP
        // =========================================================
        featureBuffer.subList(0, usableSize).clear();
        labelBuffer.subList(0, usableSize).clear();

        System.out.println(
                "Flushed dataset_" + idx +
                        " | rows=" + usableSize +
                        " | features=" + totalFeatureSize +
                        " | target=" + outputFile.getAbsolutePath()
        );
    }


    private void shuffleInUnison(List<float[]> features, List<Float> labels) {
        for (int i = features.size() - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);

            float[] tf = features.get(i);
            features.set(i, features.get(j));
            features.set(j, tf);

            float tl = labels.get(i);
            labels.set(i, labels.get(j));
            labels.set(j, tl);
        }
    }
}

