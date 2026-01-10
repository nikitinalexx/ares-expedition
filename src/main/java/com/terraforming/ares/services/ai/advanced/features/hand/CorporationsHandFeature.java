package com.terraforming.ares.services.ai.advanced.features.hand;

import com.terraforming.ares.cards.blue.AdvancedAlloys;
import com.terraforming.ares.cards.blue.CommunityGardens;
import com.terraforming.ares.cards.blue.DroneAssistedConstruction;
import com.terraforming.ares.cards.blue.OlympusConference;
import com.terraforming.ares.cards.corporations.ApolloIndustriesCorporation;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.awards.BaseAward;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CorporationsHandFeature implements FeatureBlock {
    @Override
    public int size() {
        return 102;
    }

    private static final List<String> FEATURE_NAMES = List.of(
            // --- Science / Apollo / Conference ---
            "hand_science_tag_count",
            "science_cost_discount_ratio_apollo_or_conference",
            "conference_in_hand",

            // --- Energy discounts (Thorgate / Energy Subsidies) ---
            "energy_subsidies_in_hand",
            "energy_subsidies_played",
            "energy_active_discount",
            "energy_cost_in_hand",
            "energy_discounted_value",

            // --- Nebu Labs / Communications Streamlining ---
            "communications_streamlining_in_hand",
            "communications_active_bonus",
            "active_phase_card_count",

            // --- Ecoline ---
            "ecoline_corporation_played",
            "plant_income_sum",
            "plant_tag_count_in_hand",

            // --- Mayni ---
            "mayni_corporation_played",
            "green_card_count_in_hand",

            // --- UNMI ---
            "unmi_corporation_played",
            "total_income_sum",

            // --- Zetacell / Arctic Algae ---
            "zetacell_corporation_played",
            "arctic_algae_in_hand",
            "arctic_algae_played",
            "oxygen_ocean_bonus",
            "oceans_left_ratio",
            "oxygen_left_ratio",
            "min_red_temperature_progress",

            // --- Helion / Power Infrastructure ---
            "helion_corporation_played",
            "power_infrastructure_in_hand",
            "power_infrastructure_played",
            "log_heat_income_helion",
            "log_heat_amount_helion",
            "log_heat_income_pi",
            "log_heat_amount_pi",

            // --- Mining Guild ---
            "mining_guild_corporation_played",
            "steel_income_non_zero",

            // --- Saturn Systems ---
            "saturn_systems_corporation_played",
            "jupiter_tag_count_in_hand",

            // --- Teractor / Interplanetary Conference ---
            "interplanetary_conference_in_hand",
            "interplanetary_conference_played",
            "earth_discount_active",
            "jupiter_discount_active",
            "log_earth_cost_in_hand",
            "log_jupiter_cost_in_hand",
            "log_earth_cost_with_discount",
            "log_jupiter_cost_with_discount",

            // --- Celestior ---
            "celestior_corporation_played",

            // --- Modpro ---
            "modpro_corporation_played",

            // --- DevTechs ---
            "devtechs_corporation_played",
            "green_card_count_for_devtechs",
            "log_green_card_cost_for_devtechs",

            // --- Sultira ---
            "sultira_corporation_played",
            "energy_tag_count_in_hand",
            "log_energy_card_cost_in_hand",

            // --- Launch Star ---
            "launch_star_corporation_played",
            "blue_card_count_in_hand",
            "log_blue_card_cost_in_hand",

            // --- Credicor ---
            "credicor_corporation_played",
            "expensive_card_count_in_hand",
            "log_expensive_card_cost_in_hand",

            // --- Arclight ---
            "arclight_corporation_played",
            "bio_tag_count_in_hand",

            // --- Advanced Alloys / Phobolog ---
            "log_steel_buying_power",
            "log_titanium_buying_power",
            "log_steel_power_for_building_tags",
            "log_titanium_power_for_space_tags",
            "log_potential_steel_power",
            "log_potential_titanium_power",

            // --- Hyperion / Community Gardens / Drone AC ---
            "community_gardens_in_hand",
            "drone_assisted_construction_in_hand",
            "community_gardens_played",
            "hyperion_corporation_played",
            "phase_3_mc_bonus",

            // --- Exocorp / Composting / Card selling ---
            "active_card_sell_bonus",
            "card_income",
            "hand_size",
            "composting_factory_in_hand",
            "card_to_mc_margin_active",
            "card_to_mc_margin_potential",

            // --- Events (Cinematics + synergies) ---
            "event_mc_discount_active",
            "event_mc_discount_potential",
            "optimal_aerobraking_in_hand",
            "optimal_aerobraking_played",
            "event_card_draw_bonus_active",
            "event_card_draw_bonus_potential",
            "event_tag_count_active",
            "event_tag_count_potential",

            // --- Austellar ---
            "austellar_corporation_played",
            "austellar_milestone_progress",

            // --- Milestones (per milestone) ---
            "milestone_1_my_progress_ratio",
            "milestone_1_opponent_progress_ratio",
            "milestone_1_achieved",
            "milestone_1_hand_progress",

            "milestone_2_my_progress_ratio",
            "milestone_2_opponent_progress_ratio",
            "milestone_2_achieved",
            "milestone_2_hand_progress",

            "milestone_3_my_progress_ratio",
            "milestone_3_opponent_progress_ratio",
            "milestone_3_achieved",
            "milestone_3_hand_progress",

            // --- Awards (per award) ---
            "award_1_hand_progress_log",
            "award_2_hand_progress_log",
            "award_3_hand_progress_log"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        MarsGame game = ctx.getGame();
        Player player = ctx.getPlayer();
        Player opponent = ctx.getOpponent();

        List<Card> hand = ctx.getHand();
        Set<Class<?>> handCardClasses = ctx.getHandCardClasses();
        Set<Class<?>> playedCardClasses = ctx.getPlayedCardClasses();
        Map<CardAction, Long> playedCardActions = ctx.getPlayedCardActions();
        Map<CardAction, Long> handCardActions = ctx.getHandCardActions();
        HandEncoderHelperService handEncoderHelperService = ctx.getHandEncoderHelperService();
        CardService cardService = ctx.getCardService();

        int[] handTagCounts = ctx.getHandTagCounts();

        {
            boolean apolloCorp = playedCardClasses.contains(ApolloIndustriesCorporation.class);
            boolean conferenceHand = handCardClasses.contains(OlympusConference.class);
            boolean conferenceTable = playedCardClasses.contains(OlympusConference.class);

            out.write(handTagCounts[Tag.SCIENCE.ordinal()]);
            out.write(((apolloCorp ? 1 : 0) + (conferenceTable ? 1 : 0)) / 2f);
            out.write(conferenceHand ? 1 : 0);
        }

        {
            boolean hasThorgateCorp = playedCardActions.containsKey(CardAction.THORGATE_CORPORATION);//-3 mc

            boolean subsidesInHand = handCardActions.containsKey(CardAction.ENERGY_SUBSIDIES);//-4 mc and a card
            boolean subsidesPlayed = playedCardActions.containsKey(CardAction.ENERGY_SUBSIDIES);

            out.write(subsidesInHand ? 1.0f : 0.0f);//need to track because effect also gives a card
            out.write(subsidesPlayed ? 1.0f : 0.0f);

            float activeDiscount = (hasThorgateCorp ? 3.0f : 0.0f) + (subsidesPlayed ? 4.0f : 0.0f);
            float potentialDiscount = subsidesInHand ? 4.0f : 0.0f;

            out.write(activeDiscount);

            float energyCostInHand = getTagTotalCostInHand(hand, Tag.ENERGY);
            out.write(energyCostInHand);

            out.write((float) ((activeDiscount + potentialDiscount) * Math.log1p(energyCostInHand)));
        }

        {
            boolean nebulabCorp = playedCardActions.containsKey(CardAction.NEBU_LABS_CORPORATION);
            boolean handCS = handCardActions.containsKey(CardAction.COMMUNICATIONS_STREAMLINING);
            boolean playedCS = playedCardActions.containsKey(CardAction.COMMUNICATIONS_STREAMLINING);
            out.write(handCS ? 1.0f : 0.0f);
            out.write((nebulabCorp ? 2 : 0) + (playedCS ? 1 : 0));

            if (nebulabCorp || handCS || playedCS) {
                out.write(Math.min(3, (int) player.getPhaseCards().stream().filter(phaseCard -> phaseCard != 0).count()));
            } else {
                out.write(0f);
            }
        }

        Map<GainType, Stats> gainTypeToCounts = getTerraformingGains(hand);
        Map<GainType, Stats> incomeStats = getRequiredIncomeStats(hand);

        {
            boolean ecolineCorp = playedCardActions.containsKey(CardAction.ECOLINE_CORPORATION);
            out.write(ecolineCorp ? 1.0f : 0.0f);

            if (ecolineCorp) {
                out.write(incomeStats.get(GainType.PLANT).sum);
                out.write(handTagCounts[Tag.PLANT.ordinal()]);
            } else {
                out.write(0f);
                out.write(0f);
            }
        }

        {
            long greenCount = hand.stream().filter(card -> card.getColor() == CardColor.GREEN).count();

            boolean mayniCorp = playedCardActions.containsKey(CardAction.MAY_NI_PRODUCTIONS_CORPORATION);
            out.write(mayniCorp ? 1.0f : 0.0f);
            out.write(mayniCorp ? greenCount : 0);
        }

        {
            boolean unmiCorp = playedCardActions.containsKey(CardAction.UNMI_CORPORATION);
            out.write(unmiCorp ? 1.0f : 0.0f);
            out.write(unmiCorp ? (float) gainTypeToCounts.values().stream().mapToDouble(stat -> stat.sum).sum() : 0);
        }

        {
            float oxygenLeftRatio = (float) game.getPlanet().oxygenLeft() / game.getPlanet().oxygenMax();
            float oceansLeftRatio = (float) game.getPlanet().oceansLeft() / game.getPlanet().oceansMaxCount();

            boolean zetacellCorp = playedCardActions.containsKey(CardAction.ZETACELL_CORPORATION);
            boolean handAlgae = handCardActions.containsKey(CardAction.ARCTIC_ALGAE);
            boolean playedAlgae = playedCardActions.containsKey(CardAction.ARCTIC_ALGAE);

            out.write(zetacellCorp ? 1 : 0);
            out.write(handAlgae ? 1 : 0);
            out.write(playedAlgae ? 1 : 0);

            out.write((zetacellCorp ? 2 : 0) + (playedAlgae ? 4 : 0));

            out.write((handAlgae || playedAlgae || zetacellCorp) ? oceansLeftRatio : 0);
            out.write((handAlgae || playedAlgae || zetacellCorp) ? oxygenLeftRatio : 0);

            out.write(handAlgae ? ctx.getMinRedTemperatureAvailabilityProgress() : 0);
        }

        {
            boolean isHelionCorp = playedCardActions.containsKey(CardAction.HELION_CORPORATION);
            boolean handPI = handCardActions.containsKey(CardAction.POWER_INFRASTRUCTURE);
            boolean playedPI = playedCardActions.containsKey(CardAction.POWER_INFRASTRUCTURE);

            out.write(isHelionCorp ? 1 : 0);
            out.write(handPI ? 1 : 0);
            out.write(playedPI ? 1 : 0);

            out.write(isHelionCorp ? (float) Math.log1p(player.getHeatIncome()) : 0f);
            out.write(isHelionCorp ? (float) Math.log1p(player.getHeat()) : 0f);
            out.write((handPI || playedPI) ? (float) Math.log1p(player.getHeatIncome()) : 0f);
            out.write((handPI || playedPI) ? (float) Math.log1p(player.getHeat()) : 0f);
        }

        {
            boolean hasCorp = playedCardActions.containsKey(CardAction.MINING_GUILD_CORPORATION);

            if (hasCorp) {
                out.write(1f);
                out.write(incomeStats.get(GainType.STEEL).nonZero);
            } else {
                out.write(0f);
                out.write(0f);
            }
        }

        {
            boolean hasCorp = playedCardActions.containsKey(CardAction.SATURN_SYSTEMS_CORPORATION);

            if (hasCorp) {
                out.write(1f);
                out.write(handTagCounts[Tag.JUPITER.ordinal()]);
            } else {
                out.write(0f);
                out.write(0f);
            }
        }

        {
            boolean playedTeractor = playedCardActions.containsKey(CardAction.TERACTOR_CORPORATION);//-3 mc per earth
            boolean playedConference = playedCardActions.containsKey(CardAction.INTERPLANETARY_CONFERENCE);//-3 mc and card per earth/jupiter
            boolean handConference = handCardActions.containsKey(CardAction.INTERPLANETARY_CONFERENCE);

            out.write(handConference ? 1.0f : 0.0f);
            out.write(playedConference ? 1.0f : 0.0f);

            float currentEarthDiscount = 0;
            float currentJupiterDiscount = 0;

            if (playedTeractor) {
                currentEarthDiscount += 3.0f;
            }
            if (playedConference) {
                currentEarthDiscount += 3.0f;
                currentJupiterDiscount += 3.0f;
            }

            out.write(currentEarthDiscount);
            out.write(currentJupiterDiscount);

            float earthCostInHand = getTagTotalCostInHand(hand, Tag.EARTH);
            float jupiterCostInHand = getTagTotalCostInHand(hand, Tag.JUPITER);

            out.write(handConference ? (float) Math.log1p(earthCostInHand) : 0f);
            out.write(handConference ? (float) Math.log1p(jupiterCostInHand) : 0f);

            out.write(currentEarthDiscount > 0 ? (float) Math.log1p(earthCostInHand) : 0f);
            out.write(currentJupiterDiscount > 0 ? (float) Math.log1p(jupiterCostInHand) : 0f);
        }

        {
            out.write(playedCardActions.containsKey(CardAction.CELESTIOR_CORPORATION) ? 1 : 0);
        }

        {
            out.write(playedCardActions.containsKey(CardAction.MODPRO_CORPORATION) ? 1 : 0);
        }

        {
            boolean corpOnTable = playedCardActions.containsKey(CardAction.DEVTECHS_CORPORATION);

            if (corpOnTable) {
                float totalCostInHand = 0f;
                int count = 0;
                for (Card c : hand) {
                    if (c.getColor() == CardColor.GREEN) {
                        totalCostInHand += c.getPrice();
                        count++;
                    }
                }

                out.write(1);
                out.write(count);
                out.write((float) Math.log1p(totalCostInHand));
            } else {
                out.write(0f);
                out.write(0f);
                out.write(0f);
            }
        }

        {
            boolean corpOnTable = playedCardActions.containsKey(CardAction.SULTIRA_CORPORATION);

            if (corpOnTable) {
                float totalCostInHand = 0f;
                for (Card c : hand) {
                    if (c.getTags().contains(Tag.ENERGY)) {
                        totalCostInHand += c.getPrice();
                    }
                }

                out.write(1);
                out.write(handTagCounts[Tag.ENERGY.ordinal()]);
                out.write((float) Math.log1p(totalCostInHand));
            } else {
                out.write(0f);
                out.write(0f);
                out.write(0f);
            }
        }

        {
            boolean corpOnTable = playedCardActions.containsKey(CardAction.LAUNCH_STAR_CORPORATION);

            if (corpOnTable) {
                float totalCostInHand = 0f;
                int count = 0;
                for (Card c : hand) {
                    if (c.getColor() == CardColor.BLUE) {
                        totalCostInHand += c.getPrice();
                        count++;
                    }
                }

                out.write(1);
                out.write(count);
                out.write((float) Math.log1p(totalCostInHand));
            } else {
                out.write(0f);
                out.write(0f);
                out.write(0f);
            }
        }

        {
            boolean corpOnTable = playedCardActions.containsKey(CardAction.CREDICOR_CORPORATION);

            if (corpOnTable) {
                float totalCostInHand = 0f;
                int count = 0;
                for (Card c : hand) {
                    if (c.getPrice() >= 20) {
                        totalCostInHand += c.getPrice();
                        count++;
                    }
                }

                out.write(1);
                out.write(count);
                out.write((float) Math.log1p(totalCostInHand));
            } else {
                out.write(0f);
                out.write(0f);
                out.write(0f);
            }
        }

        {
            boolean corpOnTable = playedCardActions.containsKey(CardAction.ARCLIGHT_CORPORATION);

            if (corpOnTable) {
                out.write(1);
                out.write(handTagCounts[Tag.ANIMAL.ordinal()] + handTagCounts[Tag.PLANT.ordinal()] + handTagCounts[Tag.MICROBE.ordinal()]);
            } else {
                out.write(0f);
                out.write(0f);
            }
        }

        {
            boolean advancedAlloysInHand = handCardClasses.contains(AdvancedAlloys.class);
            boolean advancedAlloysOnTable = playedCardClasses.contains(AdvancedAlloys.class);
            boolean phobologCorp = playedCardActions.containsKey(CardAction.PHOBOLOG_CORPORATION);

            float steelUnitValue = 2.0f + (advancedAlloysOnTable ? 1.0f : 0.0f);
            float titaniumUnitValue = 3.0f + (advancedAlloysOnTable ? 1.0f : 0.0f) + (phobologCorp ? 1.0f : 0.0f);

            float steelBuyingPower = player.getSteelIncome() * steelUnitValue;
            float titaniumBuyingPower = player.getTitaniumIncome() * titaniumUnitValue;

            out.write((float) Math.log1p(steelBuyingPower));
            out.write((float) Math.log1p(titaniumBuyingPower));

            int buildingTagsInHand = Math.max(0, handTagCounts[Tag.BUILDING.ordinal()] - (advancedAlloysInHand ? 1 : 0));
            int spaceTagsInHand = Math.max(0, handTagCounts[Tag.SPACE.ordinal()] - (advancedAlloysInHand ? 1 : 0));

            out.write((float) Math.log1p(steelBuyingPower * buildingTagsInHand));
            out.write((float) Math.log1p(titaniumBuyingPower * spaceTagsInHand));

            if (advancedAlloysInHand) {
                float potentialSteelValue = 3.0f;
                float potentialTitaniumValue = 4.0f + (phobologCorp ? 1.0f : 0.0f);

                float potentialSteelPower = player.getSteelIncome() * potentialSteelValue;
                float potentialTitaniumPower = player.getTitaniumIncome() * potentialTitaniumValue;

                out.write((float) Math.log1p(potentialSteelPower));
                out.write((float) Math.log1p(potentialTitaniumPower));
            } else {
                out.write(0f);
                out.write(0f);
            }
        }

        boolean hasHyperionCorp = playedCardActions.containsKey(CardAction.HYPERION_SYSTEMS_CORPORATION);
        boolean hasHandCommunityGardens = handCardClasses.contains(CommunityGardens.class);
        boolean hasPlayedCommunityGardens = playedCardClasses.contains(CommunityGardens.class);

        boolean handDroneAC = handCardClasses.contains(DroneAssistedConstruction.class);
        boolean playedDroneAC = playedCardClasses.contains(DroneAssistedConstruction.class);

        {
            out.write(hasHandCommunityGardens ? 1 : 0);//potential can get MC and plant in 3rd phase
            out.write(handDroneAC ? 1 : 0);//potential can get MC in 3rd phase
            out.write(hasPlayedCommunityGardens ? 1 : 0);//can get extra plant if chose 3rd

            out.write((hasHyperionCorp ? 1 : 0));//extra MC if chosen 3
            out.write((hasPlayedCommunityGardens ? 2 : 0) + (hasHyperionCorp ? 1 : 0) + (playedDroneAC ? 2 : 0));//get MC in 3
        }

        boolean exocorpOnTable = playedCardActions.containsKey(CardAction.EXOCORP_CORPORATION);
        boolean compostingOnTable = playedCardActions.containsKey(CardAction.COMPOSTING_FACTORY);
        boolean compostingInHand = handCardActions.containsKey(CardAction.COMPOSTING_FACTORY);

        int activeSellBonus = (exocorpOnTable ? 1 : 0) + (compostingOnTable ? 1 : 0);
        {
            out.write(activeSellBonus);

            if (activeSellBonus > 0) {
                out.write(player.getCardIncome());
                out.write(player.getHand().size());
            } else {
                out.write(0f);
                out.write(0f);
            }

            out.write(compostingInHand ? 1 : 0);

            boolean matterManufacturingOnTable = playedCardActions.containsKey(CardAction.MATTER_MANUFACTORING);
            boolean matterManufacturingInHand = handCardActions.containsKey(CardAction.MATTER_MANUFACTORING);

            boolean thinkTankOnTable = playedCardActions.containsKey(CardAction.THINKTANK);
            boolean thinkTankInHand = handCardActions.containsKey(CardAction.THINKTANK);

            int cardToMcMargin = 0;
            if (matterManufacturingOnTable) cardToMcMargin += (3 + activeSellBonus - 1);
            if (thinkTankOnTable) cardToMcMargin += (3 + activeSellBonus - 2);

            out.write(cardToMcMargin);

            int potentialMargin = 0;
            if (matterManufacturingInHand) potentialMargin += (3 + activeSellBonus - 1);
            if (thinkTankInHand) potentialMargin += (3 + activeSellBonus - 2);

            out.write(potentialMargin);
        }

        {
            boolean corpOnTable = playedCardActions.containsKey(CardAction.INTERPLANETARY_CINEMATICS); // -2 mc per event

            boolean card2InHand = handCardActions.containsKey(CardAction.MEDIA_GROUP); //-5 mc per event
            boolean card2OnTable = playedCardActions.containsKey(CardAction.MEDIA_GROUP); //-5 mc per event

            boolean card3InHand = handCardActions.containsKey(CardAction.OPTIMAL_AEROBRAKING); //+2 heat +2 plants
            boolean card3OnTable = playedCardActions.containsKey(CardAction.OPTIMAL_AEROBRAKING); //+2 heat +2 plants

            boolean card4InHand = handCardActions.containsKey(CardAction.RECYCLED_DETRITUS); //+2 cards per event
            boolean card4OnTable = playedCardActions.containsKey(CardAction.RECYCLED_DETRITUS);//+2 cards per event

            boolean card5InHand = handCardActions.containsKey(CardAction.IMPACT_ANALYSIS); //+1 card
            boolean card5OnTable = playedCardActions.containsKey(CardAction.IMPACT_ANALYSIS); //+1 card

            float totalEventMcDiscount = ((corpOnTable ? 2.0f : 0) + (card2OnTable ? 5.0f : 0.0f)) / 7.0f;
            float totalPotentialMcDiscount = card2InHand ? 1 : 0;
            out.write(totalEventMcDiscount);
            out.write(totalPotentialMcDiscount);

            out.write(card3InHand ? 1 : 0);//+2 heat + 2 plants
            out.write(card3OnTable ? 1 : 0);//+2 heat + 2 plants

            float totalCardDrawBonus = ((card4OnTable ? 2.0f : 0.0f) + (card5OnTable ? 1.0f : 0.0f)) / 3.0f;
            float potentialCardDrawBonus = ((card4InHand ? 2.0f : 0.0f) + (card5InHand ? 1.0f : 0.0f)) / 3.0f;
            out.write(totalCardDrawBonus);
            out.write(potentialCardDrawBonus);

            int eventTagsInHand = handTagCounts[Tag.EVENT.ordinal()];
            out.write((corpOnTable || card2OnTable || card3OnTable || card4OnTable || card5OnTable) ? eventTagsInHand : 0);
            out.write((card2InHand || card3InHand || card4InHand || card5InHand) ? eventTagsInHand : 0);
        }

        {
            boolean austellarCorp = playedCardActions.containsKey(CardAction.AUSTELLAR_CORPORATION);

            if (austellarCorp) {
                out.write(1f);

                Milestone austerllarMilestone = game.getMilestones().get(player.getAustellarMilestone());
                out.write(austerllarMilestone.isAchieved() ? 0 : handEncoderHelperService.countMilestoneProgressByHand(austerllarMilestone, hand, handTagCounts, gainTypeToCounts, incomeStats));
            } else {
                out.write(0f);
                out.write(0f);
            }
        }

        List<Milestone> milestones = game.getMilestones();
        for (Milestone m : milestones) {
            // 1. Мой прогресс (0.0 - 1.0)
            out.write((float) m.getValue(player, cardService) / m.getMaxValue());

            // 2. Прогресс оппонента
            out.write((float) m.getValue(opponent, cardService) / m.getMaxValue());

            boolean achieved = m.isAchieved();
            out.write(achieved ? 1.0f : 0.0f);
            out.write(achieved ? 0 : handEncoderHelperService.countMilestoneProgressByHand(m, hand, handTagCounts, gainTypeToCounts, incomeStats));
        }

        List<BaseAward> awards = game.getAwards();
        for (BaseAward award : awards) {
            float value = handEncoderHelperService.countAwardProgressByHand(award, hand, handTagCounts);
            out.write(value > 0 ? (float) Math.log(value) : 0);
        }
    }

    private Map<GainType, Stats> getTerraformingGains(List<Card> hand) {
        Stats forest = new Stats();
        Stats oxygen = new Stats();
        Stats temperature = new Stats();
        Stats ocean = new Stats();
        Stats terraformingRating = new Stats();

        for (Card c : hand) {
            List<Gain> incomes = c.getCardMetadata().getBonuses();
            if (incomes == null) continue;

            for (Gain g : incomes) {
                int v = g.getValue();
                switch (g.getType()) {
                    case FOREST -> {
                        forest.sum += v;
                    }
                    case OXYGEN -> {
                        oxygen.sum += v;
                    }
                    case TEMPERATURE -> {
                        temperature.sum += v;
                    }
                    case OCEAN -> {
                        ocean.sum += v;
                    }
                    case TERRAFORMING_RATING -> {
                        terraformingRating.sum += v;
                    }
                }
            }
        }

        Map<GainType, Stats> result = new EnumMap<>(GainType.class);
        result.put(GainType.FOREST, forest);
        result.put(GainType.OXYGEN, oxygen);
        result.put(GainType.TEMPERATURE, temperature);
        result.put(GainType.OCEAN, ocean);
        result.put(GainType.TERRAFORMING_RATING, terraformingRating);
        return result;
    }

    private Map<GainType, Stats> getRequiredIncomeStats(List<Card> hand) {
        Stats heat = new Stats();
        Stats plant = new Stats();
        Stats steel = new Stats();

        for (Card c : hand) {
            List<Gain> incomes = c.getCardMetadata().getIncomes();
            if (incomes == null) continue;

            for (Gain g : incomes) {
                int v = g.getValue();
                switch (g.getType()) {
                    case HEAT -> {
                        heat.sum += v;
                    }
                    case PLANT -> {
                        plant.sum += v;
                    }
                    case STEEL -> {
                        steel.nonZero++;
                    }
                }
            }
        }

        Map<GainType, Stats> result = new EnumMap<>(GainType.class);
        result.put(GainType.HEAT, heat);
        result.put(GainType.PLANT, plant);
        result.put(GainType.STEEL, steel);
        return result;
    }

    private float getTagTotalCostInHand(List<Card> hand, Tag earth) {
        float totalCostInHand = 0f;
        for (Card c : hand) {
            if (c.getTags().contains(earth)) {
                totalCostInHand += c.getPrice();
            }
        }
        return totalCostInHand;
    }

}
