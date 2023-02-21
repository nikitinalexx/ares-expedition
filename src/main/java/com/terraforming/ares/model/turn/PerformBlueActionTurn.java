package com.terraforming.ares.model.turn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PerformBlueActionTurn implements Turn {
    private String playerUuid;
    private int projectId;
    private Map<Integer, List<Integer>> inputParams;

    @Override
    public TurnType getType() {
        return TurnType.PERFORM_BLUE_ACTION;
    }

}
