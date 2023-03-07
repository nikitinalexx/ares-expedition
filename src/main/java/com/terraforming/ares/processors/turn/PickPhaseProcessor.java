package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.PhaseChoiceTurn;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.SpecialEffectsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class PickPhaseProcessor implements TurnProcessor<PhaseChoiceTurn> {
    private final SpecialEffectsService specialEffectsService;

    @Override
    public TurnResponse processTurn(PhaseChoiceTurn turn, MarsGame game) {
        Player player = game.getPlayerUuidToPlayer().get(turn.getPlayerUuid());

        player.clearRoundResults();
        player.setPreviousChosenPhase(turn.getPhaseId());
        player.setChosenPhase(turn.getPhaseId());

        if (game.isCrysis()) {
            game.getCrysisData().getForbiddenPhases().remove(player.getUuid());
        }

        if (turn.getPhaseId() == 3) {
            player.setBlueActionExtraActivationsLeft(1);

            if (player.hasPhaseUpgrade(Constants.PHASE_3_UPGRADE_DOUBLE_REPEAT)) {
                player.setBlueActionExtraActivationsLeft(2);
            }
        }

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.COMMUNICATIONS_STREAMLINING) && player.isPhaseUpgraded(turn.getPhaseId())) {
            player.setMc(player.getMc() + 1);
        }

        if (specialEffectsService.ownsSpecialEffect(player, SpecialEffect.NEBU_LABS_CORPORATION) && player.isPhaseUpgraded(turn.getPhaseId())) {
            player.setMc(player.getMc() + 2);
        }

        return null;
    }

    @Override
    public TurnType getType() {
        return TurnType.PICK_PHASE;
    }
}
