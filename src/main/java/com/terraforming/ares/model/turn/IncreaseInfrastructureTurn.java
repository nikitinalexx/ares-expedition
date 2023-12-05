package com.terraforming.ares.model.turn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 04.12.2023
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class IncreaseInfrastructureTurn implements Turn {
    String playerUuid;
    Map<Integer, List<Integer>> inputParams;

    @Override
    public TurnType getType() {
        return TurnType.INCREASE_INFRASTRUCTURE;
    }

}
