package com.terraforming.ares.services.ai.advanced.features.hand;

import com.terraforming.ares.cards.corporations.*;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CorporationsHandFeature implements FeatureBlock {
    public static final List<String> FEATURE_NAMES = List.of(
            "helion",
            "ecoline",
            "mining_guild",
            "saturn_systems",
            "phobolog",
            "inventrix",
            "unmi_corp",
            "arclight",
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
        return 8 + CORPORATIONS_WITH_SINGLE_FLAG.size();//8+16=24
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        Map<CardAction, Long> playedCardActions = ctx.getPlayedCardActions();
        Set<Class<?>> playedCardClasses = ctx.getPlayedCardClasses();

        out.write(playedCardActions.containsKey(CardAction.HELION_CORPORATION) ? 1 : 0);//can be buffed corporation, so requires separate field
        out.write(playedCardActions.containsKey(CardAction.ECOLINE_CORPORATION) ? 1 : 0);//can be buffed
        out.write(playedCardActions.containsKey(CardAction.MINING_GUILD_CORPORATION) ? 1 : 0);//can be buffed
        out.write(playedCardActions.containsKey(CardAction.SATURN_SYSTEMS_CORPORATION) ? 1 : 0);//can be buffed
        out.write(playedCardActions.containsKey(CardAction.PHOBOLOG_CORPORATION) ? 1 : 0);//can be buffed
        out.write(playedCardActions.containsKey(CardAction.INVENTRIX_CORPORATION) ? 1 : 0);//can be buffed
        out.write(playedCardActions.containsKey(CardAction.UNMI_CORPORATION) ? 1 : 0);//can be buffed
        out.write(playedCardActions.containsKey(CardAction.ARCLIGHT_CORPORATION) ? 1 : 0);//can be buffed

        for (Class<?> aClass : CORPORATIONS_WITH_SINGLE_FLAG) {
            out.write(playedCardClasses.contains(aClass) ? 1 : 0);
        }
    }

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
