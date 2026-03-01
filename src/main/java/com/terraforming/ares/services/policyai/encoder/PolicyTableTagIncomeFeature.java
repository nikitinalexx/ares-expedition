package com.terraforming.ares.services.policyai.encoder;

import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.util.Map;

public class PolicyTableTagIncomeFeature implements PlayerPolicyFeatureBlock {
    public static final CardAction[] ENGINE_ACTION_ORDER = {
            CardAction.HEAT_EARTH_INCOME,
            CardAction.MC_ANIMAL_PLANT_INCOME,
            CardAction.CARD_SCIENCE_INCOME,
            CardAction.MC_EARTH_INCOME,
            CardAction.PLANT_PLANT_INCOME,
            CardAction.MC_SCIENCE_INCOME,
            CardAction.MC_2_BUILDING_INCOME,
            CardAction.MC_ENERGY_INCOME,
            CardAction.MC_SPACE_INCOME,
            CardAction.HEAT_SPACE_INCOME,
            CardAction.MC_EVENT_INCOME,
            CardAction.HEAT_ENERGY_INCOME,
            CardAction.PLANT_MICROBE_INCOME,
            CardAction.MC_FOREST_INCOME
    };

    public static final int ENGINE_ACTIONS = ENGINE_ACTION_ORDER.length;

    @Override
    public void encode(TableContext ctx, PolicyRecord out, int playerIndex) {

        Map<CardAction, Long> played = ctx.getPlayedCardActions();

        for (int actionIndex = 0; actionIndex < ENGINE_ACTIONS; actionIndex++) {
            CardAction action = ENGINE_ACTION_ORDER[actionIndex];

            long value = played.getOrDefault(action, 0L);

            out.engineActionCount[playerIndex * ENGINE_ACTIONS + actionIndex] = (byte) value;
        }

    }

}
