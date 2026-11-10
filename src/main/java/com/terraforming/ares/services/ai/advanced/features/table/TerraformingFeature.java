package com.terraforming.ares.services.ai.advanced.features.table;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Planet;
import com.terraforming.ares.model.parameters.Ocean;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;

public class TerraformingFeature implements FeatureBlock {
    public static final List<String> FEATURE_NAMES = List.of(
            "oxygen_left",
            "temperature_left",
            "oxygen_still_can",
            "temperature_still_can",
            "oxygen_1_revealed",
            "oxygen_2_revealed",
            "oxygen_3_revealed",
            "oxygen_4_revealed",
            "oxygen_5_revealed",
            "oxygen_6_revealed",
            "oxygen_7_revealed",
            "oxygen_8_revealed",
            "oxygen_9_revealed",
            "last_cards",
            "last_mc",
            "last_plants"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public int size() {
        return 16;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        MarsGame game = ctx.getGame();
        Planet planet = game.getPlanet();

        float oxygenLeftRatio = (float) planet.oxygenLeft() / planet.oxygenMax();
        float temperatureLeftRatio = (float) planet.temperatureLeft() / planet.temperatureMax();

        out.write(oxygenLeftRatio);
        out.write(temperatureLeftRatio);

        Planet planetAtTheStartOfThePhase = ctx.getGame().getPlanetAtTheStartOfThePhase();

        out.write(planetAtTheStartOfThePhase.isOxygenMax() ? 0 : 1);
        out.write(planetAtTheStartOfThePhase.isTemperatureMax() ? 0 : 1);

        List<Ocean> oceans = planet.getOceans();
        boolean[] used = new boolean[AiConstants.ALL_OCEANS.size()];
        boolean[] revealed = new boolean[AiConstants.ALL_OCEANS.size()];

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
            revealed[index] = ocean.isRevealed();
        }

        for (int i = 0; i < revealed.length; i++) {
            out.write(revealed[i] ? 0 : 1);
        }

        if (planet.isOceansMax() && !planetAtTheStartOfThePhase.allOceansRevealed()) {
            Ocean lastOpenedOcean =
                    planetAtTheStartOfThePhase
                            .getOceans()
                            .get(planetAtTheStartOfThePhase.getLastOpenedOceanIndex());
            out.write(lastOpenedOcean.getCards());
            out.write(lastOpenedOcean.getMc());
            out.write(lastOpenedOcean.getPlants());
        } else {
            out.write(0);
            out.write(0);
            out.write(0);
        }
    }

}
