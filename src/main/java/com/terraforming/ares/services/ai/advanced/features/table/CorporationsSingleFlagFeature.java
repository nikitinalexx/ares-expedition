package com.terraforming.ares.services.ai.advanced.features.table;

import com.terraforming.ares.cards.corporations.*;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CorporationsSingleFlagFeature implements FeatureBlock {
    public static final List<String> FEATURE_NAMES = List.of(
            // forest affordability (Ecoline affects cost)
            "can_afford_forest_or_fraction",

            // corporations with special handling (can be buffed)
            "corp_helion",
            "corp_ecoline",
            "corp_mining_guild",
            "corp_saturn_systems",

            // single-flag corporations
            "corp_celestior",
            "corp_dev_techs",
            "corp_launch_star",
            "corp_credicor",
            "corp_modpro",
            "corp_may_ni_productions",
            "corp_sultira"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public int size() {
        return 5 + CORPORATIONS_WITH_SINGLE_FLAG.size();//12
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        Map<CardAction, Long> playedCardActions = ctx.getPlayedCardActions();
        Set<Class<?>> playedCardClasses = ctx.getPlayedCardClasses();
        Player player = ctx.getPlayer();

        float forestCost = playedCardActions.containsKey(CardAction.ECOLINE_CORPORATION) ? 7 : 8;
        out.write(player.getPlants() >= forestCost ? 1f : player.getPlants() / forestCost);

        out.write(playedCardActions.containsKey(CardAction.HELION_CORPORATION) ? 1 : 0);//can be buffed corporation, so requires separate field
        out.write(playedCardActions.containsKey(CardAction.ECOLINE_CORPORATION) ? 1 : 0);//can be buffed
        out.write(playedCardActions.containsKey(CardAction.MINING_GUILD_CORPORATION) ? 1 : 0);//can be buffed
        out.write(playedCardActions.containsKey(CardAction.SATURN_SYSTEMS_CORPORATION) ? 1 : 0);//can be buffed

        for (Class<?> aClass : CORPORATIONS_WITH_SINGLE_FLAG) {
            out.write(playedCardClasses.contains(aClass) ? 1 : 0);
        }
    }

    private final List<Class<?>> CORPORATIONS_WITH_SINGLE_FLAG = List.of(
            CelestiorCorporation.class,
            DevTechs.class,
            LaunchStarIncorporated.class,
            CredicorCorporation.class,
            ModproCorporation.class,
            MayNiProductionsCorporation.class,
            SultiraCorporation.class
    );
}
