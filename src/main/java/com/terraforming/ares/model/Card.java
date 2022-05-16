package com.terraforming.ares.model;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
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

    default void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, Card project, Map<Integer, List<Integer>> inputParams) {
    }

    default void onOceanFlippedEffect(Player player) {
    }

    default void onTemperatureChangedEffect(Player player) {
    }

    default void onOxygenChangedEffect(Player player) {
    }

    default void onForestBuiltEffect(Player player) {
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

}
