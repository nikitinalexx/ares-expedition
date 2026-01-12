package com.terraforming.ares.services.ai.turnProcessors.network2.projection;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.advanced.IDataCollect;
import com.terraforming.ares.services.ai.dl4j.NNService;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchProjectionService {
    private final NNService nnService;

    public void executeAll(Player player, List<ProjectionTask<?>> tasks) {
        if (tasks.isEmpty()) return;

        // 1. Собираем все флоаты от всех провайдеров (карты, платежи и т.д.)
        List<float[]> allInputs = new ArrayList<>();
        for (ProjectionTask<?> task : tasks) {
            allInputs.addAll(task.getProjections());
        }

        // 2. ОДИН вызов нейронки на всех
        List<Prediction> allPredictions = nnService.predictBatch(
                allInputs,
                NNService.ModelType.OPTIMIZED
        );

        // 3. Раздаем результаты обратно по коллбекам
        int cursor = 0;
        for (ProjectionTask<?> task : tasks) {
            int size = task.size();
            List<Prediction> subList = allPredictions.subList(cursor, cursor + size);
            task.consumePredictions(subList);
            cursor += size;
        }
    }

}
