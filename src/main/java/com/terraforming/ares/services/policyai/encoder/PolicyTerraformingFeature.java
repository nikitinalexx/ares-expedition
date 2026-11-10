package com.terraforming.ares.services.policyai.encoder;

import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.parameters.Ocean;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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

        List<Ocean> oceans = planet.getOceans();
        boolean[] used = new boolean[AiConstants.ALL_OCEANS.size()];

        for (Ocean ocean : oceans) {

            int index = -1;

            for (int j = 0; j < AiConstants.ALL_OCEANS.size(); j++) {
                Ocean ref = AiConstants.ALL_OCEANS.get(j);

                if (!used[j]
                        && ref.getCards() == ocean.getCards()
                        && ref.getMc() == ocean.getMc()
                        && ref.getPlants() == ocean.getPlants()) {

                    index = j;
                    used[j] = true;
                    break;
                }
            }

            if (!ocean.isRevealed()) {
                openOceansMask |= (1 << index);
            }
        }

        dto.openOceans = (short) openOceansMask;

        if (openOceansMask == 0 && !planetAtTheStartOfThePhase.allOceansRevealed()) {
            Ocean lastOpenedOcean =
                    planetAtTheStartOfThePhase
                            .getOceans()
                            .get(planetAtTheStartOfThePhase.getLastOpenedOceanIndex());

            dto.lastOceanCards = (byte) lastOpenedOcean.getCards();
            dto.lastOceanMc = (byte) lastOpenedOcean.getMc();
            dto.lastOceanPlants = (byte) lastOpenedOcean.getPlants();
        }
    }

}
