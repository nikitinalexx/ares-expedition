package com.terraforming.ares.services.ai.dl4j;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.AiConstants;
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
public class NNService {
    public static final int MAX_REQUESTS_PER_TICK = 256;//64
    public static final int MAX_STATES_PER_BATCH = 8192;//512
    public static final int CHUNK_SIZE = 4096;//512

    // Перечисление для выбора модели
    public enum ModelType {
        FIRST,
        SECOND
    }

    // Две отдельные очереди для разных моделей
    private final Map<ModelType, Queue<BatchRequest>> queues = new EnumMap<>(ModelType.class);

    private final Map<ModelType, ComputationGraph> networks = new EnumMap<>(ModelType.class);

    public NNService() throws Exception {
        networks.put(
                ModelType.FIRST,
                ComputationGraph.load(new File("iter4_epoch_2_93.zip"), false)

        );

        networks.put(
                ModelType.SECOND,
                ComputationGraph.load(new File("iter4_epoch_2_93.zip"), false)

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

    public List<Prediction> predictBatch(List<float[]> featuresBatch, ModelType modelType) {
        if (featuresBatch.isEmpty()) return List.of();

        if (featuresBatch.size() <= CHUNK_SIZE) {
            CompletableFuture<List<Prediction>> future = new CompletableFuture<>();
            queues.get(modelType).add(new BatchRequest(featuresBatch, future));
            return future.join();
        }

        // Бьём на чанки и собираем результат
        List<CompletableFuture<List<Prediction>>> futures = new ArrayList<>();

        for (int i = 0; i < featuresBatch.size(); i += CHUNK_SIZE) {
            List<float[]> chunk = featuresBatch.subList(i, Math.min(i + CHUNK_SIZE, featuresBatch.size()));
            CompletableFuture<List<Prediction>> future = new CompletableFuture<>();
            queues.get(modelType).add(new BatchRequest(chunk, future));
            futures.add(future);
        }

        List<Prediction> result = new ArrayList<>(featuresBatch.size());
        for (CompletableFuture<List<Prediction>> future : futures) {
            result.addAll(future.join());
        }
        return result;
    }

    /**
     * Теперь принимает тип модели, в которую нужно отправить батч
     */
    public List<Prediction> predictBatch(List<float[]> featuresBatch, Player player) {
        return predictBatch(featuresBatch, player.isFirstBot() ? ModelType.FIRST : ModelType.SECOND);
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

                while (currentStatesCount < MAX_STATES_PER_BATCH) {
                    BatchRequest next = queue.poll();
                    if (next != null) {
                        batch.add(next);
                        currentStatesCount += next.features.size();
                    } else {
                        break;
                    }
                }

                batchCount++;
                if (batchCount % 5000 == 0) {
                    logGpuMemory(type, batchCount, currentStatesCount);
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

    private void logGpuMemory(ModelType type, long batchCount, int states) {
        try {
            SizeTPointer free = new SizeTPointer(1);
            SizeTPointer total = new SizeTPointer(1);
            cudaMemGetInfo(free, total);
            long usedMb = (total.get() - free.get()) / 1024 / 1024;
            long totalMb = total.get() / 1024 / 1024;
            long freeMb = free.get() / 1024 / 1024;
            System.out.printf("GPU: %d/%d MB (free=%d MB)%n",usedMb, totalMb, freeMb);
        } catch (Throwable t) {
            System.out.println("GPU mem log failed: " + t.getMessage());
        }
    }

    private void processInference(List<BatchRequest> batch, ComputationGraph net, int totalStates) {
        // 1. Определяем размеры из твоих констант (подставь свои имена констант)
        int tableSize = AiConstants.TABLE_VECTOR_SIZE;
        int handSize = AiConstants.HAND_VECTOR_SIZE;

        // 2. Подготовка данных (два отдельных плоских массива)
        float[] tableFlat = new float[totalStates * tableSize];
        float[] handFlat = new float[totalStates * handSize];

        int tablePos = 0;
        int handPos = 0;

        for (BatchRequest r : batch) {
            for (float[] fullFeature : r.features) {
                // Копируем первую часть вектора в массив стола
                System.arraycopy(fullFeature, 0, tableFlat, tablePos, tableSize);
                tablePos += tableSize;

                // Копируем вторую часть вектора в массив руки
                System.arraycopy(fullFeature, tableSize, handFlat, handPos, handSize);
                handPos += handSize;
            }
        }

        try (INDArray tableInput = Nd4j.create(tableFlat, new int[]{totalStates, tableSize}, 'c');
             INDArray handInput = Nd4j.create(handFlat, new int[]{totalStates, handSize}, 'c')) {

            try {
                INDArray[] output = net.output(false, tableInput, handInput);

                try {
                    float[] outputData = output[0].data().asFloat();
                    int offset = 0;
                    for (BatchRequest r : batch) {
                        int size = r.features.size();
                        List<Prediction> subResult = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            subResult.add(new Prediction(outputData[offset + i]));
                        }
                        r.future.complete(subResult);
                        offset += size;
                    }
                } finally {
                    for (INDArray o : output) {
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

    private static class BatchRequest {
        final List<float[]> features;
        final CompletableFuture<List<Prediction>> future;

        BatchRequest(List<float[]> features, CompletableFuture<List<Prediction>> future) {
            this.features = features;
            this.future = future;
        }
    }
}
