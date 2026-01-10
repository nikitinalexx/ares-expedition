package com.terraforming.ares.services.ai.advanced.features.hand;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.green.*;
import com.terraforming.ares.cards.red.*;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SingleParamHandFeature implements FeatureBlock {
    @Override
    public int size() {
        return 42;
    }

    public static final List<String> FEATURE_NAMES = List.of(
            // hand card presence
            "hand_microprocessors",
            "hand_invention_contest",
            "hand_assorted_enterprises",
            "hand_business_contracts",
            "hand_ceos_favorite_project",
            "hand_special_design",
            "hand_biomedical_imports",
            "hand_advanced_screening_technology",

            // hand card actions / effects
            "hand_processing_plant_action",
            "hand_innovative_technologies_award_phase_cards",
            "hand_adaptation_technology_blocked",
            "has_amplify_global_requirement_effect",

            // restructured resources
            "hand_restructured_resources",
            "played_restructured_resources",

            // development center
            "hand_development_center",
            "played_development_center",

            // asset liquidation
            "hand_asset_liquidation",
            "played_asset_liquidation",

            // brainstorming session
            "hand_brainstorming_session",
            "played_brainstorming_session",

            // farmers market
            "hand_farmers_market",
            "played_farmers_market",

            // farming coops
            "hand_farming_coops_action",
            "played_farming_coops_action",

            // research grant
            "hand_research_grant_action",
            "played_research_grant_action",

            // processed metals
            "hand_processed_metals_action",
            "hand_processed_metals_energy",

            // hydro electric
            "hand_hydro_electric_action",
            "played_hydro_electric_action",

            // cost reducers
            "hand_cost_reduction_sum",
            "played_cost_reduction_sum",

            // beam from thorium asteroid
            "hand_beam_from_thorium_asteroid_jupiter_bonus",
            "hand_beam_from_thorium_asteroid_active",

            // fusion power
            "hand_fusion_power_energy_bonus",
            "hand_fusion_power_active",

            // private investor beach
            "hand_private_investor_beach_with_milestone",

            // immediate resource gains
            "immediate_microbe_gain_sum",
            "immediate_animal_gain_sum",
            "immediate_plant_gain_capacity",
            "immediate_microbe_gain_capacity",
            "immediate_animal_gain_capacity"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        Set<Class<?>> handCardClasses = ctx.getHandCardClasses();
        Map<CardAction, Long> handCardActions = ctx.getHandCardActions();
        Set<Class<?>> playedCardClasses = ctx.getPlayedCardClasses();
        Map<CardAction, Long> playedCardActions = ctx.getPlayedCardActions();
        Player player = ctx.getPlayer();
        MarsGame game = ctx.getGame();
        boolean canAmplifyOxygenOrTemperature = ctx.isCanAmplifyOxygenOrTemperature();
        Set<SpecialEffect> specialEffects = ctx.getSpecialEffects();

        float playedEnergy = ctx.getPlayedTagToCount().getOrDefault(Tag.ENERGY, 0L).floatValue();
        float playedJupiters = ctx.getPlayedTagToCount().getOrDefault(Tag.JUPITER, 0L).floatValue();
        int[] handTagCounts = ctx.getHandTagCounts();


        out.write(handCardClasses.contains(Microprocessors.class) ? 1 : 0);//draw 2 cards then discard a card
        out.write(handCardClasses.contains(InventionContest.class) ? 1 : 0);//draw 3 cards then discard a card
        out.write(handCardClasses.contains(AssortedEnterprises.class) ? 1 : 0);//lets u build any extra card with 2mc discount
        out.write(handCardClasses.contains(BusinessContracts.class) ? 1 : 0);//take 4 cards and discard 2
        out.write(handCardClasses.contains(CeosFavoriteProject.class) ? 1 : 0);//can put any 2 resources
        out.write(handCardClasses.contains(SpecialDesign.class) ? 1 : 0);//lets you build 1 building with +- requirement
        out.write(handCardClasses.contains(BiomedicalImports.class) ? 1 : 0);//raise oxygen or improve phase
        out.write(handCardClasses.contains(AdvancedScreeningTechnology.class) ? 1 : 0);


        out.write(handCardActions.containsKey(CardAction.PROCESSING_PLANT) ? 1 : 0);//gives card with building tag
        out.write(handCardClasses.contains(InnovativeTechnologiesAward.class) ? player.getPhaseCards().stream().filter(phaseCard -> phaseCard != 0).count() : 0);
        out.write(handCardClasses.contains(AdaptationTechnology.class) && !canAmplifyOxygenOrTemperature ? 1 : 0);
        out.write(specialEffects.contains(SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT) ? 1 : 0);

        out.write(handCardClasses.contains(RestructuredResources.class) ? 1 : 0);//handled by log of plants and plantsincome
        out.write(playedCardClasses.contains(RestructuredResources.class) ? 1 : 0);//handled by log of plants and plantsincome

        out.write(handCardClasses.contains(DevelopmentCenter.class) ? 1 : 0);//handled by log of heat and heatincome
        out.write(playedCardClasses.contains(DevelopmentCenter.class) ? 1 : 0);//handled by log of heat and heatincome

        out.write(handCardClasses.contains(AssetLiquidation.class) ? 1 : 0);
        out.write(playedCardClasses.contains(AssetLiquidation.class) ? 1 : 0);

        out.write(handCardClasses.contains(BrainstormingSession.class) ? 1 : 0);
        out.write(playedCardClasses.contains(BrainstormingSession.class) ? 1 : 0);

        out.write(handCardClasses.contains(FarmersMarket.class) ? 1 : 0);
        out.write(playedCardClasses.contains(FarmersMarket.class) ? 1 : 0);

        out.write(handCardActions.containsKey(CardAction.FARMING_COOPS) ? 1 : 0);//can be buffed
        out.write(playedCardActions.containsKey(CardAction.FARMING_COOPS) ? 1 : 0);//can be buffed

        out.write(handCardActions.containsKey(CardAction.RESEARCH_GRANT) ? 1 : 0);
        out.write(playedCardActions.containsKey(CardAction.RESEARCH_GRANT) ? 1 : 0);

        out.write(handCardActions.containsKey(CardAction.PROCESSED_METALS) ? 1 : 0);
        out.write(handCardActions.containsKey(CardAction.PROCESSED_METALS) ? playedEnergy : 0);

        out.write(handCardActions.containsKey(CardAction.HYDRO_ELECTRIC) ? 1 : 0);
        out.write(playedCardActions.containsKey(CardAction.HYDRO_ELECTRIC) ? 1 : 0);

        out.write((handCardClasses.contains(EarthCatapult.class) ? 2 : 0) + (handCardClasses.contains(ResearchOutpost.class) ? 1 : 0) + (handCardClasses.contains(HohmannTransferShipping.class) ? 1 : 0));
        out.write((playedCardClasses.contains(EarthCatapult.class) ? 2 : 0) + (playedCardClasses.contains(ResearchOutpost.class) ? 1 : 0) + (playedCardClasses.contains(HohmannTransferShipping.class) ? 1 : 0));

        out.write(handCardClasses.contains(BeamFromThoriumAsteroid.class) ? Math.max(0, handTagCounts[Tag.JUPITER.ordinal()] - 1) : 0);
        out.write(handCardClasses.contains(BeamFromThoriumAsteroid.class) && playedJupiters >= 1 ? 1 : 0);

        out.write(handCardClasses.contains(FusionPower.class) ? Math.max(0, handTagCounts[Tag.ENERGY.ordinal()] - 1) : 0);
        out.write(handCardClasses.contains(FusionPower.class) && playedEnergy >= 2 ? 1 : 0);

        out.write((handCardClasses.contains(PrivateInvestorBeach.class)) && game.getMilestones().stream().anyMatch(milestone -> milestone.isAchieved(player)) ? 1 : 0);


        out.write(getImmediateMicrobeGainSum(handCardActions));//cards that when built put microbes on any other chosen card
        out.write(getImmediateAnimalGainSum(handCardActions));//cards that when built put animals on any other chose card
        out.write(getImmediatePlantGainCapacity(handCardActions));
        out.write(getImmediateMicrobeGainCapacity(handCardActions, handCardClasses));
        out.write(getImmediateAnimalGainCapacity(handCardActions, handCardClasses));
    }

    private int getImmediateMicrobeGainSum(Map<CardAction, Long> handCardActions) {
        return (handCardActions.containsKey(CardAction.ASTROFARM) ? 2 : 0) + (handCardActions.containsKey(CardAction.IMPORTED_NITROGEN) ? 3 : 0);
    }

    private int getImmediatePlantGainCapacity(Map<CardAction, Long> handCardActions) {
        return (handCardActions.containsKey(CardAction.IMPORTED_HYDROGEN) ? 3 : 0) + (handCardActions.containsKey(CardAction.LARGE_CONVOY) ? 5 : 0);
    }

    private int getImmediateAnimalGainCapacity(Map<CardAction, Long> handCardActions, Set<Class<?>> handCardClasses) {
        return (handCardClasses.contains(CeosFavoriteProject.class) ? 2 : 0) + (handCardActions.containsKey(CardAction.IMPORTED_HYDROGEN) ? 2 : 0) + (handCardActions.containsKey(CardAction.LARGE_CONVOY) ? 3 : 0);
    }

    private int getImmediateMicrobeGainCapacity(Map<CardAction, Long> handCardActions, Set<Class<?>> handCardClasses) {
        return (handCardClasses.contains(CeosFavoriteProject.class) ? 2 : 0) + (handCardActions.containsKey(CardAction.IMPORTED_HYDROGEN) ? 3 : 0) + (handCardActions.containsKey(CardAction.LOCAL_HEAT_TRAPPING) ? 2 : 0) + (handCardActions.containsKey(CardAction.CRYOGENIC_SHIPMENT) ? 3 : 0);
    }

    private int getImmediateAnimalGainSum(Map<CardAction, Long> handCardActions) {
        return (handCardActions.containsKey(CardAction.EOS_CHASMA) ? 1 : 0) + (handCardActions.containsKey(CardAction.IMPORTED_NITROGEN) ? 2 : 0) + (handCardActions.containsKey(CardAction.LOCAL_HEAT_TRAPPING) ? 2 : 0) + (handCardActions.containsKey(CardAction.CRYOGENIC_SHIPMENT) ? 2 : 0);
    }

}
