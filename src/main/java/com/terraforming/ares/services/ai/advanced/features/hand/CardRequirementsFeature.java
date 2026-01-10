package com.terraforming.ares.services.ai.advanced.features.hand;

import com.terraforming.ares.cards.blue.AdaptationTechnology;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.GlobalParameter;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.parameters.OceanRequirement;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.terraforming.ares.model.parameters.ParameterColor.*;

public class CardRequirementsFeature implements FeatureBlock {
    @Override
    public int size() {
        return 39; // oxygen(6*2) + temp(6*2) + ocean(4) + global(7) + science(4) = 14+14+4+7+4 = 43
    }

    public static final List<String> FEATURE_NAMES = List.of(

            // --- Oxygen requirements (current amplify state) ---
            "o2_ready_current",
            "o2_critical_near_current",
            "o2_next_zone_or_far_current",
            "o2_min_req_mean_current",
            "o2_min_req_max_current",
            "o2_max_req_min_current",

            // --- Oxygen requirements (potential amplify state) ---
            "o2_ready_potential",
            "o2_critical_near_potential",
            "o2_next_zone_or_far_potential",
            "o2_min_req_mean_potential",
            "o2_min_req_max_potential",
            "o2_max_req_min_potential",

            // --- Temperature requirements (current amplify state) ---
            "temp_ready_current",
            "temp_critical_near_current",
            "temp_next_zone_or_far_current",
            "temp_min_req_mean_current",
            "temp_min_req_max_current",
            "temp_max_req_min_current",

            // --- Temperature requirements (potential amplify state) ---
            "temp_ready_potential",
            "temp_critical_near_potential",
            "temp_next_zone_or_far_potential",
            "temp_min_req_mean_potential",
            "temp_min_req_max_potential",
            "temp_max_req_min_potential",

            // --- Ocean requirements ---
            "ocean_ready",
            "ocean_near_ready",
            "ocean_cards_with_requirements",
            "ocean_closest_activation_norm",

            // --- Global progress ratios ---
            "oxygen_left_ratio",
            "oxygen_built_ratio",
            "temperature_left_ratio",
            "temperature_built_ratio",
            "oceans_left_ratio",
            "oceans_built_ratio",
            "total_global_progress",

            // --- Science tag requirements ---
            "science_max_required_norm",
            "science_mean_required_norm",
            "science_cards_with_req_count",
            "science_total_gap"
    );

    private static final int O2_MAX_LIMIT = 14;
    private static final int TEMP_MAX_LIMIT = 19;
    private static final float OCEAN_MAX_NORM = 9.0f;



    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        MarsGame game = ctx.getGame();
        List<Card> hand = ctx.getHand();
        boolean canAmplifyOxygenOrTemperature = ctx.isCanAmplifyOxygenOrTemperature();
        Set<Class<?>> handCardClasses = ctx.getHandCardClasses();

        // Oxygen requirements (current amplify state)
        extractOxygenRequirements(out, game, canAmplifyOxygenOrTemperature, hand);

        // Oxygen requirements (potential amplify state)
        boolean potentialAmplify = canAmplifyOxygenOrTemperature || handCardClasses.contains(AdaptationTechnology.class);
        extractOxygenRequirements(out, game, potentialAmplify, hand);

        // Temperature requirements (current and potential amplify states)
        extractTemperatureRequirements(out, game, canAmplifyOxygenOrTemperature, hand);
        extractTemperatureRequirements(out, game, potentialAmplify, hand);

        // Ocean requirements
        extractOceanRequirements(out, game, hand);

        // Global progress ratios
        Planet planet = game.getPlanet();
        float oxygenLeftRatio = (float) planet.oxygenLeft() / planet.oxygenMax();
        float temperatureLeftRatio = (float) planet.temperatureLeft() / planet.temperatureMax();
        float oceansLeftRatio = (float) planet.oceansLeft() / planet.oceansMaxCount();

        out.write(oxygenLeftRatio);
        out.write(1f - oxygenLeftRatio);
        out.write(temperatureLeftRatio);
        out.write(1f - temperatureLeftRatio);
        out.write(oceansLeftRatio);
        out.write(1f - oceansLeftRatio);

        float totalProgress = planet.oceansBuilt()
                + planet.getCurrentLevel(GlobalParameter.TEMPERATURE)
                + planet.getCurrentLevel(GlobalParameter.OXYGEN);
        out.write(totalProgress);

        // --- 4.1 Science requirements ---
        int science = ctx.getPlayedTagToCount().getOrDefault(Tag.SCIENCE, 0L).intValue();

        int maxReq = 0;
        int nonZeroCount = 0;
        float sumReq = 0f;

        for (Card c : hand) {
            List<Tag> tagRequirements = c.getTagRequirements();
            if (tagRequirements.contains(Tag.SCIENCE)) {
                int req = tagRequirements.size();
                maxReq = Math.max(maxReq, req);
                sumReq += req;
                nonZeroCount++;
            }
        }

        float meanReq = nonZeroCount > 0 ? sumReq / nonZeroCount : 0f;

        out.write(maxReq / 5.0f);
        out.write(meanReq / 5.0f);
        out.write(nonZeroCount);

        float scienceGap = 0f;
        for (Card card : hand) {
            if (card.getTagRequirements().contains(Tag.SCIENCE)) {
                scienceGap += Math.max(0,
                        card.getTagRequirements().size() - science);
            }
        }

        out.write(scienceGap);
    }

    private void extractOxygenRequirements(FeatureWriter out, MarsGame game, boolean canAmplify, List<Card> hand) {
        int currentValue = game.getPlanet().getCurrentLevel(GlobalParameter.OXYGEN);

        int ready = 0;
        int criticalNear = 0;
        int nextZoneOrFar = 0;

        float minReqSum = 0, minReqMax = Float.NEGATIVE_INFINITY;
        float maxReqMin = Float.POSITIVE_INFINITY;
        int reqCount = 0;

        for (Card card : hand) {
            Collection<ParameterColor> oxygenReq = card.getOxygenRequirement();
            if (CollectionUtils.isEmpty(oxygenReq)) {
                continue;
            }

            int[] boundaries = getOxygenBoundaries(card, canAmplify);
            int minReq = boundaries[0];
            int maxReq = boundaries[1];

            // Accumulate stats
            minReqSum += minReq;
            minReqMax = Math.max(minReqMax, minReq);
            maxReqMin = Math.min(maxReqMin, maxReq);
            reqCount++;

            // Проверяем доступность карты: она ready если текущее значение попадает в [minReq, maxReq]
            boolean isReady = currentValue >= minReq && currentValue <= maxReq;

            if (isReady) {
                ready++;
            } else {
                // Карта недоступна - анализируем почему
                int diffToMin = minReq - currentValue;
                int diffToMax = currentValue - maxReq;

                if (diffToMin > 0) {
                    // Значение ниже минимума - карта станет доступной при росте параметра
                    if (diffToMin == 1) {
                        criticalNear++;
                    } else {
                        nextZoneOrFar++;
                    }
                } else if (diffToMax > 0) {
                    // Значение выше максимума - карта навсегда недоступна (параметр растёт только вверх)
                    // Это критическая информация для нейронки!
                    // Можно добавить в отдельный счётчик, но пока просто не учитываем
                }
            }
        }

        out.write(ready);
        out.write(criticalNear);
        out.write(nextZoneOrFar);

        // Write normalized stats
        if (reqCount > 0) {
            out.write((minReqSum / reqCount) / O2_MAX_LIMIT); // mean
            out.write(minReqMax / O2_MAX_LIMIT);
            out.write(maxReqMin / O2_MAX_LIMIT);
        } else {
            out.write(0f);
            out.write(0f);
            out.write(0f);
        }
    }

    private void extractTemperatureRequirements(FeatureWriter out, MarsGame game, boolean canAmplify, List<Card> hand) {
        int currentValue = game.getPlanet().getCurrentLevel(GlobalParameter.TEMPERATURE);
        ParameterColor currentColor = game.getPlanet().getTemperatureColor();

        int ready = 0;
        int criticalNear = 0;
        int nextZoneOrFar = 0;

        float minReqSum = 0, minReqMax = Float.NEGATIVE_INFINITY;
        float maxReqMin = Float.POSITIVE_INFINITY;
        int reqCount = 0;

        for (Card card : hand) {
            Collection<ParameterColor> tempReq = card.getTemperatureRequirement();
            if (CollectionUtils.isEmpty(tempReq)) {
                continue;
            }

            int[] boundaries = getTemperatureBoundaries(card, canAmplify);
            int minReq = boundaries[0];
            int maxReq = boundaries[1];

            // Accumulate stats
            minReqSum += minReq;
            minReqMax = Math.max(minReqMax, minReq);
            maxReqMin = Math.min(maxReqMin, maxReq);
            reqCount++;

            // Проверяем доступность карты: она ready если текущее значение попадает в [minReq, maxReq]
            boolean isReady = currentValue >= minReq && currentValue <= maxReq;

            if (isReady) {
                ready++;
            } else {
                // Карта недоступна - анализируем почему
                int diffToMin = minReq - currentValue;
                int diffToMax = currentValue - maxReq;

                if (diffToMin > 0) {
                    // Значение ниже минимума - карта станет доступной при росте параметра
                    if (diffToMin == 1) {
                        criticalNear++;
                    } else {
                        nextZoneOrFar++;
                    }
                } else if (diffToMax > 0) {
                    // Значение выше максимума - карта навсегда недоступна
                    // Не учитываем в тактических счётчиках
                }
            }
        }

        out.write(ready);
        out.write(criticalNear);
        out.write(nextZoneOrFar);

        // Write normalized stats
        if (reqCount > 0) {
            out.write((minReqSum / reqCount) / TEMP_MAX_LIMIT); // mean
            out.write(minReqMax / TEMP_MAX_LIMIT);
            out.write(maxReqMin / TEMP_MAX_LIMIT);
        } else {
            out.write(0f);
            out.write(0f);
            out.write(0f);
        }
    }

    private void extractOceanRequirements(FeatureWriter out, MarsGame game, List<Card> hand) {
        int currentOceans = game.getPlanet().oceansBuilt();

        int ready = 0;
        int nearReady = 0; // В пределах 1-2 океанов от активации
        int cardsWithReqs = 0;
        float closestActivation = Float.POSITIVE_INFINITY; // Ближайший порог активации

        for (Card card : hand) {
            OceanRequirement req = card.getOceanRequirement();
            if (req == null) {
                continue;
            }

            cardsWithReqs++;
            int minReq = req.getMinValue();
            int maxReq = req.getMaxValue();

            // Проверяем доступность: текущее значение должно быть в [minReq, maxReq]
            if (currentOceans >= minReq && currentOceans <= maxReq) {
                ready++;
            } else {
                int diffToMin = minReq - currentOceans;

                if (diffToMin > 0) {
                    // Карта ещё не активна - считаем расстояние до активации
                    closestActivation = Math.min(closestActivation, diffToMin);

                    if (diffToMin <= 2) {
                        nearReady++; // Скоро станет доступной
                    }
                }
                // Если currentOceans > maxReq - карта навсегда недоступна (не учитываем)
            }
        }

        // 4 параметра для нейронки:
        // 1. Сколько карт доступно прямо сейчас
        out.write((float) ready);

        // 2. Сколько карт скоро станут доступны (1-2 океана)
        out.write((float) nearReady);

        // 3. Сколько всего карт с требованиями по океанам (контекст для интерпретации ready/nearReady)
        out.write((float) cardsWithReqs);

        // 4. Нормализованное расстояние до ближайшей активации (0 если все доступны или нет карт)
        if (closestActivation != Float.POSITIVE_INFINITY) {
            out.write(closestActivation / OCEAN_MAX_NORM);
        } else {
            out.write(0f);
        }
    }

    private int[] getOxygenBoundaries(Card card, boolean canAmplify) {
        Collection<ParameterColor> reqs = card.getOxygenRequirement();
        if (CollectionUtils.isEmpty(reqs)) {
            return new int[]{0, O2_MAX_LIMIT};
        }

        if (canAmplify) {
            Set<ParameterColor> effectiveReqs = new HashSet<>(reqs);
            for (ParameterColor color : reqs) {
                effectiveReqs.addAll(getAdjacentColors(color));
            }
            reqs = effectiveReqs;
        }

        int overallMin = Integer.MAX_VALUE;
        int overallMax = Integer.MIN_VALUE;

        for (ParameterColor color : reqs) {
            int[] range = getOxygenRange(color);
            overallMin = Math.min(overallMin, range[0]);
            overallMax = Math.max(overallMax, range[1]);
        }

        return new int[]{overallMin, Math.min(overallMax, O2_MAX_LIMIT)};
    }

    private int[] getTemperatureBoundaries(Card card, boolean canAmplify) {
        Collection<ParameterColor> reqs = card.getTemperatureRequirement();
        if (CollectionUtils.isEmpty(reqs)) {
            return new int[]{0, TEMP_MAX_LIMIT};
        }

        if (canAmplify) {
            Set<ParameterColor> effectiveReqs = new HashSet<>(reqs);
            for (ParameterColor color : reqs) {
                effectiveReqs.addAll(getAdjacentColors(color));
            }
            reqs = effectiveReqs;
        }

        int overallMin = Integer.MAX_VALUE;
        int overallMax = Integer.MIN_VALUE;

        for (ParameterColor color : reqs) {
            int[] range = getTemperatureRange(color);
            overallMin = Math.min(overallMin, range[0]);
            overallMax = Math.max(overallMax, range[1]);
        }

        return new int[]{overallMin, Math.min(overallMax, TEMP_MAX_LIMIT)};
    }

    private int[] getOxygenRange(ParameterColor color) {
        return switch (color) {
            case P -> new int[]{0, 2};    // Purple: 0-2
            case R -> new int[]{3, 6};    // Red: 3-6
            case Y -> new int[]{7, 11};   // Yellow: 7-11
            case W -> new int[]{12, 14};  // White: 12-14
            default -> new int[]{0, O2_MAX_LIMIT};
        };
    }

    private int[] getTemperatureRange(ParameterColor color) {
        return switch (color) {
            case P -> new int[]{0, 5};    // Purple: 0-5
            case R -> new int[]{6, 10};   // Red: 6-10
            case Y -> new int[]{11, 15};  // Yellow: 11-15
            case W -> new int[]{16, 19};  // White: 16-19
            default -> new int[]{0, TEMP_MAX_LIMIT};
        };
    }

    private ParameterColor getOxygenColorFromValue(int value) {
        if (value <= 2) return P;
        if (value <= 6) return R;
        if (value <= 11) return Y;
        return W;
    }

    private ParameterColor getTemperatureColorFromValue(int value) {
        if (value <= 5) return P;
        if (value <= 10) return R;
        if (value <= 15) return Y;
        return W;
    }

    private List<ParameterColor> getAdjacentColors(ParameterColor color) {
        return switch (color) {
            case P -> List.of(R);
            case R -> List.of(P, Y);
            case Y -> List.of(R, W);
            case W -> List.of(Y);
        };
    }

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }
}