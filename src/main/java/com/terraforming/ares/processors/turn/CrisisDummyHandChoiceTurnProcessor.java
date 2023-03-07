package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.CrisisDummyHandChoiceTurn;
import com.terraforming.ares.model.turn.TurnType;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class CrisisDummyHandChoiceTurnProcessor implements TurnProcessor<CrisisDummyHandChoiceTurn> {


    @Override
    public TurnResponse processTurn(CrisisDummyHandChoiceTurn turn, MarsGame game) {
        game.getCrysisData().setChosenDummyPhases(List.of(
                turn.getChoiceOptions().get(0).charAt(0) - 48,
                turn.getChoiceOptions().get(1).charAt(0) - 48
        ));

        return null;
    }

    @Override
    public TurnType getType() {
        return TurnType.CRISIS_CHOOSE_DUMMY_HAND;
    }
}
