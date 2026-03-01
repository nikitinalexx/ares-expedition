package com.terraforming.ares.services.policyai.encoder;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.green.CommunicationsStreamlining;
import com.terraforming.ares.cards.green.ImmigrationShuttles;
import com.terraforming.ares.cards.green.IoMiningIndustries;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.util.List;
import java.util.Set;

public class PolicyTableBlueCardsFeature implements PlayerPolicyFeatureBlock {

    @Override
    public void encode(TableContext ctx, PolicyRecord dto, int playerIndex) {
        Set<Class<?>> played = ctx.getPlayedCardClasses();

        long mask0 = 0L;
        long mask1 = 0L;

        for (int i = 0; i < AiConstants.BLUE_CARDS_WITH_SINGLE_FLAG.size(); i++) {

            if (!played.contains(AiConstants.BLUE_CARDS_WITH_SINGLE_FLAG.get(i))) {
                continue;
            }

            if (i < 64) {
                mask0 |= (1L << i);
            } else {
                mask1 |= (1L << (i - 64));
            }
        }

        dto.playedBlueCards[playerIndex][0] = mask0;
        dto.playedBlueCards[playerIndex][1] = mask1;
    }




}
