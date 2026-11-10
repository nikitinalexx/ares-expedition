package com.terraforming.ares.services.policyai.service;

public class PolicyLogitDeltaHelper {

    public static float calculateLogitAdvantage(float vReality, float vBaseline) {
        // 1. Сначала переводим из [-1, 1] в [0, 1]
        // Теперь vReality -0.93 станет 0.035, что валидно
        float pReality = (vReality + 1f) / 2f;
        float pBaseline = (vBaseline + 1f) / 2f;

        // 2. И только теперь клипаем вероятности!
        float eps = 1e-7f;
        pReality = Math.max(eps, Math.min(1f - eps, pReality));
        pBaseline = Math.max(eps, Math.min(1f - eps, pBaseline));

        // 3. Считаем логиты
        float logitReality = (float) Math.log(pReality / (1f - pReality));
        float logitBaseline = (float) Math.log(pBaseline / (1f - pBaseline));

        return logitReality - logitBaseline;
    }

}
