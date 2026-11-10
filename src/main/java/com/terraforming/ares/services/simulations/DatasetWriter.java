package com.terraforming.ares.services.simulations;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.factory.Nd4j;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DatasetWriter implements Runnable {
    public static final SampleBatch POISON = new SampleBatch(List.of(), new FloatArrayList());

    static final int TARGET_FILE_ROWS = 200_000;
    static final DataType DTYPE = DataType.FLOAT16;

    private final BlockingQueue<SampleBatch> queue;

    private List<float[]> featureBuffer = new ArrayList<>(TARGET_FILE_ROWS + 1000);
    private FloatArrayList labelBuffer = new FloatArrayList(TARGET_FILE_ROWS + 1000);

    private final Random rnd = new Random();
    private static final AtomicInteger FILE_INDEX = new AtomicInteger(211);
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
                if (batch == POISON) break;

                featureBuffer.addAll(batch.features());
                labelBuffer.addAll(batch.labels()); // FloatArrayList.addAll — нативный, без боксинга

                if (featureBuffer.size() >= TARGET_FILE_ROWS) {
                    flush();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void flush() {
        int usableSize = TARGET_FILE_ROWS;
        int totalFeatureSize = featureBuffer.get(0).length;

        shuffleInUnison(featureBuffer, labelBuffer, usableSize);

        int idx = FILE_INDEX.getAndIncrement();
        File outputFile = new File(FULL_DATA_DIR, "dataset_" + idx + ".bin");

        // Размер буфера: header(8) + каждая строка = (features + 1 label) * 2 байта
        int rowBytes = (totalFeatureSize + 1) * 2;
        ByteBuffer bb = ByteBuffer.allocateDirect(8 + usableSize * rowBytes)
                .order(ByteOrder.BIG_ENDIAN);

        bb.putInt(usableSize);
        bb.putInt(totalFeatureSize);

        for (int i = 0; i < usableSize; i++) {
            for (float f : featureBuffer.get(i)) {
                bb.putShort(Float.floatToFloat16(f));
            }
            bb.putShort(Float.floatToFloat16(labelBuffer.getFloat(i)));
        }

        try (FileOutputStream fos = new FileOutputStream(outputFile);
             FileChannel ch = fos.getChannel()) {
            bb.flip();
            ch.write(bb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write dataset", e);
        }

        // Swap вместо subList.clear() — O(1) вместо O(n)
        int remaining = featureBuffer.size() - usableSize;
        List<float[]> newFeatures = new ArrayList<>(TARGET_FILE_ROWS + 1000);
        FloatArrayList newLabels = new FloatArrayList(TARGET_FILE_ROWS + 1000);

        if (remaining > 0) {
            newFeatures.addAll(featureBuffer.subList(usableSize, featureBuffer.size()));
            newLabels.addAll(labelBuffer.subList(usableSize, labelBuffer.size()));
        }

        featureBuffer = newFeatures;
        labelBuffer = newLabels;

        System.out.println("Flushed dataset_" + idx +
                " | rows=" + usableSize +
                " | features=" + totalFeatureSize +
                " | target=" + outputFile.getAbsolutePath());
    }

    private void shuffleInUnison(List<float[]> features, FloatArrayList labels, int size) {
        for (int i = size - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);

            float[] tf = features.get(i);
            features.set(i, features.get(j));
            features.set(j, tf);

            // getFloat/set — примитивы, без боксинга
            float tl = labels.getFloat(i);
            labels.set(i, labels.getFloat(j));
            labels.set(j, tl);
        }
    }
}

