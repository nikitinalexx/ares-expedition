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
public class CrysisCardPersistentChoiceTurn implements Turn {
    private String playerUuid;
    private Integer card;
    private Map<Integer, List<Integer>> input;
    private boolean expectedAsNextTurn;

    @Override
    public boolean expectedAsNextTurn() {
        return expectedAsNextTurn;
    }

    @Override
    public TurnType getType() {
        return TurnType.RESOLVE_PERSISTENT_WITH_CHOICE;
    }

}
