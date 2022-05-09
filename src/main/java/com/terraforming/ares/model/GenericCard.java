package com.terraforming.ares.model;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.income.Income;
import com.terraforming.ares.services.CardService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
public interface GenericCard {

    int getId();

    default TurnResponse buildProject(MarsContext marsContext) {
        return null;
    }

    //TODO find a better way to transfer card description and parameters
    String description();

    default List<Tag> getTags() {
        return Collections.emptyList();
    }

    default List<Income> getIncomes() {
        return List.of();
    }

    Expansion getExpansion();

    int getPrice();

    default void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard project, Map<Integer, List<Integer>> inputParams) {
    }

    default void onOceanFlippedEffect(Player player) {
    }

    default void onTemperatureChangedEffect(Player player) {
    }

    default void onOxygenChangedEffect(Player player) {
    }

    default void onForestBuiltEffect(Player player) {
    }

}
