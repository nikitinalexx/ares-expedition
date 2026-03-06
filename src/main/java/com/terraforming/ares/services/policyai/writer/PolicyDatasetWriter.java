package com.terraforming.ares.services.policyai.writer;

import com.terraforming.ares.services.policyai.dto.GameRecordArena;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import org.nd4j.linalg.factory.Nd4j;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class PolicyDatasetWriter implements Runnable {

    private static final int FILE_RECORDS = 50_000;
//    private static final int REPLAY_SIZE  = 550000;
//    private static final int REPLAY_SIZE_THRESHOLD  = 500000;
private static final int REPLAY_SIZE  = 100000;
    private static final int REPLAY_SIZE_THRESHOLD  = 55000;

    private final PolicyReplayBuffer replay = new PolicyReplayBuffer(REPLAY_SIZE);


    private final BlockingQueue<GameRecordArena> finishedGames;


    private int fileIndex = 0;

    private static final String FULL_DATA_DIR;


    static {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        FULL_DATA_DIR = "sim/full/" + ts;

        new File(FULL_DATA_DIR).mkdirs();
    }

    public PolicyDatasetWriter(
            BlockingQueue<GameRecordArena> finishedGames) {
        this.finishedGames = finishedGames;
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

        File file = new File(
                FULL_DATA_DIR,
                "policy_" + (fileIndex++) + ".bin"
        );

        try (DataOutputStream out =
                     new DataOutputStream(
                             new BufferedOutputStream(
                                     new FileOutputStream(file),
                                     1 << 20))) {

            out.writeInt(0x504F4C59);
            out.writeInt(1);
            out.writeInt(records.size());

            for (PolicyRecord r : records)
                PolicyBinaryCodec.write(out, r);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

