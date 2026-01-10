package com.terraforming.ares.services.ai.advanced.features.hand;

import com.terraforming.ares.dto.DraftCardsDto;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;
import java.util.Map;

public class HandPlayerFeature implements FeatureBlock {
    @Override
    public int size() {
        return 54;
    }

    public static final List<String> FEATURE_NAMES = List.of(
            // hand basics
            "hand_size",
            "inv_hand_size_plus_one",
            "hand_size_over_10",

            // hand colors
            "hand_blue_count",
            "hand_red_count",
            "hand_green_count",

            // active cards
            "played_active_cards_count",
            "hand_active_cards_count",

            // hand cost buckets (normalized)
            "hand_cost_bucket_0_5",
            "hand_cost_bucket_6_12",
            "hand_cost_bucket_13_20",
            "hand_cost_bucket_21_30",
            "hand_cost_bucket_31_plus",

            // plants
            "has_plants_income",
            "log_plants",
            "log_plants_income",

            // heat
            "log_heat",
            "log_heat_income",

            // cards income
            "has_card_income",
            "log_card_income",

            // player phase cards upgrades
            "player_phase1_upgrade_lvl1",
            "player_phase1_upgrade_lvl2",
            "player_phase2_upgrade_lvl1",
            "player_phase2_upgrade_lvl2",
            "player_phase3_upgrade_lvl1",
            "player_phase3_upgrade_lvl2",
            "player_phase4_upgrade_lvl1",
            "player_phase4_upgrade_lvl2",
            "player_phase5_upgrade_lvl1",
            "player_phase5_upgrade_lvl2",

            // opponent phase cards upgrades
            "opponent_phase1_upgrade_lvl1",
            "opponent_phase1_upgrade_lvl2",
            "opponent_phase2_upgrade_lvl1",
            "opponent_phase2_upgrade_lvl2",
            "opponent_phase3_upgrade_lvl1",
            "opponent_phase3_upgrade_lvl2",
            "opponent_phase4_upgrade_lvl1",
            "opponent_phase4_upgrade_lvl2",
            "opponent_phase5_upgrade_lvl1",
            "opponent_phase5_upgrade_lvl2",

            // phase upgrades availability
            "phase1_upgrades_gt_0",
            "phase1_upgrades_gt_1",
            "phase2_upgrades_gt_0",
            "phase2_upgrades_gt_1",
            "phase3_upgrades_gt_0",
            "phase3_upgrades_gt_1",
            "phase4_upgrades_gt_0",
            "phase4_upgrades_gt_1",
            "global_upgrades_gt_0",
            "global_upgrades_gt_1",

            // draft & take cards
            "cards_to_see",
            "cards_to_take",

            // extra draft/take potential from hand
            "extra_cards_to_draft_potential",
            "extra_cards_to_take_potential"
    );

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        Player player = ctx.getPlayer();
        Player opponent = ctx.getOpponent();
        List<Card> hand = ctx.getHand();
        List<Card> playedCards = ctx.getPlayedCards();
        Map<CardAction, Long> handCardActions = ctx.getHandCardActions();

        DraftCardsService draftCardsService = ctx.getDraftCardsService();

        out.write(hand.size());
        out.write(1f / (hand.size() + 1));
        out.write(hand.size() > 10 ? 1 : 0);

        long blueCount = hand.stream().filter(card -> card.getColor() == CardColor.BLUE).count();
        long redCount = hand.stream().filter(card -> card.getColor() == CardColor.RED).count();
        long greenCount = hand.stream().filter(card -> card.getColor() == CardColor.GREEN).count();

        out.write(blueCount);
        out.write(redCount);
        out.write(greenCount);

        out.write(playedCards.stream().filter(Card::isActiveCard).count());
        out.write(hand.stream().filter(Card::isActiveCard).count());

        int[] costBuckets = new int[5];

        for (Card card : hand) {
            if (card.getPrice() <= 5) costBuckets[0]++;
            else if (card.getPrice() <= 12) costBuckets[1]++;
            else if (card.getPrice() <= 20) costBuckets[2]++;
            else if (card.getPrice() <= 30) costBuckets[3]++;
            else costBuckets[4]++;
        }

        for (int i = 0; i < 5; i++) {
            out.write(Math.min(1.0f, costBuckets[i]));
        }

        //real incomes, lets artificial jungle to know if it has plants
        out.write(player.getPlantsIncome() > 0 ? 1 : 0);
        out.write((float) Math.log1p(player.getPlants()));
        out.write((float) Math.log1p(player.getPlantsIncome()));

        //real incomes, lets development center to know if it has plants
        out.write((float) Math.log1p(player.getHeat()));
        out.write((float) Math.log1p(player.getHeatIncome()));

        out.write(player.getCardIncome() > 0 ? 1 : 0);
        out.write((float) Math.log1p(player.getCardIncome()));

        for (int i = 0; i < 5; i++) {
            out.write(player.getPhaseCards().get(i) == 1 ? 1 : 0);//first upgrade
            out.write(player.getPhaseCards().get(i) == 2 ? 1 : 0);//second upgrade
        }
        for (int i = 0; i < 5; i++) {
            out.write(opponent.getPhaseCards().get(i) == 1 ? 1 : 0);//first upgrade
            out.write(opponent.getPhaseCards().get(i) == 2 ? 1 : 0);//second upgrade
        }

        int globalUpgrades = (int) (handCardActions.getOrDefault(CardAction.UPDATE_PHASE_CARD, 0L) + 2 * handCardActions.getOrDefault(CardAction.UPDATE_PHASE_CARD_TWICE, 0L) + handCardActions.getOrDefault(CardAction.CRYOGENIC_SHIPMENT, 0L));
        int phase1Upgrades = globalUpgrades + handCardActions.getOrDefault(CardAction.UPDATE_PHASE_1_CARD, 0L).intValue();
        int phase2Upgrades = globalUpgrades + handCardActions.getOrDefault(CardAction.UPDATE_PHASE_2_CARD, 0L).intValue();
        int phase3Upgrades = globalUpgrades + handCardActions.getOrDefault(CardAction.COMMUNICATIONS_STREAMLINING, 0L).intValue();
        int phase4Upgrades = globalUpgrades + handCardActions.getOrDefault(CardAction.UPDATE_PHASE_4_CARD, 0L).intValue();

        out.write(phase1Upgrades > 0 ? 1 : 0);
        out.write(phase1Upgrades > 1 ? 1 : 0);
        out.write(phase2Upgrades > 0 ? 1 : 0);
        out.write(phase2Upgrades > 1 ? 1 : 0);
        out.write(phase3Upgrades > 0 ? 1 : 0);
        out.write(phase3Upgrades > 1 ? 1 : 0);
        out.write(phase4Upgrades > 0 ? 1 : 0);
        out.write(phase4Upgrades > 1 ? 1 : 0);
        out.write(globalUpgrades > 0 ? 1 : 0);
        out.write(globalUpgrades > 1 ? 1 : 0);

        {
            int extraCardsToDraftPotential = 0;
            int extraCardsToTakePotential = 0;

            if (handCardActions.containsKey(CardAction.INTERPLANETARY_RELATIONS)) {
                extraCardsToDraftPotential++;
                extraCardsToTakePotential++;
            }

            if (handCardActions.containsKey(CardAction.INTERNS)) {
                extraCardsToDraftPotential += 2;
            }

            if (handCardActions.containsKey(CardAction.UNITED_PLANETARY)) {
                extraCardsToDraftPotential++;
                extraCardsToTakePotential++;
            }

            if (handCardActions.containsKey(CardAction.BACTERIAL_AGGREGATES)) {
                extraCardsToDraftPotential++;
            }

            if (handCardActions.containsKey(CardAction.EXTENDED_RESOURCES)) {
                extraCardsToTakePotential++;
            }

            DraftCardsDto draftCardsDto = draftCardsService.countCardsToTakeAndDraft(player);

            out.write(draftCardsDto.getCardsToSee());
            out.write(draftCardsDto.getCardsToTake());

            out.write(extraCardsToDraftPotential);
            out.write(extraCardsToTakePotential);
        }
    }
}
