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
public class CrisisVpToTokenTurn implements Turn {
    private String playerUuid;
    private List<Integer> cards;

    @Override
    public TurnType getType() {
        return TurnType.CRISIS_VP_TO_TOKEN;
    }

}
