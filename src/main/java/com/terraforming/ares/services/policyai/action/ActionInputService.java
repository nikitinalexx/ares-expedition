package com.terraforming.ares.services.policyai.action;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.StandardProjectType;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.policyai.encoder.IncomeCategory;

/**
 * Фабрика действий политики. Все методы возвращают кэшированный {@link HeadAction}.
 *
 * <p>Типобезопасность гарантируется структурой: каждый метод делегирует
 * строго в один класс головы. Перепутать головы невозможно.
 *
 * <p>Аллокаций нет — все объекты созданы один раз при загрузке классов.
 *
 * <pre>
 *   HeadAction action = ActionInputService.buildProjectAction(card);
 *
 *   // текущий формат записи:
 *   record.chosenAction = action.globalIndex();
 *
 *   // будущий формат:
 *   record.chosenHead       = (byte)  action.head().ordinal();
 *   record.chosenLocalIndex = (short) action.localIndex();
 * </pre>
 */
public class ActionInputService {

    // =========================================================================
    // BUILD
    // =========================================================================

    public static HeadAction buildProjectAction(Card card) {
        return BuildAction.forCardIndex(AiConstants.ALL_CARDS_INDEX_BY_CLASS.get(card.getClass()));
    }

    // =========================================================================
    // SELL_DISCARD
    // =========================================================================

    public static HeadAction sellDiscardCardAction(Card card) {
        return SellDiscardAction.forCardIndex(AiConstants.ALL_CARDS_INDEX_BY_CLASS.get(card.getClass()));
    }

    // =========================================================================
    // CORPORATION
    // =========================================================================

    public static HeadAction chooseCorporationAction(Card card) {
        return CorporationAction.forCorpIndex(AiConstants.CORP_INDEX.get(card.getCardMetadata().getCardAction()));
    }

    // =========================================================================
    // TARGET
    // =========================================================================

    public static HeadAction getAnimalTargetAction(Card card) {
        return TargetAction.forAnimalOffset(ActionRegistry.ANIMAL_TARGET_TO_OFFSET.get(card.getClass()));
    }

    public static HeadAction getMicrobeTargetAction(Card card) {
        return TargetAction.forMicrobeOffset(ActionRegistry.MICROBE_TARGET_TO_OFFSET.get(card.getClass()));
    }

    public static HeadAction getScienceResourceTargetAction(Card card) {
        int offset = ActionRegistry.SCIENCE_TARGET_TO_OFFSET.get(card.getClass());
        return offset == 0 ? TargetAction.SCIENCE_PHYSICS_COMPLEX : TargetAction.SCIENCE_FIBROUS_COMPOSITE;
    }

    public static HeadAction getRedCardTargetAction(Card redCard) {
        return TargetAction.forRedCardIndex(ActionRegistry.SYNTHETIC_CATSTROPHE_TARGET.get(redCard.getClass()));
    }

    public static HeadAction takePlantTargetAction() {
        return TargetAction.TAKE_PLANT;
    }

    public static HeadAction specialGreenCardThatPays(int cardIndex) {
        return TargetAction.forSpecGreenCardIndex(cardIndex);
    }

    public static HeadAction specialPhasePaymentByIncomeType(IncomeCategory category) {
        return switch (category) {
            case MC    -> TargetAction.MAX_MC_INCOME;
            case HEAT  -> TargetAction.MAX_HEAT_INCOME;
            case PLANT -> TargetAction.MAX_PLANTS_INCOME;
            case CARD  -> TargetAction.MAX_CARDS_INCOME;
            default    -> throw new IllegalArgumentException("Unsupported IncomeCategory: " + category);
        };
    }

    // =========================================================================
    // BLUE
    // =========================================================================

    public static HeadAction getBlueAction(Card card) {
        if (AiConstants.NO_PAYMENT_BLUE_ACTIONS.contains(card.getClass())) {
            return BlueAction.forNoPayIndex(AiConstants.NO_PAY_BLUE_ACTION_INDEX_BY_CLASS.get(card.getClass()));
        } else {
            return BlueAction.forWithPayIndex(AiConstants.WITH_PAY_BLUE_ACTION_INDEX_BY_CLASS.get(card.getClass()));
        }
    }

    // =========================================================================
    // SMALL
    // =========================================================================

    public static HeadAction choosePhaseUpgradeAction(int upgradeId) {
        return SmallAction.forPhaseUpgrade(upgradeId);
    }

    public static HeadAction choosePhaseTurn(int phaseId) {
        if (phaseId < 1 || phaseId > 5) {
            throw new  IllegalArgumentException("Invalid phase id: " + phaseId);
        }
        return SmallAction.forPhaseUpgrade(phaseId);
    }

    public static HeadAction austellarMilestoneChoice(int milestoneIndex) {
        if (milestoneIndex == -1) throw new IllegalArgumentException("Invalid milestone index");
        return SmallAction.forAustellarMilestone(milestoneIndex);
    }

    public static HeadAction biomedicalImportsUpgradeOxygen() {
        return SmallAction.BIOMEDICAL_IMPORTS_OXYGEN;
    }

    public static HeadAction biomedicalImportsPhaseUpgrade() {
        return SmallAction.BIOMEDICAL_IMPORTS_PHASE;
    }

    public static HeadAction decomposersPickMicrobe() {
        return SmallAction.DECOMPOSERS_PICK_MICROBE;
    }

    public static HeadAction decomposersPickCard() {
        return SmallAction.DECOMPOSERS_PICK_CARD;
    }

    public static HeadAction chooseTag(int index) {
        return SmallAction.forTag(index);
    }

    public static HeadAction actionTakeMicrobe() {
        return SmallAction.ACTION_TAKE_MICROBE;
    }

    public static HeadAction actionUseMicrobe() {
        return SmallAction.ACTION_USE_MICROBE;
    }

    public static HeadAction action1To4Count(int count) {
        return SmallAction.forCount(count - 1);
    }

    public static HeadAction payForTheBuild(int type) {
        return SmallAction.forPayForBuild(type);
    }

    public static HeadAction standardProject(StandardProjectType type) {
        return SmallAction.forStandardProject(type);
    }

    public static HeadAction convertHeatToTemperature() {
        return SmallAction.CONVERT_HEAT_TO_TEMPERATURE;
    }

    public static HeadAction convertPlantsToForest() {
        return SmallAction.CONVERT_PLANTS_TO_FOREST;
    }

    // =========================================================================
    // SYSTEM
    // =========================================================================

    public static HeadAction passAction()            { return SystemAction.PASS; }
    public static HeadAction unmiAction()            { return SystemAction.UNMI_RT_FOR_6_MC; }
    public static HeadAction enterSellingModeAction(){ return SystemAction.ENTER_SELL_MODE; }
    public static HeadAction enterHelionModeAction() { return SystemAction.ENTER_HELION_MODE; }
    public static HeadAction exchange1Heat()         { return SystemAction.HEAT_EXCHANGE; }
    public static HeadAction takeBonusAction()       { return SystemAction.TAKE_BONUS; }
}
