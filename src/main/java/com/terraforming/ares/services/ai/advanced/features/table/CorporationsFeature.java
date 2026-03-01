package com.terraforming.ares.services.ai.advanced.features.table;

import com.terraforming.ares.cards.buffedCorporations.BuffedArclightCorporation;
import com.terraforming.ares.cards.corporations.*;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CorporationsFeature implements FeatureBlock {
    public static final List<String> FEATURE_NAMES = List.of(
            "helion",
            "ecoline",
            "mining_guild",
            "saturn_systems",
            "phobolog",
            "inventrix",
            "unmi_corp",
            "arclight",
            "arclight_animals",
            "celestior",
            "thorgate",
            "devtechs",
            "launcstar",
            "credicor",
            "modpro",
            "mayni",
            "sultira",
            "teractor",
            "tharsis",
            "hyperion",
            "exocorp",
            "apolloindustries",
            "zetacell",
            "nebulabs",
            "interplanetary"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public int size() {
        return 9 + CORPORATIONS_WITH_SINGLE_FLAG.size();//9+16=25
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        Map<CardAction, Long> playedCardActions = ctx.getPlayedCardActions();
        Set<Class<?>> playedCardClasses = ctx.getPlayedCardClasses();
        Player player = ctx.getPlayer();

        out.write(playedCardActions.containsKey(CardAction.HELION_CORPORATION) ? 1 : 0);//can be buffed corporation, so requires separate field
        out.write(playedCardActions.containsKey(CardAction.ECOLINE_CORPORATION) ? 1 : 0);//can be buffed
        out.write(playedCardActions.containsKey(CardAction.MINING_GUILD_CORPORATION) ? 1 : 0);//can be buffed
        out.write(playedCardActions.containsKey(CardAction.SATURN_SYSTEMS_CORPORATION) ? 1 : 0);//can be buffed
        out.write(playedCardActions.containsKey(CardAction.PHOBOLOG_CORPORATION) ? 1 : 0);//can be buffed
        out.write(playedCardActions.containsKey(CardAction.INVENTRIX_CORPORATION) ? 1 : 0);//can be buffed
        out.write(playedCardActions.containsKey(CardAction.UNMI_CORPORATION) ? 1 : 0);//can be buffed


        {
            boolean arclightCorp = playedCardActions.containsKey(CardAction.ARCLIGHT_CORPORATION);
            out.write(arclightCorp ? 1 : 0);
            out.write(arclightCorp ? player.getCardResourcesCount().getOrDefault(ArclightCorporation.class, 0) + player.getCardResourcesCount().getOrDefault(BuffedArclightCorporation.class, 0) : 0);
        }


        for (Class<?> aClass : CORPORATIONS_WITH_SINGLE_FLAG) {
            out.write(playedCardClasses.contains(aClass) ? 1 : 0);
        }
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
            InterplanetaryCinematics.class
    );
}
