package com.terraforming.ares.services.policyai.encoder;

import com.terraforming.ares.cards.buffedCorporations.BuffedArclightCorporation;
import com.terraforming.ares.cards.corporations.*;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PolicyCorporationsFeature implements PlayerPolicyFeatureBlock {
    private static final int HELION_BIT = 0;
    private static final int ECOLINE_BIT = 1;
    private static final int MINING_GUILD_BIT = 2;
    private static final int SATURN_BIT = 3;
    private static final int PHOBOLOG_BIT = 4;
    private static final int INVENTRIX_BIT = 5;
    private static final int UNMI_BIT = 6;
    private static final int ARCLIGHT_BIT = 7;

    private static final int SINGLE_FLAG_OFFSET = 8;

    @Override
    public void encode(TableContext ctx, PolicyRecord dto, int playerIndex) {
        Map<CardAction, Long> played = ctx.getPlayedCardActions();
        Set<Class<?>> playedClasses = ctx.getPlayedCardClasses();
        Player player = ctx.getPlayer();

        if (player.getSelectedCorporationCard() == null) {
            return;
        }

        int mask = 0;

        if (played.containsKey(CardAction.HELION_CORPORATION))
            mask |= (1 << HELION_BIT);

        if (played.containsKey(CardAction.ECOLINE_CORPORATION))
            mask |= (1 << ECOLINE_BIT);

        if (played.containsKey(CardAction.MINING_GUILD_CORPORATION))
            mask |= (1 << MINING_GUILD_BIT);

        if (played.containsKey(CardAction.SATURN_SYSTEMS_CORPORATION))
            mask |= (1 << SATURN_BIT);

        if (played.containsKey(CardAction.PHOBOLOG_CORPORATION))
            mask |= (1 << PHOBOLOG_BIT);

        if (played.containsKey(CardAction.INVENTRIX_CORPORATION))
            mask |= (1 << INVENTRIX_BIT);

        if (played.containsKey(CardAction.UNMI_CORPORATION))
            mask |= (1 << UNMI_BIT);

        boolean arclight = played.containsKey(CardAction.ARCLIGHT_CORPORATION);

        if (arclight) {
            mask |= (1 << ARCLIGHT_BIT);

            int resources =
                    player.getCardResourcesCount()
                            .getOrDefault(ArclightCorporation.class, 0)
                            + player.getCardResourcesCount()
                            .getOrDefault(BuffedArclightCorporation.class, 0);

            dto.arclightResources[playerIndex] = (byte) resources;
        } else {
            dto.arclightResources[playerIndex] = 0;
        }


        for (int i = 0; i < CORPORATIONS_WITH_SINGLE_FLAG.size(); i++) {
            if (playedClasses.contains(CORPORATIONS_WITH_SINGLE_FLAG.get(i))) {

                mask |= (1 << (SINGLE_FLAG_OFFSET + i));
            }
        }

        dto.corporationMask[playerIndex] = mask;
    }

    //16
    private final List<Class<?>> CORPORATIONS_WITH_SINGLE_FLAG = List.of(
            CelestiorCorporation.class,
            ThorgateCorporation.class,
            DevTechs.class,
            LaunchStarIncorporated.class,
            CredicorCorporation.class,
            ModproCorporation.class,
            MayNiProductionsCorporation.class,
            SultiraCorporation.class,
            TeractorCorporation.class,
            TharsisCorporation.class,
            HyperionSystemsCorporation.class,
            ExocorpCorporation.class,
            ApolloIndustriesCorporation.class,
            ZetacellCorporation.class,
            NebuLabsCorporation.class,
            InterplanetaryCinematics.class,
            AustellarCorporation.class
    );


}
