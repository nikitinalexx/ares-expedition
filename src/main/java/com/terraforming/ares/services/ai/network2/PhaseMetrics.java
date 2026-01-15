package com.terraforming.ares.services.ai.network2;

import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

public class PhaseMetrics {
    // Мапы для хранения времени (в наносекундах) и количества вызовов
    public static final Map<Integer, LongAdder> totalTime = Map.of(
            1, new LongAdder(), 2, new LongAdder(), 3, new LongAdder(),
            4, new LongAdder(), 5, new LongAdder()
    );
    public static final Map<Integer, LongAdder> callCount = Map.of(
            1, new LongAdder(), 2, new LongAdder(), 3, new LongAdder(),
            4, new LongAdder(), 5, new LongAdder()
    );

    public static final LongAdder FIRST = new LongAdder();
    public static final LongAdder SECOND = new LongAdder();
    public static final LongAdder THIRD = new LongAdder();
    public static final LongAdder FOURTH = new LongAdder();

    public static final LongAdder QUEUE_ADD_TIME = new LongAdder();
    public static final LongAdder FUTURE_JOIN_TIME = new LongAdder();

    public static final LongAdder BATCH_TOTAL_SIZE = new LongAdder(); // Суммарное кол-во состояний
    public static final LongAdder BATCH_COUNT = new LongAdder();      // Кол-во запусков инференса
    public static final LongAdder BATCHER_IDLE_TIME = new LongAdder(); // Время ожидания первого запроса

    public static final LongAdder INF_PREPARE_DATA = new LongAdder(); // Сборка массива и создание INDArray
    public static final LongAdder INF_GPU_COMPUTE = new LongAdder();  // Чистая работа net.output
    public static final LongAdder INF_POST_PROCESS = new LongAdder(); // Распаковка и завершение Future

    public static void reset() {
        totalTime.values().forEach(LongAdder::reset);
        callCount.values().forEach(LongAdder::reset);
    }
}
