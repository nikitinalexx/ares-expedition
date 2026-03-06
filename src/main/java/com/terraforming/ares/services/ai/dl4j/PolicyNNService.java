package com.terraforming.ares.services.ai.dl4j;

import org.bytedeco.javacpp.SizeTPointer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.bytedeco.cuda.global.cudart.cudaMemGetInfo;

/**
 * Loom-friendly NN service with real ND4J inference.
 * <p>
 * Поддерживает:
 * - predict(float[], boolean)
 * - predictBatch(List<float[]>, boolean)
 * <p>
 * Внутри:
 * - одна очередь
 * - один batcher-thread
 * - реальный GPU batch через ND4J
 */
@Service
public class PolicyNNService {
    public static final int MAX_REQUESTS_PER_TICK = 256;//64
    public static final int MAX_STATES_PER_BATCH = 8192;//512
    public static final int CHUNK_SIZE = 4096;//512
    private static final long BATCH_ACCUMULATION_NANOS = 2_000_000; // 2 ms

    // Перечисление для выбора модели
    public enum ModelType {
        FIRST,
        SECOND
    }

    // Две отдельные очереди для разных моделей
    private final Map<ModelType, Queue<BatchRequest>> queues = new EnumMap<>(ModelType.class);

    private final Map<ModelType, ComputationGraph> networks = new EnumMap<>(ModelType.class);

    public PolicyNNService() throws Exception {
        networks.put(
                ModelType.FIRST,
                ComputationGraph.load(new File("model_epoch1_batch75.zip"), false)

        );

        networks.put(
                ModelType.SECOND,
                ComputationGraph.load(new File("model_epoch1_batch75.zip"), false)

        );

        // Инициализируем очереди и батчеры
        for (ModelType type : ModelType.values()) {
            Queue<BatchRequest> queue = new ConcurrentLinkedQueue<>();
            queues.put(type, queue);

            ComputationGraph net = networks.get(type);

            Thread.ofVirtual()
                    .name("Batcher-" + type)
                    .start(() -> batchLoop(type, queue, net));
        }
    }

    public List<PolicyPrediction> predictBatch(List<float[]> featuresBatch, ModelType modelType) {
        if (featuresBatch.isEmpty()) return List.of();

        if (featuresBatch.size() <= CHUNK_SIZE) {
            CompletableFuture<List<PolicyPrediction>> future = new CompletableFuture<>();
            queues.get(modelType).add(new BatchRequest(featuresBatch, future));
            return future.join();
        }

        // Бьём на чанки и собираем результат
        List<CompletableFuture<List<PolicyPrediction>>> futures = new ArrayList<>();

        for (int i = 0; i < featuresBatch.size(); i += CHUNK_SIZE) {
            List<float[]> chunk = featuresBatch.subList(i, Math.min(i + CHUNK_SIZE, featuresBatch.size()));
            CompletableFuture<List<PolicyPrediction>> future = new CompletableFuture<>();
            queues.get(modelType).add(new BatchRequest(chunk, future));
            futures.add(future);
        }

        List<PolicyPrediction> result = new ArrayList<>(featuresBatch.size());
        for (CompletableFuture<List<PolicyPrediction>> future : futures) {
            result.addAll(future.join());
        }
        return result;
    }

    private void batchLoop(
            ModelType type,
            Queue<BatchRequest> queue,
            ComputationGraph net
    ) {
        long batchCount = 0;

        while (!Thread.currentThread().isInterrupted()) {
            List<BatchRequest> batch = new ArrayList<>();
            try {
                BatchRequest first;
                while ((first = queue.poll()) == null) {
                    if (Thread.currentThread().isInterrupted()) return;
                    Thread.onSpinWait();
                }

                batch.add(first);
                int currentStatesCount = first.features.size();

                // --- Небольшое окно накопления ---
                long deadline = System.nanoTime() + BATCH_ACCUMULATION_NANOS;

                while (System.nanoTime() < deadline &&
                        currentStatesCount < MAX_STATES_PER_BATCH) {

                    BatchRequest next = queue.poll();
                    if (next != null) {
                        batch.add(next);
                        currentStatesCount += next.features.size();
                    } else {
                        Thread.onSpinWait(); // не засыпаем полностью, держим низкую latency
                    }
                }
                // --- конец окна накопления ---

                batchCount++;
                if (batchCount % 5000 == 0) {
                    logGpuMemory();
                }

                processInference(batch, net, currentStatesCount);

            } catch (Throwable t) {
                t.printStackTrace();
                for (BatchRequest r : batch) {
                    r.future.completeExceptionally(t);
                }
            }
        }
    }

    private void logGpuMemory() {
        try {
            SizeTPointer free = new SizeTPointer(1);
            SizeTPointer total = new SizeTPointer(1);
            cudaMemGetInfo(free, total);
            long usedMb = (total.get() - free.get()) / 1024 / 1024;
            long totalMb = total.get() / 1024 / 1024;
            long freeMb = free.get() / 1024 / 1024;
            System.out.printf("GPU: %d/%d MB (free=%d MB)%n", usedMb, totalMb, freeMb);
        } catch (Throwable t) {
            System.out.println("GPU mem log failed: " + t.getMessage());
        }
    }

    private void processInference(List<BatchRequest> batch, ComputationGraph net, int totalStates) {
//        System.out.println("States " +  totalStates);
        // 1. Размеры из констант (убедись, что они соответствуют 699, 592 и 268)
        int tableSize = 699;
        int handSize = 592;
        int selectableSize = 268;

        // 2. Подготовка трех массивов данных
        float[] tableFlat = new float[totalStates * tableSize];
        float[] handFlat = new float[totalStates * handSize];
        float[] selectableFlat = new float[totalStates * selectableSize];

        int tablePos = 0, handPos = 0, selectPos = 0;

        for (BatchRequest r : batch) {
            for (float[] fullFeature : r.features) {
                // fullFeature должен быть длиной 1559 (699 + 592 + 268)
                System.arraycopy(fullFeature, 0, tableFlat, tablePos, tableSize);
                tablePos += tableSize;

                System.arraycopy(fullFeature, tableSize, handFlat, handPos, handSize);
                handPos += handSize;

                System.arraycopy(fullFeature, tableSize + handSize, selectableFlat, selectPos, selectableSize);
                selectPos += selectableSize;
            }
        }

        // Создаем INDArrays (используем try-with-resources для авто-закрытия)
        try (INDArray tableInput = Nd4j.create(tableFlat, new int[]{totalStates, tableSize}, 'c');
             INDArray handInput = Nd4j.create(handFlat, new int[]{totalStates, handSize}, 'c');
             INDArray selectInput = Nd4j.create(selectableFlat, new int[]{totalStates, selectableSize}, 'c')) {

            try {
                // Выполняем инференс (передаем 3 входа)
//                long startTime = System.currentTimeMillis();
                INDArray[] outputs = net.output(false, tableInput, handInput, selectInput);
//                System.out.println("Spent " + (System.currentTimeMillis() - startTime) + " ms");

                try {
//                    // Извлекаем данные всех голов (Value, Build, Sell, Target, Blue, Corp, Small, System)
//                    // Нам нужно преобразовать их в удобный формат для PolicyPrediction
//                    int offset = 0;
//                    for (BatchRequest r : batch) {
//                        int size = r.features.size();
//                        List<PolicyPrediction> subResult = new ArrayList<>(size);
//
//                        for (int i = 0; i < size; i++) {
//                            int currentRow = offset + i;
//
//                            // Создаем объект PolicyPrediction, передавая туда данные из каждой головы
//                            // Нам нужны данные только для текущей строки (currentRow)
//                            PolicyPrediction p = new PolicyPrediction();
//                            p.value = outputs[0].getFloat(currentRow, 0);
//                            p.buildLogits = getRowAsFloat(outputs[1], currentRow);
//                            p.sellLogits = getRowAsFloat(outputs[2], currentRow);
//                            p.targetLogits = getRowAsFloat(outputs[3], currentRow);
//                            p.blueLogits = getRowAsFloat(outputs[4], currentRow);
//                            p.corpLogits = getRowAsFloat(outputs[5], currentRow);
//                            p.smallLogits = getRowAsFloat(outputs[6], currentRow);
//                            p.systemLogits = getRowAsFloat(outputs[7], currentRow);
//
//                            subResult.add(p);
//                        }
//                        r.future.complete(subResult);
//                        offset += size;
//                    }
                    // 1. Сначала выкачиваем все головы целиком в плоские массивы
                    float[][] allHeadsData = new float[outputs.length][];
                    for (int h = 0; h < outputs.length; h++) {
                        allHeadsData[h] = outputs[h].data().asFloat();
                    }

// 2. В цикле по BatchRequest просто копируем куски из этих массивов
                    int offset = 0;
                    for (BatchRequest r : batch) {
                        int size = r.features.size();
                        List<PolicyPrediction> subResult = new ArrayList<>(size);

                        for (int i = 0; i < r.features.size(); i++) {
                            int currentRow = offset + i;
                            PolicyPrediction p = new PolicyPrediction();

                            // Быстрый ручной метод нарезки
                            p.buildLogits = copyFromFlat(allHeadsData[1], currentRow, 268);
                            p.sellLogits = copyFromFlat(allHeadsData[2], currentRow, 268);
                            p.targetLogits = copyFromFlat(allHeadsData[3], currentRow, 90);
                            p.blueLogits = copyFromFlat(allHeadsData[4], currentRow, 49);
                            p.corpLogits = copyFromFlat(allHeadsData[5], currentRow, 25);
                            p.smallLogits = copyFromFlat(allHeadsData[6], currentRow, 40);
                            p.systemLogits = copyFromFlat(allHeadsData[7], currentRow, 6);
//
                            subResult.add(p);
                            // ... и так далее
                        }
                        r.future.complete(subResult);
                        offset += size;
                    }
                } finally {
                    // Закрываем выходные массивы для предотвращения утечек памяти в Off-heap
                    for (INDArray o : outputs) {
                        if (o != null) o.close();
                    }
                }
            } catch (Throwable t) {
                Nd4j.getWorkspaceManager().destroyAllWorkspacesForCurrentThread();
                for (BatchRequest r : batch) {
                    r.future.completeExceptionally(t);
                }
                t.printStackTrace();
            }
        }
    }

    private float[] copyFromFlat(float[] source, int row, int width) {
        float[] dest = new float[width];
        System.arraycopy(source, row * width, dest, 0, width);
        return dest;
    }

    // Вспомогательный метод для конвертации строки INDArray в float[]
    private float[] getRowAsFloat(INDArray array, int row) {
        return array.getRow(row).toFloatVector();
    }

    private static class BatchRequest {
        final List<float[]> features;
        final CompletableFuture<List<PolicyPrediction>> future;

        BatchRequest(List<float[]> features, CompletableFuture<List<PolicyPrediction>> future) {
            this.features = features;
            this.future = future;
        }
    }
}
