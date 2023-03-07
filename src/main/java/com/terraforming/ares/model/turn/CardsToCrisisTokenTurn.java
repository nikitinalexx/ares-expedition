package com.terraforming.ares.model.turn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CardsToCrisisTokenTurn implements Turn {
    String playerUuid;
    List<Integer> cards;

    @Override
    public TurnType getType() {
        return TurnType.CARDS_TO_CRISIS_TOKEN;
    }

}
