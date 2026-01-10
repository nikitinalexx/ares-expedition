package com.terraforming.ares.services.ai.turnProcessors.network2.projection;

import com.terraforming.ares.services.ai.dl4j.Prediction;

import java.util.List;

public interface ProjectionTask<T> {
    /** Создает список флоатов для нейронки.
     Один объект может генерировать несколько проекций (например, разные способы оплаты) */
    List<float[]> getProjections();

    int size();

    /** Принимает результаты предиктов, относящиеся именно к этому объекту */
    void consumePredictions(List<Prediction> predictions);

    T getResults();
}
