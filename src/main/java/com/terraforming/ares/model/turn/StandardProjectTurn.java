package com.terraforming.ares.model.turn;

import com.terraforming.ares.model.StandardProjectType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class StandardProjectTurn implements Turn {
    String playerUuid;
    StandardProjectType projectType;
    Map<Integer, List<Integer>> inputParams;

    public TurnType getType() {
        return TurnType.STANDARD_PROJECT;
    }

}
