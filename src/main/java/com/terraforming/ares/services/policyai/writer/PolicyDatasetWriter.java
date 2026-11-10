package com.terraforming.ares.services.policyai.writer;

import com.terraforming.ares.services.policyai.dto.GameRecordArena;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class PolicyDatasetWriter implements Runnable {

    private static final int FILE_RECORDS = 50_000;
    //    private static final int REPLAY_SIZE  = 550000;
//    private static final int REPLAY_SIZE_THRESHOLD  = 500000;
    private static final int REPLAY_SIZE = 550000;
    private static final int REPLAY_SIZE_THRESHOLD = 500000;

    private final PolicyReplayBuffer replay = new PolicyReplayBuffer(REPLAY_SIZE);


    private final BlockingQueue<GameRecordArena> finishedGames;


    private int fileIndex;

    private static final String FULL_DATA_DIR;


    static {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        FULL_DATA_DIR = "sim/full/" + "main";

        new File(FULL_DATA_DIR).mkdirs();
    }

    public PolicyDatasetWriter(
            BlockingQueue<GameRecordArena> finishedGames, int fileIndex) {
        this.finishedGames = finishedGames;
        this.fileIndex = fileIndex;
    }

    @Override
    public void run() {
        List<PolicyRecord> batch = new ArrayList<>(FILE_RECORDS);

        try {

            while (true) {

                GameRecordArena arena = finishedGames.take();

                for (int i = 0; i < arena.size(); i++) {
                    replay.add(arena.records()[i]);
                }

                if (replay.size() >= REPLAY_SIZE_THRESHOLD) {

                    batch.clear();

                    replay.sample(batch, FILE_RECORDS);

                    writeFile(batch);
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void writeFile(List<PolicyRecord> records) {
        File file = new File(FULL_DATA_DIR, "policy_" + (fileIndex++) + ".bin");

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file), 1 << 20))) {

            // --- Pass 1: сериализуем строки во временный буфер, собираем оффсеты ---
            ByteArrayOutputStream rowBytes = new ByteArrayOutputStream(records.size() * 620);
            DataOutputStream rowOut = new DataOutputStream(rowBytes);

            long[] offsets = new long[records.size()];
            // Заголовок: magic(4) + version(4) + count(4) + offsets(8 * count)
            long headerSize = 12L + (long) records.size() * 8;
            long cursor = headerSize;

            for (int i = 0; i < records.size(); i++) {
                offsets[i] = cursor;
                PolicyBinaryCodec.write(rowOut, records.get(i));
                cursor = headerSize + rowBytes.size();
            }
            rowOut.flush();

            // --- Pass 2: пишем финальный файл ---
            out.writeInt(0x504F4C59);   // magic
            out.writeInt(2);            // version = 2
            out.writeInt(records.size());

            for (long offset : offsets)
                out.writeLong(offset);

            out.write(rowBytes.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

