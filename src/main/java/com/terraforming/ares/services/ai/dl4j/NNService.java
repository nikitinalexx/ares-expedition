package com.terraforming.ares.services.ai.dl4j;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.AiConstants;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    public static final int MAX_STATES_PER_BATCH = 4096;//512

    // Перечисление для выбора модели
    public enum ModelType {
        FIRST,
        SECOND
    }

    // Две отдельные очереди для разных моделей
    private final Map<ModelType, Queue<BatchRequest>> queues = new EnumMap<>(ModelType.class);

    private final Map<ModelType, MultiLayerNetwork> networks = new EnumMap<>(ModelType.class);

    public NNService() throws Exception {
        networks.put(
                ModelType.FIRST,
//                MultiLayerNetwork.load(new File("self_play_iter_3_8876_4278_a15_l55.zip"), false)


//                MultiLayerNetwork.load(new File("self_play_iter_2_8853_4430_batch200_a15_l14.zip"), false)

//                MultiLayerNetwork.load(new File("iter5_epoch_3.zip"), false)
//                MultiLayerNetwork.load(new File("epoch_4_150.zip"), false)
                MultiLayerNetwork.load(new File("iter8_v4_350k_epoch_2_375.zip"), false)






//                MultiLayerNetwork.load(new File("self_play_iter_2_9331_3374.zip"), false)
        );

        networks.put(
                ModelType.SECOND,
//                MultiLayerNetwork.load(new File("self_play_iter_2_8853_4430_batch200_a15_l14.zip"), false)
//                MultiLayerNetwork.load(new File("epoch_1_120.zip"), false)
                MultiLayerNetwork.load(new File("iter8_v4_350k_epoch_2_375.zip"), false)


//                MultiLayerNetwork.load(new File("iter8_epoch_3_50_dropout79_150k_files.zip"), false)



//                MultiLayerNetwork.load(new File("epoch_1_1768944741372.zip"), false)
//                MultiLayerNetwork.load(new File("epoch_1_1768929578929.zip"), false)

        );

        // Инициализируем очереди и батчеры
        for (ModelType type : ModelType.values()) {
            Queue<BatchRequest> queue = new ConcurrentLinkedQueue<>();
            queues.put(type, queue);

            MultiLayerNetwork net = networks.get(type);

            Thread.ofVirtual()
                    .name("Batcher-" + type)
                    .start(() -> batchLoop(type, queue, net));
        }
    }

    public List<Prediction> predictBatch(List<float[]> featuresBatch, ModelType modelType) {
        if (featuresBatch.isEmpty()) return List.of();

        CompletableFuture<List<Prediction>> future = new CompletableFuture<>();
        queues.get(modelType).add(new BatchRequest(featuresBatch, future));

        return future.join();
    }

    /**
     * Теперь принимает тип модели, в которую нужно отправить батч
     */
    public List<Prediction> predictBatch(List<float[]> featuresBatch, Player player) {
        if (featuresBatch.isEmpty()) return List.of();

        CompletableFuture<List<Prediction>> future = new CompletableFuture<>();
        queues.get(player.isFirstBot() ? ModelType.FIRST : ModelType.SECOND).add(new BatchRequest(featuresBatch, future));

        return future.join();
    }

    private void batchLoop(
            ModelType type,
            Queue<BatchRequest> queue,
            MultiLayerNetwork net
    ) {
        final long BATCH_WINDOW_NANOS = 4_000_000; // 4 мс
        final int TARGET_BATCH_SIZE = 1024;

        while (!Thread.currentThread().isInterrupted()) {
            List<BatchRequest> batch = new ArrayList<>();
            try {
                // Ждём первый запрос
                BatchRequest first;
                while ((first = queue.poll()) == null) {
                    if (Thread.currentThread().isInterrupted()) return;
                    Thread.onSpinWait();
                }

                batch.add(first);
                int currentStatesCount = first.features.size();

                long deadline = System.nanoTime() + BATCH_WINDOW_NANOS;

                // Добираем батч
                while (currentStatesCount < MAX_STATES_PER_BATCH) {
                    BatchRequest next = queue.poll();

                    if (next != null) {
                        batch.add(next);
                        currentStatesCount += next.features.size();
                        if (currentStatesCount >= TARGET_BATCH_SIZE) break;
                    } else {
                        if (System.nanoTime() >= deadline) break;
                        Thread.onSpinWait();
                    }
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

    private void processInference(List<BatchRequest> batch, MultiLayerNetwork net, int totalStates) {
        // 1. Подготовка данных
        float[] allFeatures = new float[totalStates * AiConstants.TABLE_VECTOR_SIZE];
        int currentPos = 0;
        for (BatchRequest r : batch) {
            for (float[] feature : r.features) {
                System.arraycopy(feature, 0, allFeatures, currentPos, feature.length);
                currentPos += feature.length;
            }
        }
        INDArray tableFeatures = Nd4j.create(allFeatures, new int[]{totalStates, AiConstants.TABLE_VECTOR_SIZE}, 'c');

        // 2. Вычисление (GPU)
        INDArray output = net.output(tableFeatures, false);

        // 3. Пост-обработка
        float[] outputData = output.data().asFloat();
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
