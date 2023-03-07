package com.terraforming.ares.model;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 02.03.2023
 */
public interface CrysisCard {

    int getId();

    CardTier tier();

    int playerCount();

    int initialTokens();

    String name();

    CrysisCardAction cardAction();

    void onPersistentEffect(MarsContext marsContext, Map<Integer, List<Integer>> input);

    boolean persistentEffectRequiresChoice();

    String immediateEffect();

    default List<String> immediateOptions() {
        return List.of();
    }

    default List<String> persistentOptions() {
        return List.of();
    }

    void onImmediateEffect(MarsContext marsContext, Map<Integer, List<Integer>> input);

    boolean immediateEffectRequiresChoice();

    void postProjectBuiltEffect(MarsContext marsContext, Card card, Map<Integer, List<Integer>> inputParams);

    default void onOxygenChangedEffect(MarsContext marsContext) {
    }

    default void onTemperatureChangedEffect(MarsContext marsContext) {
    }

    default void onOceanFlippedEffect(MarsContext marsContext) {
    }

    default CrysisActiveCardAction getActiveCardAction() {
        return null;
    }

    default SpecialCrysisGoal getSpecialCrysisGoal() {
        return null;
    }

    default boolean endGameCard() {
        return false;
    }

}
