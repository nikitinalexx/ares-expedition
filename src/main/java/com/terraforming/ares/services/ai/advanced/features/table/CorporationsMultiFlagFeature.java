package com.terraforming.ares.services.ai.advanced.features.table;

import com.terraforming.ares.cards.blue.AdvancedAlloys;
import com.terraforming.ares.cards.buffedCorporations.BuffedArclightCorporation;
import com.terraforming.ares.cards.corporations.*;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CorporationsMultiFlagFeature implements FeatureBlock {
    public static final List<String> FEATURE_NAMES = List.of(
            // Arclight Corporation
            "arclight_enabled",
            "arclight_total_card_resources",

            // Thorgate + Energy Subsidies
            "thorgate_energy_discount",
            "energy_subsidies_card_per_energy",

            // Teractor + Interplanetary Conference
            "earth_tag_discount",
            "interplanetary_conference_card_per_tag",

            // Advanced Alloys + Phobolog
            "steel_total_discount",
            "titanium_total_discount",
            "steel_bonus_per_unit",
            "titanium_bonus_per_unit",

            // Hyperion / Drone Assisted Construction / Community Gardens
            "mc_gain_on_standard_action",
            "hyperion_extra_mc",
            "drone_assisted_extra_mc_on_upgrade",
            "community_gardens_extra_plant",

            // Exocorp + Composting Factory
            "card_sell_bonus",

            // Apollo + Olympus Conference
            "card_on_science_played",

            // Zetacell + Arctic Algae
            "zetacell_mc_discount_per_ocean",
            "plants_per_ocean",
            "ocean_not_max_signal",

            // Special effects
            "amplify_global_requirement",

            // Nebu Labs + Communications Streamlining
            "card_draw_on_action",
            "cards_played_this_phase_capped",

            // UNMI
            "unmi_enabled",
            "unmi_mc_threshold_reached",

            // Interplanetary Cinematics + Media Group
            "event_mc_gain",

            // Austellar Corporation
            "austellar_enabled",
            "austellar_my_milestone_progress",
            "austellar_opponent_milestone_progress",
            "austellar_progress_delta",
            "austellar_milestone_achieved"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public int size() {
        return 30;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        Map<CardAction, Long> playedCardActions = ctx.getPlayedCardActions();
        Set<Class<?>> playedCardClasses = ctx.getPlayedCardClasses();
        Set<SpecialEffect> specialEffects = ctx.getSpecialEffects();
        CardService cardService = ctx.getCardService();
        Player player = ctx.getPlayer();
        Player opponent = ctx.getOpponent();
        MarsGame game = ctx.getGame();

        {
            boolean arclightCorp = playedCardActions.containsKey(CardAction.ARCLIGHT_CORPORATION);
            out.write(arclightCorp ? 1 : 0);
            out.write(arclightCorp ? player.getCardResourcesCount().getOrDefault(ArclightCorporation.class, 0) + player.getCardResourcesCount().getOrDefault(BuffedArclightCorporation.class, 0) : 0);
        }

        {
            boolean thorgateCorp = playedCardActions.containsKey(CardAction.THORGATE_CORPORATION);
            boolean energySubsidies = playedCardActions.containsKey(CardAction.ENERGY_SUBSIDIES);
            out.write((thorgateCorp ? 3 : 0) + (energySubsidies ? 4 : 0));
            out.write(energySubsidies ? 1 : 0);//gives card per energy tag
        }

        {
            boolean teractorCorp = playedCardActions.containsKey(CardAction.TERACTOR_CORPORATION);
            boolean interplanetaryConference = playedCardActions.containsKey(CardAction.INTERPLANETARY_CONFERENCE);
            out.write((teractorCorp ? 3 : 0) + (interplanetaryConference ? 3 : 0));//earth discounts
            out.write(interplanetaryConference ? 1 : 0);//gives card per earth/jupiter tag. gives 3mc per jupiter tag
        }

        {
            //advanced alloys +1/+1 steel/titanium
            //phobolog +1 per titanium
            boolean phobologCorp = playedCardActions.containsKey(CardAction.PHOBOLOG_CORPORATION);
            boolean advancedAlloys = playedCardClasses.contains(AdvancedAlloys.class);

            float steelBonus = (advancedAlloys ? 1 : 0);
            float totalSteelDiscount = player.getSteelIncome() * (2 + steelBonus);

            float titaniumBonus = (phobologCorp ? 1 : 0) + (advancedAlloys ? 1 : 0);
            float totalTitaniumDiscount = player.getTitaniumIncome() * (3 + titaniumBonus);

            out.write(totalSteelDiscount);
            out.write(totalTitaniumDiscount);
            out.write(steelBonus);
            out.write(titaniumBonus);
        }

        {
            boolean hyperionCorp = playedCardActions.containsKey(CardAction.HYPERION_SYSTEMS_CORPORATION);
            boolean droneAssistedConstruction = playedCardActions.containsKey(CardAction.DRONE_ASSISTED_CONSTRUCTION);
            boolean communityGardens = playedCardActions.containsKey(CardAction.COMMUNITY_GARDENS);

            out.write((hyperionCorp ? 1 : 0) + (droneAssistedConstruction ? 2 : 0) + (communityGardens ? 2 : 0));//total MC gain
            out.write((hyperionCorp ? 1 : 0));//extra MC if chose 3rd
            out.write(droneAssistedConstruction ? 1 : 0);//extra 2mc if played upgraded phase this turn
            out.write((communityGardens ? 1 : 0));//extra Plant if chose 3rd
        }

        {
            boolean exocorp = playedCardActions.containsKey(CardAction.EXOCORP_CORPORATION);
            boolean compostingFactory = playedCardActions.containsKey(CardAction.COMPOSTING_FACTORY);

            int cardSellBonus = (exocorp ? 1 : 0) + (compostingFactory ? 1 : 0);
            out.write(cardSellBonus);
        }

        {
            boolean apollo = playedCardActions.containsKey(CardAction.APOLLO_CORPORATION);
            boolean olympusConference = playedCardActions.containsKey(CardAction.OLYMPUS_CONFERENCE);

            out.write((apollo ? 1 : 0) + (olympusConference ? 1 : 0));//get card on science played
        }

        {
            boolean zetacell = playedCardActions.containsKey(CardAction.ZETACELL_CORPORATION);
            boolean arcticAlgae = playedCardActions.containsKey(CardAction.ARCTIC_ALGAE);

            out.write(zetacell ? 1 : 0);//signals about 2mc discount per ocean
            out.write((zetacell ? 2 : 0) + (arcticAlgae ? 4 : 0));//signals about plants per ocean
            out.write((zetacell || arcticAlgae) && !game.getPlanet().isOceansMax() ? 1 : 0);
        }

        {
            out.write(specialEffects.contains(SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT) ? 1 : 0);
        }

        {
            boolean nebulabsCorp = playedCardActions.containsKey(CardAction.NEBU_LABS_CORPORATION);
            boolean communicationsStreamlining = playedCardActions.containsKey(CardAction.COMMUNICATIONS_STREAMLINING);

            out.write((nebulabsCorp ? 2 : 0) + (communicationsStreamlining ? 1 : 0));
            out.write((nebulabsCorp || communicationsStreamlining) ? Math.min(3, (int) player.getPhaseCards().stream().filter(phaseCard -> phaseCard != 0).count()) : 0);
        }

        {
            boolean unmiCorp = playedCardActions.containsKey(CardAction.UNMI_CORPORATION);
            out.write(unmiCorp ? 1 : 0);
            out.write(player.getMc() >= 6 ? 1 : 0);
        }

        {
            boolean cinematicsCorp = playedCardActions.containsKey(CardAction.INTERPLANETARY_CINEMATICS);
            boolean mediaGroup = playedCardActions.containsKey(CardAction.MEDIA_GROUP);
            out.write((cinematicsCorp ? 2 : 0) + (mediaGroup ? 5 : 0));
        }

        {
            boolean austellarCorp = playedCardActions.containsKey(CardAction.AUSTELLAR_CORPORATION);
            out.write(austellarCorp ? 1 : 0);

            if (austellarCorp) {
                Milestone m = game.getMilestones().get(player.getAustellarMilestone());

                float myProgress = (float) m.getValue(player, cardService) / m.getMaxValue();
                float opponentProgress = (float) m.getValue(opponent, cardService) / m.getMaxValue();

                boolean achieved = m.isAchieved();

                out.write(achieved ? 1.0f : myProgress);
                out.write(achieved ? 1.0f : opponentProgress);
                out.write(achieved ? 0.0f : (myProgress - opponentProgress));
                out.write(achieved ? 1.0f : 0.0f);
            } else {
                out.write(0);
                out.write(0);
                out.write(0);
                out.write(0);
            }
        }
    }
}
