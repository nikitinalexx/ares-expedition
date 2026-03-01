package com.terraforming.ares.services.simulations;

import com.terraforming.ares.services.policyai.dto.GameRecordArena;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.factory.Nd4j;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class PolicyDatasetWriter implements Runnable {
    public static final SampleBatch POISON = new SampleBatch(List.of(), List.of());


    static final int TARGET_FILE_ROWS = 200_000;
    static final DataType DTYPE = DataType.FLOAT16;

    private final BlockingQueue<GameRecordArena> finishedGames;

    private final List<float[]> featureBuffer = new ArrayList<>(TARGET_FILE_ROWS);
    private final List<Float> labelBuffer = new ArrayList<>(TARGET_FILE_ROWS);

    private final Random rnd = new Random();
    private static final AtomicInteger FILE_INDEX = new AtomicInteger(158);

    private static final String FULL_DATA_DIR;


    static {
        Nd4j.setDefaultDataTypes(DTYPE, DTYPE);

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        FULL_DATA_DIR = "sim/full/" + ts;

        new File(FULL_DATA_DIR).mkdirs();
    }

    public PolicyDatasetWriter(BlockingQueue<GameRecordArena> finishedGames) {
        this.finishedGames = finishedGames;
    }

    @Override
    public void run() {
        try {
            while (true) {

                GameRecordArena arena = finishedGames.take();

                writeArena(arena);


            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void writeArena(GameRecordArena arena) {
//
//        int[] ids = arena.ids();
//
//        for (int i = 0; i < arena.size(); i++) {
//
//            PolicyRecord r = storage.get(ids[i]);
//
//            serializer.write(r, byteBuffer);
//        }
//
//        flushIfNeeded();
    }

    private void flush() {
        shuffleInUnison(featureBuffer, labelBuffer);

        int usableSize = TARGET_FILE_ROWS;
        int totalFeatureSize = featureBuffer.getFirst().length;

        int idx = FILE_INDEX.getAndIncrement();
        File outputFile = new File(FULL_DATA_DIR, "dataset_" + idx + ".bin");

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(outputFile), 1 << 20))) {

            // Заголовок чтобы знать размеры при чтении
            dos.writeInt(usableSize);
            dos.writeInt(totalFeatureSize);

            for (int i = 0; i < usableSize; i++) {
                float[] features = featureBuffer.get(i);
                for (float f : features) {
                    dos.writeShort(Float.floatToFloat16(f));
                }
                dos.writeShort(Float.floatToFloat16(labelBuffer.get(i)));
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to write dataset", e);
        }

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

