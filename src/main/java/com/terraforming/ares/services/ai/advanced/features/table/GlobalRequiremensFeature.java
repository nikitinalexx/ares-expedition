package com.terraforming.ares.services.ai.advanced.features.table;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.green.*;
import com.terraforming.ares.cards.red.*;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.parameters.OceanRequirement;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalRequiremensFeature implements FeatureBlock {
    public static final double WAIT_PHASE_PENALTY = 0.2;

    private static final Map<Class<?>, Double> TEMPERATURE_RAW = Map.ofEntries(
            Map.entry(CaretakerContract.class, 0.0585),
            Map.entry(ArcticAlgae.class, 0.1816),
            Map.entry(Fish.class, 0.16815),
            Map.entry(GreenHouses.class, 0.095475),
            Map.entry(RegolithEaters.class, 0.1907),
            Map.entry(SmallAnimals.class, 0.139175),
            Map.entry(SymbioticFungus.class, 0.051),
            Map.entry(ArtificialLake.class, 0.134875),
            Map.entry(LakeMariners.class, 0.1453),
            Map.entry(PermafrostExtraction.class, 0.126425),
            Map.entry(IceCapMelting.class, 0.11725),
            Map.entry(Mangrove.class, 0.15005),
            Map.entry(Bushes.class, 0.2764),
            Map.entry(EosChasmaNationalPark.class, 0.244625),
            Map.entry(Farming.class, 0.39305),
            Map.entry(Grass.class, 0.30445),
            Map.entry(NoctisFarming.class, 0.2062),
            Map.entry(TrappedHeat.class, 0.259025),
            Map.entry(Trees.class, 0.382275),
            Map.entry(TundraFarming.class, 0.32365),
            Map.entry(ImportedConstructionCrews.class, 0.130825),
            Map.entry(Dandelions.class, 0.315225)
    );

    private static final Map<Class<?>, Double> OXYGEN_RAW = Map.ofEntries(
            Map.entry(Birds.class, 0.3563),
            Map.entry(Decomposers.class, 0.07795),
            Map.entry(GhgProductionBacteria.class, 0.1824),
            Map.entry(Livestock.class, 0.265025),
            Map.entry(BreathingFilters.class, 0.045625),
            Map.entry(AeratedMagma.class, 0.338325),
            Map.entry(AirborneRadiation.class, 0.241625),
            Map.entry(LowAtmoShields.class, 0.222575),
            Map.entry(MethaneFromTitan.class, 0.3809),
            Map.entry(NaturalPreserve.class, 0.2007),
            Map.entry(Worms.class, 0.240525),
            Map.entry(Zeppelins.class, 0.108275)
    );

    private static final Map<Class<?>, Double> OCEANS_RAW = Map.ofEntries(
            Map.entry(FilterFeeders.class, 0.067875),
            Map.entry(Herbivores.class, 0.299675),
            Map.entry(Algae.class, 0.2724),
            Map.entry(GreatDam.class, 0.20585),
            Map.entry(KelpFarming.class, 0.43855),
            Map.entry(Moss.class, 0.153875),
            Map.entry(RadSuits.class, 0.1567),
            Map.entry(WavePower.class, 0.239475)
    );

    private static final Map<Class<?>, Double> TAGS_RAW = Map.ofEntries(
            Map.entry(AiCentral.class, 0.39435), // 5 science tags
            Map.entry(AntiGravityTechnology.class, 0.485775), // 5 science tags
            Map.entry(PhysicsComplex.class, 0.173425), // 4 science tags
            Map.entry(GeneRepair.class, 0.222075), // 3 science tags
            Map.entry(QuantumExtractor.class, 0.285175), // 3 science tags
            Map.entry(VirtualEmployeeDevelopment.class, 0.20625), // 3 science tags
            Map.entry(MassConverter.class, 0.372375), // 4 science tags
            Map.entry(AtmosphereFiltering.class, 0.1103), // 2 science tags
            Map.entry(InterstellarColonyShip.class, 0.120375), // 4 science tags
            Map.entry(Plantation.class, 0.255), // 4 science tags

            Map.entry(AdvancedEcosystems.class, 0.08895), // ANIMAL+MICROBE+PLANT TAG
            Map.entry(Crater.class, 0.108325), // 3 EVENT tags
            Map.entry(BeamFromThoriumAsteroid.class, 0.33395), // JUPITER tag
            Map.entry(FusionPower.class, 0.235925) // 2 Energy tags
    );

    private static final Map<Class<?>, Double> REQUIRES_HEAT_RAW = Map.ofEntries(
            // Тепло (Heat)
            Map.entry(BuildingIndustries.class, 0.2133), // -4 heat
            Map.entry(FuelFactory.class, 0.196675), // -3 heat
            Map.entry(TropicalResort.class, 0.319225) // -5 heat
    );

    private static final Map<Class<?>, Integer> REQUIRES_HEAT_COUNT = Map.ofEntries(
            // Тепло (Heat)
            Map.entry(BuildingIndustries.class, 4), // -4 heat
            Map.entry(FuelFactory.class, 3), // -3 heat
            Map.entry(TropicalResort.class, 5) // -5 heat
    );

    private static final Map<Class<?>, Double> REQUIRES_PLANT_RAW = Map.ofEntries(
            // Растительность (Plants)
            Map.entry(Moss.class, 0.153875), // Requires 3 oceans, -1 PLANT tag
            Map.entry(BiomassCombustors.class, 0.3189), // -2 plants
            Map.entry(FoodFactory.class, 0.240675) // -2 plants
    );

    private static final Map<Class<?>, Integer> REQUIRES_PLANT_COUNT = Map.ofEntries(
            // Растительность (Plants)
            Map.entry(Moss.class, 1), // Requires 3 oceans, -1 PLANT tag
            Map.entry(BiomassCombustors.class, 2), // -2 plants
            Map.entry(FoodFactory.class, 2) // -2 plants
    );

    public static final List<String> FEATURE_NAMES = List.of(
            "temperature_pressure",
            "oxygen_pressure",
            "oceans_pressure",
            "science_pressure",
            "regular_tag_pressure",
            "3_tag_pressure",
            "resource_pressure"
    );

    @Override
    public int size() {
        return 7;
    }

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        handleTemperature(ctx, out);
        handleOxygen(ctx, out);
        handleOceans(ctx, out);
        handleTags(ctx, out);
        handleResourceRequirements(ctx, out);
    }

    private void handleTemperature(TableContext ctx, FeatureWriter out) {
        List<Card> hand = ctx.getHand();
        float totalTemperaturePressure = 0f;

        // Параметры для динамической гаммы
        int currentEpoch = Math.max(ctx.getGame().getTurns(), 1);
        // Средняя длительность твоей игры
        double progress = Math.min((double) currentEpoch / Constants.ESTIMATED_TURNS_PER_GAME, 1.0);

        // Начальная гамма 0.35 (строгая), к концу падает до 0.05
        double dynamicGamma = Math.max(0.35 - (progress * 0.30), 0.02);

        int currentTemperature = ctx.getGame().getPlanet().getCurrentLevel(GlobalParameter.TEMPERATURE);

        for (Card card : hand) {
            List<ParameterColor> tempReq = card.getTemperatureRequirement();
            if (tempReq == null || tempReq.isEmpty()) continue;

            ParameterColor minReq = tempReq.getFirst();

            // Обработка фиолетового (стартового) цвета — давления нет
            if (minReq == ParameterColor.P) {
                continue;
            }

            // Применяем спецэффект (Amplify)
            if (ctx.isCanAmplifyOxygenOrTemperature() && minReq.ordinal() > 0) {
                minReq = ParameterColor.values()[minReq.ordinal() - 1];
            }

            int minRequiredLevel = ctx.getTemperatureStepFromColor(minReq, false);

            // Если требование уже выполнено — давления нет (0.0)
            if (currentTemperature >= minRequiredLevel) {
                continue;
            }

            Double rawValue = TEMPERATURE_RAW.get(card.getClass());

            // Считаем дистанцию
            int stepsRequired = minRequiredLevel - currentTemperature;

            // Pressure = Недоступный потенциал
            // 1.0 - exp(...) дает 0.0, если steps=0, и стремится к 1.0, если steps огромный
            double cardPressure = rawValue * (1.0 - Math.exp(-dynamicGamma * stepsRequired));

            totalTemperaturePressure += (float) cardPressure;
        }

        // Записываем итоговый признак (Feature)
        out.write(totalTemperaturePressure);
    }

    private void handleOxygen(TableContext ctx, FeatureWriter out) {
        List<Card> hand = ctx.getHand();
        float totalOxygenPressure = 0f;

        int currentEpoch = Math.max(ctx.getGame().getTurns(), 1);
        double progress = Math.min((double) currentEpoch / Constants.ESTIMATED_TURNS_PER_GAME, 1.0);

        // Для кислорода (2.09 ходов на шаг) гамма должна быть чуть выше, чем у температуры
        // Старт: 0.45, Середина: 0.28, Конец: 0.10
        double dynamicGammaO2 = Math.max(0.45 - (progress * 0.35), 0.05);

        int currentOxygen = ctx.getGame().getPlanet().getCurrentLevel(GlobalParameter.OXYGEN);

        for (Card card : hand) {

            List<ParameterColor> oxygenReq = card.getOxygenRequirement();
            if (oxygenReq == null || oxygenReq.isEmpty()) continue;

            ParameterColor minReq = oxygenReq.getFirst();

            // Если требование - фиолетовый (P), это 0-й уровень, давления нет
            if (minReq == ParameterColor.P) {
                continue;
            }

            // Применяем Amplify
            if (ctx.isCanAmplifyOxygenOrTemperature() && minReq.ordinal() > 0) {
                minReq = ParameterColor.values()[minReq.ordinal() - 1];
            }

            int minRequiredLevel = ctx.getOxygenStepFromColor(minReq, false);

            if (currentOxygen >= minRequiredLevel) {
                continue;
            }

            Double rawValue = OXYGEN_RAW.get(card.getClass());

            int stepsRequired = minRequiredLevel - currentOxygen;

            // Pressure = Недоступный потенциал (инвертированная экспонента)
            double cardPressure = rawValue * (1.0 - Math.exp(-dynamicGammaO2 * stepsRequired));

            totalOxygenPressure += (float) cardPressure;
        }

        out.write(totalOxygenPressure);
    }

    private void handleOceans(TableContext ctx, FeatureWriter out) {
        List<Card> hand = ctx.getHand();
        float totalOceanPressure = 0f;

        int currentEpoch = Math.max(ctx.getGame().getTurns(), 1);
        double progress = Math.min((double) currentEpoch / Constants.ESTIMATED_TURNS_PER_GAME, 1.0);

        // Океаны самые медленные (2.54 хода), гамма самая высокая.
        // Старт: 0.55 (очень жестко), Середина: 0.37, Конец: 0.12
        double dynamicGammaOcean = Math.max(0.55 - (progress * 0.43), 0.05);

        int oceansBuilt = ctx.getGame().getPlanet().oceansBuilt();

        for (Card card : hand) {
            OceanRequirement req = card.getOceanRequirement();
            // Нас интересуют только карты с minValue > 0 (давление на вход)
            if (req == null || req.getMinValue() <= 0) {
                continue;
            }

            // Если уже выполнено - давления нет
            if (oceansBuilt >= req.getMinValue()) {
                continue;
            }

            Double rawValue = OCEANS_RAW.get(card.getClass());

            int stepsRequired = req.getMinValue() - oceansBuilt;

            // Расчет давления
            double cardPressure = rawValue * (1.0 - Math.exp(-dynamicGammaOcean * stepsRequired));

            totalOceanPressure += (float) cardPressure;
        }

        out.write(totalOceanPressure);
    }

    private void handleTags(TableContext ctx, FeatureWriter out) {
        List<Card> hand = ctx.getHand();
        float sciencePressure = 0f;
        float commonTagsPressure = 0f; // Энергия, Юпитер, События
        float comboTagsPressure = 0f;  // Сложные сочетания (Зоопарк)

        int currentEpoch = Math.max(ctx.getGame().getTurns(), 1);
        double progress = Math.min((double) currentEpoch / Constants.ESTIMATED_TURNS_PER_GAME, 1.0);

        double gammaScience = Math.max(0.35 - (progress * 0.15), 0.15);
        double gammaCommon = 0.5;
        double gammaCombo = 0.7; // Самый жесткий штраф за "разношерстные" требования

        for (Card card : hand) {
            List<Tag> tagRequirements = card.getTagRequirements();
            if (CollectionUtils.isEmpty(tagRequirements)) continue;

            // 1. Считаем науку
            int scienceRequired = 0;
            for (Tag t : tagRequirements) {
                if (t == Tag.SCIENCE) scienceRequired++;
            }

            if (scienceRequired > 0) {
                int owned = ctx.getPlayedTagToCount().getOrDefault(Tag.SCIENCE, 0L).intValue();
                if (owned < scienceRequired) {

                    sciencePressure += (float) (TAGS_RAW.get(card.getClass()) * (1.0 - Math.exp(-gammaScience * (scienceRequired - owned))));
                }
                continue;
            }

            // 2. Проверяем на "Комбо" (Advanced Ecosystems и аналоги)
            // Если в требованиях больше 2 разных типов тегов (не считая науку)
            long distinctNonScienceTags = tagRequirements.stream()
                    .filter(t -> t != Tag.SCIENCE)
                    .distinct()
                    .count();

            if (distinctNonScienceTags >= 3) {
                double cardMaxGap = 0;
                for (Tag t : tagRequirements.stream().distinct().toList()) {
                    int owned = ctx.getPlayedTagToCount().getOrDefault(t, 0L).intValue();
                    if (owned == 0) { // Для комбо критично наличие хотя бы одного тега
                        cardMaxGap = Math.max(cardMaxGap, TAGS_RAW.get(card.getClass()) * (1.0 - Math.exp(-gammaCombo * 1)));
                    }
                }
                comboTagsPressure += (float) cardMaxGap;
                continue;
            }

            // 3. Остальные обычные теги
            Map<Tag, Integer> otherReqs = new HashMap<>();
            for (Tag t : tagRequirements) {
                otherReqs.put(t, otherReqs.getOrDefault(t, 0) + 1);
            }

            double cardMaxP = 0;
            for (var entry : otherReqs.entrySet()) {
                int owned = ctx.getPlayedTagToCount().getOrDefault(entry.getKey(), 0L).intValue();
                if (owned < entry.getValue()) {
                    double p = TAGS_RAW.get(card.getClass()) * (1.0 - Math.exp(-gammaCommon * (entry.getValue() - owned)));
                    cardMaxP = Math.max(cardMaxP, p);
                }
            }
            commonTagsPressure += (float) cardMaxP;
        }

        out.write(sciencePressure);
        out.write(commonTagsPressure);
        out.write(comboTagsPressure);
    }

    private void handleResourceRequirements(TableContext ctx, FeatureWriter out) {
        List<Card> hand = ctx.getHand();
        Player player = ctx.getPlayer();

        float resourcePressure = 0f;

        // Галма для ресурсов остается высокой
        double gammaResource = 0.8;

        for (Card card : hand) {

            // 1. РАСТЕНИЯ
            int plantReq = REQUIRES_PLANT_COUNT.getOrDefault(card.getClass(), 0);
            if (plantReq > 0) {
                int currentPlants = player.getPlants();
                int plantProd = player.getPlantsIncome();

                // Если текущие + доход покрывают нужду, gap = 0.1 (небольшой штраф за ожидание фазы)
                // Иначе gap = нехватка
                double gap;
                if (currentPlants + plantProd >= plantReq) {
                    gap = (currentPlants >= plantReq) ? 0.0 : WAIT_PHASE_PENALTY;
                } else {
                    gap = plantReq - (currentPlants + plantProd);
                }
                if (gap > 0) {
                    resourcePressure += (float) (REQUIRES_PLANT_RAW.get(card.getClass()) * (1.0 - Math.exp(-gammaResource * gap)));
                }
            }

            // 2. ТЕПЛО
            int heatReq = REQUIRES_HEAT_COUNT.getOrDefault(card.getClass(), 0);
            if (heatReq > 0) {
                int currentHeat = player.getHeat();
                int heatProd = player.getHeatIncome();

                double gap;
                if (currentHeat + heatProd >= heatReq) {
                    gap = (currentHeat >= heatReq) ? 0.0 : WAIT_PHASE_PENALTY;
                } else {
                    gap = heatReq - (currentHeat + heatProd);
                }
                if (gap > 0) {
                    resourcePressure += (float) (REQUIRES_HEAT_RAW.get(card.getClass()) * (1.0 - Math.exp(-gammaResource * gap)));
                }
            }

        }

        out.write(resourcePressure);
    }

}
