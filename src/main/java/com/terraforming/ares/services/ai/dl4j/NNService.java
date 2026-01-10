package com.terraforming.ares.services.ai.dl4j;

import com.terraforming.ares.services.ai.AiConstants;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import java.util.*;
import java.util.concurrent.*;

import org.springframework.stereotype.Service;

/**
 * Loom-friendly NN service with real ND4J inference.
 *
 * Поддерживает:
 *  - predict(float[], boolean)
 *  - predictBatch(List<float[]>, boolean)
 *
 * Внутри:
 *  - одна очередь
 *  - один batcher-thread
 *  - реальный GPU batch через ND4J
 */
@Service
public class NNService {
    private static final int MAX_REQUESTS_PER_TICK = 256;//64
    private static final int MAX_STATES_PER_BATCH = 4096;//512

    // Перечисление для выбора модели
    public enum ModelType { BASE, OPTIMIZED }

    // Две отдельные очереди для разных моделей
    private final Map<ModelType, BlockingQueue<BatchRequest>> queues = Map.of(
            ModelType.BASE, new LinkedBlockingQueue<>(),
            ModelType.OPTIMIZED, new LinkedBlockingQueue<>()
    );

    private final MultiLayerNetwork baseNet;
    private final MultiLayerNetwork optimizedNet;

    public NNService() throws Exception {
        // Загружаем обе модели
        this.baseNet = MultiLayerNetwork.load(new File("436_epoch_1_8495_5228.zip"), false);
        // Используем модель 3-й эпохи, так как она показала лучший баланс
        this.optimizedNet = MultiLayerNetwork.load(new File("436_epoch_1_8565_4821.zip"), false);

        // Запускаем два независимых батчера
        Thread.ofPlatform().name("nn-batcher-base").start(() -> batchLoop(ModelType.BASE, baseNet));
        Thread.ofPlatform().name("nn-batcher-opt").start(() -> batchLoop(ModelType.OPTIMIZED, optimizedNet));
    }

    /**
     * Теперь принимает тип модели, в которую нужно отправить батч
     */
    public List<Prediction> predictBatch(List<float[]> featuresBatch, ModelType modelType) {
        if (featuresBatch.isEmpty()) return List.of();

        CompletableFuture<List<Prediction>> future = new CompletableFuture<>();
        queues.get(modelType).add(new BatchRequest(featuresBatch, future));

        return future.join();
    }

    private void batchLoop(ModelType type, MultiLayerNetwork net) {
        BlockingQueue<BatchRequest> queue = queues.get(type);

        while (!Thread.currentThread().isInterrupted()) {
            List<BatchRequest> batch = new ArrayList<>();
            try {
                BatchRequest first = queue.take(); // Ждем первый запрос
                batch.add(first);

                int currentStatesCount = first.features.size();

                // Собираем батч только из своей очереди
                while (batch.size() < MAX_REQUESTS_PER_TICK) {
                    BatchRequest next = queue.peek();
                    if (next == null || currentStatesCount + next.features.size() > MAX_STATES_PER_BATCH) break;

                    batch.add(queue.poll());
                    currentStatesCount += next.features.size();
                }

                // Инференс конкретной моделью
                processInference(batch, net, currentStatesCount);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                t.printStackTrace();
                for (BatchRequest r : batch) r.future.completeExceptionally(t);
            }
        }
    }

    private void processInference(List<BatchRequest> batch, MultiLayerNetwork net, int totalStates) {
        // 1. Создаем один плоский массив вместо множества мелких
        float[] allFeatures = new float[totalStates * AiConstants.TABLE_VECTOR_SIZE];
        int currentPos = 0;

        for (BatchRequest r : batch) {
            for (float[] feature : r.features) {
                System.arraycopy(feature, 0, allFeatures, currentPos, feature.length);
                currentPos += feature.length;
            }
        }

        // 2. Создаем тензор ОДНИМ вызовом
        INDArray tableFeatures = Nd4j.create(allFeatures, new int[]{totalStates, AiConstants.TABLE_VECTOR_SIZE}, 'c');

        // 3. Вычисление
        INDArray output = net.output(tableFeatures, false);

        // 4. Оптимизируем чтение (getDouble в цикле тоже медленный)
        float[] outputData = output.data().asFloat(); // Получаем все вероятности разом

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
