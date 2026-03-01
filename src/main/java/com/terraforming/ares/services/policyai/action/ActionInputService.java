package com.terraforming.ares.services.policyai.action;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.StandardProjectType;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.policyai.encoder.IncomeCategory;

import java.util.List;

/**
 * 0..267   -> BUILD_CARD
 * 268..535 -> SELL_CARD
 * 536..543 -> ANIMAL_TARGET
 * 544..552 -> MICROBE_TARGET
 * 553..600 -> RED_CARD_TARGET
 * 844       -> PASS
 */
public class ActionInputService {

    public static long[] createMask() {
        return new long[ActionRegistry.TOTAL_ACTIONS];
    }

    // ---------- BUILD ----------
    public static void enableBuild(
            long[] mask,
            List<Class<?>> cards
    ) {
        for (Class<?> card : cards) {
            Integer idx =
                    AiConstants.ALL_CARDS_INDEX_BY_CLASS.get(card);

            if (idx != null) {
                mask[ActionRegistry.BUILD_START + idx] = 1;
            }
        }
    }

    public static int buildProjectActionId(Card card) {
        return ActionRegistry.BUILD_START + AiConstants.ALL_CARDS_INDEX_BY_CLASS.get(card.getClass());
    }

    // ---------- SELL OR DISCARD ----------
    public static void enableSellDiscard(
            long[] mask,
            List<Class<?>> hand
    ) {
        for (Class<?> card : hand) {
            Integer idx =
                    AiConstants.ALL_CARDS_INDEX_BY_CLASS.get(card);

            if (idx != null) {
                mask[ActionRegistry.SELL_DISCARD_START + idx] = 1;
            }
        }
    }

    public static int sellDiscardCardActionIndex(Card card) {
        return ActionRegistry.SELL_DISCARD_START + AiConstants.ALL_CARDS_INDEX_BY_CLASS.get(card.getClass());
    }

    public static int chooseCorporationActionIndex(Card card) {
        return ActionRegistry.TARGET_CORPORATIONS_START + AiConstants.CORP_INDEX.get(card.getCardMetadata().getCardAction());
    }

    // ---------- TARGET ----------
    public static void enableAnimalTarget(
            long[] mask,
            Class<?> animal
    ) {
        mask[
                ActionRegistry.TARGET_ANIMAL_START + ActionRegistry.ANIMAL_TARGET_TO_OFFSET.get(animal)
                ] = 1;
    }

    public static int getAnimalTargetActionIndex(Card card) {
        return ActionRegistry.TARGET_ANIMAL_START + ActionRegistry.ANIMAL_TARGET_TO_OFFSET.get(card.getClass());
    }

    public static void enableMicrobeTarget(
            long[] mask,
            Class<?> microbe
    ) {
        mask[
                ActionRegistry.TARGET_MICROBE_START + ActionRegistry.MICROBE_TARGET_TO_OFFSET.get(microbe)
                ] = 1;
    }

    public static int getMicrobeTargetActionIndex(Card card) {
        return ActionRegistry.TARGET_MICROBE_START + ActionRegistry.MICROBE_TARGET_TO_OFFSET.get(card.getClass());
    }

    public static void enabledRedCardTarget(
            long[] mask,
            Class<?> redCard) {
        mask[ActionRegistry.TARGET_RED_CARD_START + ActionRegistry.SYNTHETIC_CATSTROPHE_TARGET.get(redCard)] = 1;
    }

    public static int getRedCardTargetActionIndex(Card redCard) {
        return ActionRegistry.TARGET_RED_CARD_START + ActionRegistry.SYNTHETIC_CATSTROPHE_TARGET.get(redCard.getClass());
    }


    public static int passActionId() {
        return ActionRegistry.SYSTEM_CONFIRM_PASS;
    }

    public static int unmiActionId() {
        return ActionRegistry.SYSTEM_UNMI_RT_FOR_6_MC;
    }


    public static int takePlantTargetAction() {
        return ActionRegistry.TARGET_TAKE_PLANT_START;
    }

    public static int choosePhaseUpgradeIndex(int upgradeId) {
        return ActionRegistry.SMALL_CHOICE_PHASE_UPGRADE_START + upgradeId;
    }

    public static int austellarMilestoneChoice(int milestoneIndex) {
        return ActionRegistry.SMALL_CHOICE_PHASE_UPGRADE_START + milestoneIndex;//we reuse phase upgrade action space
    }

    public static int biomedicalImportsUpgradeOxygen() {
        return ActionRegistry.SMALL_BIOMEDICAL_IMPORTS_OXYGEN_START;
    }

    public static int biomedicalImportsPhaseUpgrade() {
        return ActionRegistry.SMALL_BIOMEDICAL_IMPORTS_PHASE_START;
    }

    public static int decomposersPickMicrobe() {
        return ActionRegistry.SMALL_DECOMPOSERS_PICK_MICROBE;
    }

    public static int decomposersPickCard() {
        return ActionRegistry.SMALL_DECOMPOSERS_PICK_CARD;
    }

    public static int chooseTag(int index) {
        return ActionRegistry.SMALL_CHOICE_TAG_START + index;
    }

    public static int specialGreenCardThatPays(int cardIndex) {
        return ActionRegistry.TARGET_SPEC_GREEN_CARD_INCOME_START + cardIndex;
    }

    public static int specialPhasePaymentByIncomeType(IncomeCategory category) {
        switch (category) {
            case MC:
                return ActionRegistry.TARGET_MAX_MC_INCOME;
            case HEAT:
                return ActionRegistry.TARGET_MAX_HEAT_INCOME;
            case PLANT:
                return ActionRegistry.TARGET_MAX_PLANTS_INCOME;
            case CARD:
                return ActionRegistry.TARGET_MAX_CARDS_INCOME;
            default:
                throw new IllegalArgumentException("Invalid income category");
        }
    }

    public static int enterSellingModeAction() {
        return ActionRegistry.SYSTEM_ENTER_SELL_MODE;
    }

    public static int enterHelionModeAction() {
        return ActionRegistry.SYSTEM_ENTER_HELION_MODE;
    }

    public static int exchange1Heat() {
        return ActionRegistry.SYSTEM_HEAT_EXCHANGE;
    }

    public static int getBlueAction(Card card) {
        if (AiConstants.NO_PAYMENT_BLUE_ACTIONS.contains(card.getClass())) {
            return ActionRegistry.TARGET_NO_PAY_BLUE_ACTION_START + AiConstants.NO_PAY_BLUE_ACTION_INDEX_BY_CLASS.get(card.getClass());
        } else {
            return ActionRegistry.TARGET_WITH_PAY_BLUE_ACTION_START + AiConstants.WITH_PAY_BLUE_ACTION_INDEX_BY_CLASS.get(card.getClass());
        }
    }

    public static int actionTakeMicrobe() {
        return ActionRegistry.SMALL_ACTION_TAKE_MICROBE_START;
    }

    public static int actionUseMicrobe() {
        return ActionRegistry.SMALL_ACTION_USE_MICROBE_START;
    }

    public static int action1To4Count(int count) {
        return ActionRegistry.SMALL_ACTION_COUNT_1_4_START + count;
    }

    public static int getScienceResourceTargetActionIndex(Card card) {
        return ActionRegistry.TARGET_SCIENCE_START + ActionRegistry.SCIENCE_TARGET_TO_OFFSET.get(card.getClass());
    }

    public static int takeBonusAction() {
        return ActionRegistry.SYSTEM_TAKE_BONUS;
    }

    public static int payForTheBuild(int type) {
        return ActionRegistry.SMALL_PAY_FOR_THE_BUILD_START + type;
    }

    public static int standardProject(StandardProjectType type) {
        if (type == StandardProjectType.INFRASTRUCTURE) {
            throw new IllegalStateException("Infrastructure not available for policy");
        }
        return ActionRegistry.SMALL_ACTION_STANDARD_PROJECT_START + type.ordinal();
    }

    public static int convertHeatToTemperature() {
        return ActionRegistry.SMALL_ACTION_HEAT_PLANTS_CONVERSION_START;
    }

    public static int convertPlantsToTemperature() {
        return ActionRegistry.SMALL_ACTION_HEAT_PLANTS_CONVERSION_START + 1;
    }

}
