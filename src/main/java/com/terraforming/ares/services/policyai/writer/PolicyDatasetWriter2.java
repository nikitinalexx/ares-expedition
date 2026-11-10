package com.terraforming.ares.services.policyai.writer;

import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class PolicyDatasetWriter2 implements Runnable {

    private static final int FILE_RECORDS = 50_000;
    private static final int REPLAY_SIZE = 550_000;
    private static final int REPLAY_SIZE_THRESHOLD = 500_000;

    private final PolicyReplayBuffer replay = new PolicyReplayBuffer(REPLAY_SIZE);

    private final BlockingQueue<PolicyRecord> recordQueue;

    // gameId -> outcome с точки зрения первого игрока (1.0 = первый выиграл, 0.0 = проиграл, 0.5 = ничья)
    private final ConcurrentHashMap<Long, Integer> gameOutcomes = new ConcurrentHashMap<>();

    private int fileIndex;
    private final List<File> savedFiles = new ArrayList<>();

    private static final String FULL_DATA_DIR;

    static {
        FULL_DATA_DIR = "sim/full/main";
        new File(FULL_DATA_DIR).mkdirs();
    }

    public PolicyDatasetWriter2(BlockingQueue<PolicyRecord> recordQueue, int fileIndex) {
        this.recordQueue = recordQueue;
        this.fileIndex = fileIndex;
    }

    /**
     * Сообщить об исходе игры.
     * outcome — с точки зрения первого игрока: 1.0 = победа, 0.0 = поражение, 0.5 = ничья.
     * Для второго игрока outcome будет инвертирован автоматически при finalizeResults.
     */
    public void reportOutcome(long gameId, int outcomeForFirstPlayer) {
        gameOutcomes.put(gameId, outcomeForFirstPlayer);
    }

    private volatile boolean stopped = false;
    public void stop() {
        stopped = true;
    }


    @Override
    public void run() {
        List<PolicyRecord> batch = new ArrayList<>(FILE_RECORDS);

        try {
            while (!stopped) {
                PolicyRecord record = recordQueue.take();
                replay.add(record);

                if (replay.size() >= REPLAY_SIZE_THRESHOLD) {
                    batch.clear();
                    replay.sample(batch, FILE_RECORDS);
                    File file = writeFile(batch);
                    savedFiles.add(file);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Пробегается по всем сохранённым файлам и проставляет outcome каждой записи
     * на основе gameOutcomes. Перезаписывает файлы.
     * Бросает исключение если для какой-то игры не нашлось исхода.
     */
    public void finalizeResults() {
        for (File file : savedFiles) {
            finalizeFile(file);
        }
    }

    private void finalizeFile(File file) {
        // Читаем все записи из файла
        List<PolicyRecord> records = readFile(file);

        // Обновляем outcome для каждой записи
        for (PolicyRecord record : records) {
            Integer firstPlayerOutcome = gameOutcomes.get(record.gameId);
            if (firstPlayerOutcome == null) {
                throw new IllegalStateException(
                        "No outcome found for gameId=" + record.gameId +
                                " in file " + file.getName()
                );
            }
            if (record.isFirstPlayer) {
                record.outcome = firstPlayerOutcome;
            } else {
                // Для второго игрока инвертируем: победа первого = поражение второго
                record.outcome = -firstPlayerOutcome;
            }
        }

        // Перезаписываем файл — финальный формат v2, без gameId/isFirstPlayer
        writeFinalizedFile(records, file);
    }

    // =========================================================
    // WRITE
    // =========================================================

    private File writeFile(List<PolicyRecord> records) {
        File file = new File(FULL_DATA_DIR, "policy_" + (fileIndex++) + ".bin");
        writeRawFile(records, file);
        return file;
    }

    // v3 — промежуточный, с gameId + isFirstPlayer
    private void writeRawFile(List<PolicyRecord> records, File file) {
        writeFileInternal(records, file, 3, false);
    }

    // v2 — финальный, без gameId/isFirstPlayer
    private void writeFinalizedFile(List<PolicyRecord> records, File file) {
        writeFileInternal(records, file, 2, true);
    }

    private void writeFileInternal(List<PolicyRecord> records, File file, int version, boolean finalized) {
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file), 1 << 20))) {

            ByteArrayOutputStream rowBytes = new ByteArrayOutputStream(records.size() * 628);
            DataOutputStream rowOut = new DataOutputStream(rowBytes);

            long[] offsets = new long[records.size()];
            // Заголовок: magic(4) + version(4) + count(4) + offsets(8 * count)
            long headerSize = 12L + (long) records.size() * 8;
            long cursor = headerSize;

            for (int i = 0; i < records.size(); i++) {
                offsets[i] = cursor;
                if (finalized) {
                    PolicyBinaryCodec2.writeFinalized(rowOut, records.get(i));
                } else {
                    PolicyBinaryCodec.write(rowOut, records.get(i));
                }
                cursor = headerSize + rowBytes.size();
            }
            rowOut.flush();

            out.writeInt(0x504F4C59);   // magic
            out.writeInt(version);
            out.writeInt(records.size());

            for (long offset : offsets)
                out.writeLong(offset);

            out.write(rowBytes.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + file.getPath(), e);
        }
    }

    // =========================================================
    // READ (для finalizeResults)
    // =========================================================

    private List<PolicyRecord> readFile(File file) {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file), 1 << 20))) {

            int magic = in.readInt();
            if (magic != 0x504F4C59) {
                throw new IllegalStateException("Invalid magic in file: " + file.getName());
            }

            int version = in.readInt();
            if (version != 3) {
                throw new IllegalStateException(
                        "Unsupported file version " + version + " in " + file.getName() +
                                ". finalizeResults requires version 3."
                );
            }

            int count = in.readInt();

            // Пропускаем offsets
            for (int i = 0; i < count; i++) {
                in.readLong();
            }

            List<PolicyRecord> records = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                records.add(PolicyBinaryCodec2.read(in));
            }
            return records;

        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + file.getPath(), e);
        }
    }
}