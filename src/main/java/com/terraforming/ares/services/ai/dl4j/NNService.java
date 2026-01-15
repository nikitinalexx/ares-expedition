package com.terraforming.ares.services.ai.dl4j;

import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.network2.PhaseMetrics;
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
    public static final int MAX_REQUESTS_PER_TICK = 256;//64
    public static final int MAX_STATES_PER_BATCH = 4096;//512

    // Перечисление для выбора модели
    public enum ModelType { OPTIMIZED }

    // Две отдельные очереди для разных моделей
    private final Queue<BatchRequest> queue = new ConcurrentLinkedQueue<>();

    private final MultiLayerNetwork optimizedNet;

    public NNService() throws Exception {
        // Загружаем обе модели
        // Используем модель 3-й эпохи, так как она показала лучший баланс
        this.optimizedNet = MultiLayerNetwork.load(new File("mix_4_5_8587_4958.zip"), false);

        // Запускаем два независимых батчера
//        Thread.ofPlatform().name("nn-batcher-opt").start(() -> batchLoop(ModelType.OPTIMIZED, optimizedNet));

        Thread.ofVirtual()
                .name("Batcher-" + 0)
                .start(() -> batchLoop(ModelType.OPTIMIZED, optimizedNet));
    }

    /**
     * Теперь принимает тип модели, в которую нужно отправить батч
     */
    public List<Prediction> predictBatch(List<float[]> featuresBatch, ModelType modelType) {
        if (featuresBatch.isEmpty()) return List.of();

        long start = System.nanoTime();

        CompletableFuture<List<Prediction>> future = new CompletableFuture<>();
        queue.add(new BatchRequest(featuresBatch, future));

        long afterAdd = System.nanoTime();
        PhaseMetrics.QUEUE_ADD_TIME.add(afterAdd - start);

        List<Prediction> result = future.join();

        PhaseMetrics.FUTURE_JOIN_TIME.add(System.nanoTime() - afterAdd);

        return result;
    }

    private void batchLoop(ModelType type, MultiLayerNetwork net) {
        // Теперь это ConcurrentLinkedQueue (интерфейс Queue)
        final long BATCH_WINDOW_NANOS = 4_000_000; // 2 мс
        final int TARGET_BATCH_SIZE = 1024;

        while (!Thread.currentThread().isInterrupted()) {
            List<BatchRequest> batch = new ArrayList<>();
            try {
                // 1. ОЖИДАНИЕ ПЕРВОГО ЭЛЕМЕНТА (Замена queue.take())
                long idleStart = System.nanoTime();
                BatchRequest first = null;

                while ((first = queue.poll()) == null) {
                    // Если поток прерван — выходим
                    if (Thread.currentThread().isInterrupted()) return;

                    // Важно: onSpinWait дает сигналу процессору, что мы в пустом цикле ожидания.
                    // Это гораздо эффективнее для задержек, чем Thread.sleep или yield.
                    Thread.onSpinWait();
                }
                PhaseMetrics.BATCHER_IDLE_TIME.add(System.nanoTime() - idleStart);

                batch.add(first);
                int currentStatesCount = first.features.size();

                // 2. СБОР БАТЧА (Микрозадержка)
                long deadline = System.nanoTime() + BATCH_WINDOW_NANOS;

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

                PhaseMetrics.BATCH_TOTAL_SIZE.add(currentStatesCount);
                PhaseMetrics.BATCH_COUNT.add(1);

                processInference(batch, net, currentStatesCount);

            } catch (Throwable t) {
                if (t instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
                t.printStackTrace();
                for (BatchRequest r : batch) r.future.completeExceptionally(t);
            }
        }
    }

    private void processInference(List<BatchRequest> batch, MultiLayerNetwork net, int totalStates) {
        long start = System.nanoTime();

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

        long afterPrepare = System.nanoTime();
        PhaseMetrics.INF_PREPARE_DATA.add(afterPrepare - start);

        // 2. Вычисление (GPU)
        INDArray output = net.output(tableFeatures, false);

        long afterCompute = System.nanoTime();
        PhaseMetrics.INF_GPU_COMPUTE.add(afterCompute - afterPrepare);

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

        PhaseMetrics.INF_POST_PROCESS.add(System.nanoTime() - afterCompute);
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
