package com.terraforming.ares.services.policyai.service;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.StandardProjectType;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.dl4j.PolicyNNService;
import com.terraforming.ares.services.ai.dl4j.PolicyPrediction;
import com.terraforming.ares.services.policyai.GameArenaContext;
import com.terraforming.ares.services.policyai.action.*;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.encoder.PolicyRecordFeatureConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PolicyActionSelector {
    private final PolicyNNService policyNNService;
    private final PolicyRecordFeatureConverter policyRecordFeatureConverter;


    public HeadAction chooseBestPhaseAction(
            PolicyRecord record,
            List<HeadAction> legalActions
    ) {
        PolicyPrediction predict = null;
        if (AiConstants.POLICY_AVAILABLE) {
            predict = predict(record);
        }

        HeadAction bestAction = null;
        {//random choice
//            bestAction = legalActions.get(ThreadLocalRandom.current().nextInt(legalActions.size()));
            bestAction = choosePhaseAction(predict, legalActions);
        }


        //TODO ask what about this delta, it should be smarter?
        if (GameArenaContext.isArenaAvailable()) {
            PolicyRecord recordToWrite = GameArenaContext.current().next();
            recordToWrite.copyFrom(record);
            recordToWrite.setAction(bestAction);
            recordToWrite.setLegalActionsMask(legalActions, recordToWrite.chosenHead);
            recordToWrite.setLegalHeadMask(legalActions);
            if (AiConstants.POLICY_AVAILABLE) {
                recordToWrite.policyDelta = predict.outcome;
                recordToWrite.isBackwardPropagationStop = true;
                recordToWrite.finalizedDelta = true;//not really, we do this in the controller
            }
        }

        return bestAction;
    }

    public HeadAction chooseBestAction(
            PolicyRecord record,
            List<HeadAction> legalActions,
            MarsGame game
    ) {
        if (legalActions == null || legalActions.isEmpty()) {
            throw new IllegalArgumentException("No valid actions were provided");
        }

        if (legalActions.size() == 1) {
            return legalActions.getFirst();
        }


        PolicyPrediction predict = null;
        if (AiConstants.POLICY_AVAILABLE) {
            predict = predict(record);
        }

        HeadAction bestAction = null;
        {//random choice
//            bestAction = legalActions.get(ThreadLocalRandom.current().nextInt(legalActions.size()));
            bestAction = chooseBestAction(predict, legalActions);
        }

        if (GameArenaContext.isArenaAvailable()) {
            PolicyRecord recordToWrite = GameArenaContext.current().next();
            recordToWrite.copyFrom(record);
            recordToWrite.setAction(bestAction);
            recordToWrite.setLegalActionsMask(legalActions, recordToWrite.chosenHead);
            recordToWrite.setLegalHeadMask(legalActions);
            if (AiConstants.POLICY_AVAILABLE) {
                recordToWrite.policyDelta = predict.outcome;
                if (bestAction.head() == ActionHead.BUILD
                        || bestAction == SystemAction.UNMI_RT_FOR_6_MC
                        || bestAction == SystemAction.TAKE_BONUS
                        || bestAction == SmallAction.CONVERT_HEAT_TO_TEMPERATURE
                        || bestAction == SmallAction.CONVERT_PLANTS_TO_FOREST
                        || bestAction == ActionInputService.standardProject(StandardProjectType.FOREST)
                        || bestAction == ActionInputService.standardProject(StandardProjectType.OCEAN)
                        || bestAction == ActionInputService.standardProject(StandardProjectType.TEMPERATURE)
                        || bestAction.head() == ActionHead.BLUE && bestAction != BlueAction.REDRAFTED_CONTRACTS_PASS && bestAction != BlueAction.POWER_INFRASTRUCTURE_PASS
                        || bestAction == SystemAction.ENTER_HELION_MODE
                        || bestAction == SystemAction.ENTER_SELL_MODE) {
                    recordToWrite.marsReference = new MarsGame(game);
                }
            }
        }

        return bestAction;
    }

    private boolean isPassRecord(PolicyRecord record) {
        return record.chosenHead == ActionHead.HEAD_SELECTION.ordinal() && (record.chosenLocalIndex >= HeadSelectionAction.PASS_PHASE_1.ordinal() && record.chosenLocalIndex <= HeadSelectionAction.PASS_PHASE_3.ordinal());
    }

    public List<PolicyPrediction> predict(List<PolicyRecord> records) {
        List<float[]> data = new ArrayList<>(records.size());
        for (PolicyRecord record : records) {
            data.add(policyRecordFeatureConverter.convert(record));
        }
        return policyNNService.predictBatch(data, PolicyNNService.ModelType.FIRST);
    }


    public PolicyPrediction predict(PolicyRecord record) {
//        return policyNNService.predictBatch(List.of(policyRecordFeatureConverter.convert(record)), record.isFirstPlayer ? PolicyNNService.ModelType.FIRST : PolicyNNService.ModelType.SECOND).getFirst();
        return policyNNService.predictBatch(List.of(policyRecordFeatureConverter.convert(record)), PolicyNNService.ModelType.FIRST).getFirst();
    }

    private HeadAction chooseBestAction(
            PolicyPrediction prediction,
            List<HeadAction> legalActions
    ) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        if (legalActions.isEmpty()) {
            throw new IllegalStateException("No legal actions");
        }

        // Параметры шума
        // Рекомендуемые настройки для "выхода из депрессии паса"
//        float temperatureHead = 1.2f;    // Поднимаем выше 1.0, чтобы притоптать фаворитов
//        float temperatureAction = 1.1f;  // Делаем распределение внутри карт более плоским
//
//        float gumbelHead = 0.5f;         // Усиливаем шум выбора головы (Build/Sell/System)
//        float gumbelAction = 0.4f;       // Резко повышаем шанс выбора непопулярных карт
//
//        float dirichletAlphaAction = 0.05f; // Чем меньше альфа, тем более "пиковым" и случайным будет шум
//        float dirichletMixAction = 0.25f;   // 25% — каждый четвертый ход должен быть "экспериментальным"
        // Параметры для ОСМЫСЛЕННОГО обучения
        float temperatureHead = 0.9f;    // Чуть-чуть поджимаем фаворитов, делаем выбор увереннее
        float temperatureAction = 0.85f; // Карты с хорошей дельтой должны выбираться чаще

        float gumbelHead = 0.15f;        // Легкое дрожание выбора ветки
        float gumbelAction = 0.15f;      // Небольшой шанс выбрать вторую по силе карту

        float dirichletAlphaAction = 0.5f; // Для большого количества карт (268) 0.5 дает адекватный разброс
        float dirichletMixAction = 0.10f;  // 10% экспериментов — этого хватит, чтобы найти новые комбо

        // ---------------- 1. ГРУППИРОВКА ----------------
        // Теперь это критично: в группе может быть терминальное действие (пасс)
        Map<ActionHead, List<HeadAction>> byHead = new EnumMap<>(ActionHead.class);
        for (HeadAction a : legalActions) {
            byHead.computeIfAbsent(a.head(), k -> new ArrayList<>()).add(a);
        }

        // ---------------- 2. ВЫБОР ГОЛОВЫ (HEAD SELECTION) ----------------
        ActionHead chosenHead = null;
        {
            int headCount = byHead.size();
            float[] probsHead = new float[headCount];
            ActionHead[] heads = new ActionHead[headCount];

            int idx = 0;
            float sum = 0f;
            for (ActionHead h : byHead.keySet()) {
                // Берем вероятность из HEAD_SELECTION
                float p = prediction.getActionProbability(ActionHead.HEAD_SELECTION, h.ordinal());
                if (!Float.isFinite(p) || p <= 0f) p = 1e-10f;

                probsHead[idx] = p;
                heads[idx] = h;
                sum += p;
                idx++;
            }

            // Нормализация + Температура
            float tempSum = 0f;
            for (int i = 0; i < headCount; i++) {
                probsHead[i] = (float) Math.pow(probsHead[i] / sum, 1f / temperatureHead);
                tempSum += probsHead[i];
            }

            // Gumbel Sampling для выбора головы
            float bestScore = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < headCount; i++) {
                float u = Math.max(rnd.nextFloat(), 1e-7f);
                float g = (float) -Math.log(-Math.log(u));
                float score = (float) (Math.log(probsHead[i] / tempSum) + gumbelHead * g);

                if (score > bestScore) {
                    bestScore = score;
                    chosenHead = heads[i];
                }
            }
        }

        // ---------------- 3. ПРОВЕРКА НА ТЕРМИНАЛЬНЫЙ ПАСС ----------------
        List<HeadAction> actions = byHead.get(chosenHead);

        // Если выбранная голова — это один из наших новых пассов (7, 8, 9)
        // У них нет "внутренних" действий в рабочих головах, они сами по себе действия.
        if (chosenHead.ordinal() >= 7) {
            // Возвращаем любой (он там обычно один в списке для этой головы)
            return actions.get(0);
        }

        // ---------------- 4. ВЫБОР ВНУТРИ ГОЛОВЫ (ACTIONS IN HEAD) ----------------
        // (Этот блок выполняется только для рабочих голов 0-6)
        int m = actions.size();
        float[] probs = new float[m];
        float sum = 0f;

        for (int i = 0; i < m; i++) {
            HeadAction action = actions.get(i);
            float p = prediction.getActionProbability(action.head(), action.localIndex());
            if (!Float.isFinite(p) || p <= 0f) p = 1e-10f;
            probs[i] = p;
            sum += p;
        }

        // Нормализация
        for (int i = 0; i < m; i++) probs[i] /= sum;

        // Dirichlet Noise
        if (dirichletMixAction > 0) {
            float[] dir = new float[m];
            float dirSum = 0f;
            for (int i = 0; i < m; i++) {
                float g = gamma(dirichletAlphaAction, rnd);
                dir[i] = Float.isFinite(g) && g > 0 ? g : 1e-8f;
                dirSum += dir[i];
            }
            for (int i = 0; i < m; i++) {
                probs[i] = (1f - dirichletMixAction) * probs[i] + dirichletMixAction * (dir[i] / dirSum);
            }
        }

        // Температура
        float finalSum = 0f;
        for (int i = 0; i < m; i++) {
            probs[i] = (float) Math.pow(probs[i], 1f / temperatureAction);
            finalSum += probs[i];
        }

        // Финальный Gumbel выбор
        HeadAction best = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < m; i++) {
            float u = Math.max(rnd.nextFloat(), 1e-7f);
            float g = (float) -Math.log(-Math.log(u));
            float score = (float) (Math.log(probs[i] / finalSum) + gumbelAction * g);

            if (score > bestScore) {
                bestScore = score;
                best = actions.get(i);
            }
        }

        return (best != null) ? best : actions.get(rnd.nextInt(m));
    }

    private HeadAction choosePhaseAction(
            PolicyPrediction prediction,
            List<HeadAction> legalActions
    ) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int m = legalActions.size();
        if (m == 0) throw new IllegalStateException("No legal phase actions");

        // --- Параметры агрессивной эксплорации ---
//        float temperature = 1.1f;       // Слегка разглаживаем пики
//        float gumbelScale = 0.5f;      // Сила Гамбел-шума
//        float dirichletMix = 0.3f;     // 30% веса отдаем абсолютно случайному шуму
//        float dirichletAlpha = 0.5f;   // Для 5 действий alpha=0.5 дает хороший разброс
        float temperature = 0.8f;      // Обостряем выбор (теперь лидеры станут заметнее)
        float gumbelScale = 0.15f;     // Легкое дрожание, чтобы не было жесткого детерминизма
        float dirichletMix = 0.1f;     // Только 10% на случайные идеи
        float dirichletAlpha = 1.0f;   // При 5 фазах alpha=1.0 дает более ровный (не экстремальный) шум

        float[] probs = new float[m];
        float sum = 0f;

        // 1. Извлекаем базовые вероятности
        for (int i = 0; i < m; i++) {
            HeadAction action = legalActions.get(i);
            float p = prediction.getActionProbability(action.head(), action.localIndex());
            probs[i] = (Float.isFinite(p) && p > 0) ? p : 1e-10f;
            sum += probs[i];
        }

        // 2. Нормализация и Temperature
        for (int i = 0; i < m; i++) {
            probs[i] = (float) Math.pow(probs[i] / sum, 1.0 / temperature);
        }

        // Пересчитываем сумму после температуры
        float tempSum = 0;
        for (float p : probs) tempSum += p;
        for (int i = 0; i < m; i++) probs[i] /= tempSum;

        // 3. ПОДМЕШИВАЕМ ШУМ ДИРИХЛЕ (AlphaZero style)
        // Это заставит бота пробовать даже те фазы, которые сеть считает "плохими"
        float[] dNoise = sampleDirichlet(m, dirichletAlpha);
        for (int i = 0; i < m; i++) {
            probs[i] = (1.0f - dirichletMix) * probs[i] + dirichletMix * dNoise[i];
        }

        // 4. Gumbel Sampling для финального выбора
        HeadAction best = null;
        float bestScore = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < m; i++) {
            float u = Math.max(rnd.nextFloat(), 1e-7f);
            float g = (float) -Math.log(-Math.log(u));

            // Используем логарифм от уже зашумленных вероятностей
            float score = (float) (Math.log(probs[i]) + gumbelScale * g);

            if (score > bestScore) {
                bestScore = score;
                best = legalActions.get(i);
            }
        }

        return (best != null) ? best : legalActions.get(rnd.nextInt(m));
    }

    // Вспомогательный метод для генерации шума Дирихле
    private float[] sampleDirichlet(int k, float alpha) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        float[] samples = new float[k];
        float sum = 0;
        for (int i = 0; i < k; i++) {
            // Гамма-распределение через аппроксимацию для малых alpha
            double u = rnd.nextDouble(1e-7, 1.0);
            samples[i] = (float) Math.pow(u, 1.0 / alpha);
            sum += samples[i];
        }
        for (int i = 0; i < k; i++) samples[i] /= sum;
        return samples;
    }

    private float gamma(float alpha, ThreadLocalRandom rnd) {
        if (alpha < 1f) {
            while (true) {
                float u = safeRandom(rnd);
                float v = safeRandom(rnd);

                float x = (float) Math.pow(u, 1f / alpha);
                float y = (float) Math.pow(v, 1f / (1f - alpha));

                if (x + y <= 1f) {
                    float e = (float) -Math.log(safeRandom(rnd));
                    return e * x / (x + y);
                }
            }
        } else {
            float d = alpha - 1f / 3f;
            float c = (float) (1 / Math.sqrt(9 * d));

            while (true) {
                float x = (float) rnd.nextGaussian();
                float v = 1 + c * x;

                if (v <= 0) continue;

                v = v * v * v;

                float u = safeRandom(rnd);

                if (u < 1 - 0.0331f * x * x * x * x) {
                    return d * v;
                }

                if (Math.log(u) < 0.5f * x * x + d * (1 - v + Math.log(v))) {
                    return d * v;
                }
            }
        }
    }

    private float safeRandom(ThreadLocalRandom rnd) {
        return Math.max(rnd.nextFloat(), 1e-7f);
    }

}
