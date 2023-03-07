package com.terraforming.ares.model;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.model.parameters.OceanRequirement;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.services.CardService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
public interface Card {

    int getId();

    boolean isActiveCard();

    default TurnResponse buildProject(MarsContext marsContext) {
        return null;
    }


    default List<Tag> getTags() {
        return Collections.emptyList();
    }

    CardMetadata getCardMetadata();

    Expansion getExpansion();

    int getPrice();

    CardColor getColor();

    default void payAgain(MarsGame game, CardService cardService, Player player) {
    }

    default int getWinningPoints() {
        return 0;
    }

    default Set<SpecialEffect> getSpecialEffects() {
        return Collections.emptySet();
    }

    default List<Tag> getTagRequirements() {
        return Collections.emptyList();
    }

    default List<ParameterColor> getTemperatureRequirement() {
        return Collections.emptyList();
    }

    default List<ParameterColor> getOxygenRequirement() {
        return Collections.emptyList();
    }

    default OceanRequirement getOceanRequirement() {
        return null;
    }

    default void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
    }

    default void onOceanFlippedEffect(MarsContext context) {
    }

    default void onTemperatureChangedEffect(MarsContext context) {
    }

    default void onOxygenChangedEffect(MarsContext context) {
    }

    default void onForestBuiltEffect(MarsContext context) {
    }

    default CardCollectableResource getCollectableResource() {
        return CardCollectableResource.NONE;
    }

    boolean isCorporation();

    default boolean onBuiltEffectApplicableToItself() {
        return false;
    }

    default boolean onBuiltEffectApplicableToOther() {
        return false;
    }

    default int heatSpendOnBuild() {
        return 0;
    }

    default void revertPlayedTags(CardService cardService, Card card, Player player) {
    }

    default void revertCardIncome(MarsContext context) {
        final Player player = context.getPlayer();
        getCardMetadata().getIncomes().forEach(income -> {
            switch (income.getType()) {
                case MC:
                    player.setMcIncome(player.getMcIncome() - income.getValue());
                    break;
                case HEAT:
                    player.setHeatIncome(player.getHeatIncome() - income.getValue());
                    break;
                case PLANT:
                    player.setPlantsIncome(player.getPlantsIncome() - income.getValue());
                    break;
                case STEEL:
                    player.setSteelIncome(player.getSteelIncome() - income.getValue());
                    break;
                case TITANIUM:
                    player.setTitaniumIncome(player.getTitaniumIncome() - income.getValue());
                    break;
                case CARD:
                    player.setCardIncome(player.getCardIncome() - income.getValue());
                    break;
                default:
                    break;
            }
        });
    }

    default void onMilestoneGained(MarsContext context, Player player, Milestone milestone) {

    }

    default boolean isBlankCard() {
        return false;
    }

    default boolean isSupportedByExpansionSet(Set<Expansion> expansions) {
        return true;
    }


}
