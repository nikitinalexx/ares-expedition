package com.terraforming.ares.model.turn;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@AllArgsConstructor
@Getter
@NoArgsConstructor
public class CorporationChoiceTurn implements Turn {
    private String playerUuid;
    private int corporationCardId;
    private Map<Integer, List<Integer>> inputParams;

    @Override
    public TurnType getType() {
        return TurnType.PICK_CORPORATION;
    }

}
