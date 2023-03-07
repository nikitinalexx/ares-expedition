package com.terraforming.ares.model.turn;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@AllArgsConstructor
@Getter
@NoArgsConstructor
public class CrisisDummyHandChoiceTurn implements Turn {
    private String playerUuid;
    private List<String> choiceOptions;

    @Override
    public TurnType getType() {
        return TurnType.CRISIS_CHOOSE_DUMMY_HAND;
    }

}
