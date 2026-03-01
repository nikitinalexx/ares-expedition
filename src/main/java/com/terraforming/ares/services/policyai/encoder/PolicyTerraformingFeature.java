package com.terraforming.ares.services.policyai.encoder;

import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.parameters.Ocean;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PolicyTerraformingFeature implements PolicyFeatureBlock {

    @Override
    public void encode(TableContext ctx, PolicyRecord dto) {
        Planet planet = ctx.getGame().getPlanet();

        dto.oxygenLeft = (byte) planet.oxygenLeft();
        dto.temperatureLeft = (byte) planet.temperatureLeft();

        Planet planetAtTheStartOfThePhase = ctx.getGame().getPlanetAtTheStartOfThePhase();

        dto.stillCanDoOxygen = !planetAtTheStartOfThePhase.isOxygenMax();
        dto.stillCanDoTemperature = !planetAtTheStartOfThePhase.isTemperatureMax();

        int openOceansMask = 0;

        // счётчики использований типов
        int[] used = new int[1 << 24];

        for (Ocean ocean : planet.getOceans()) {

            int type = ocean.key();

            int index = TYPE_TO_INDEX[type][used[type]++];

            if (!ocean.isRevealed()) {
                openOceansMask |= (1 << index);
            }
        }
        dto.openOceans = (short) openOceansMask;

        if (openOceansMask == 0 && !planetAtTheStartOfThePhase.allOceansRevealed()) {
            Ocean lastOpenedOcean = planetAtTheStartOfThePhase.getOceans().get(planetAtTheStartOfThePhase.getLastOpenedOceanIndex());
            dto.lastOceanCards = (byte) lastOpenedOcean.getCards();
            dto.lastOceanMc = (byte) lastOpenedOcean.getMc();
            dto.lastOceanPlants = (byte) lastOpenedOcean.getPlants();
        }

    }

    public static final List<Ocean> ALL_OCEANS = List.of(
            new Ocean(0, 0, 2),
            new Ocean(0, 4, 0),
            new Ocean(1, 1, 0),
            new Ocean(0, 2, 1),
            new Ocean(1, 0, 1),
            new Ocean(1, 0, 0),
            new Ocean(0, 1, 1),
            new Ocean(1, 0, 0),
            new Ocean(0, 0, 2)
    );

    private static final int MAX_TYPE = 1 << 24;

    public static final int[][] TYPE_TO_INDEX;

    static {
        Map<Integer, int[]> temp = new HashMap<>();
        Map<Integer, Integer> counters = new HashMap<>();

        for (int i = 0; i < ALL_OCEANS.size(); i++) {
            int type = ALL_OCEANS.get(i).key();

            temp.computeIfAbsent(type, k -> new int[2]);
            temp.get(type)[counters.getOrDefault(type, 0)] = i;

            counters.merge(type, 1, Integer::sum);
        }

        TYPE_TO_INDEX = new int[MAX_TYPE][];

        for (var e : temp.entrySet()) {
            TYPE_TO_INDEX[e.getKey()] = e.getValue();
        }
    }

}
