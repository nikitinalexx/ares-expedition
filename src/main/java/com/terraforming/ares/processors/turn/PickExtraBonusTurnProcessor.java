package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildDto;
import com.terraforming.ares.model.BuildType;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.PickExtraBonusSecondPhase;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@Service
@RequiredArgsConstructor
public class PickExtraBonusTurnProcessor implements TurnProcessor<PickExtraBonusSecondPhase> {
    private final CardService cardService;

    @Override
    public TurnType getType() {
        return TurnType.PICK_EXTRA_BONUS_SECOND_PHASE;
    }

    @Override
    public TurnResponse processTurn(PickExtraBonusSecondPhase turn, MarsGame game) {
        Player player = game.getPlayerByUuid(turn.getPlayerUuid());

        final BuildDto getCardDto = player.getBuilds().stream().filter(build -> build.getType() == BuildType.BLUE_RED_OR_CARD).findAny().orElse(null);

        if (getCardDto != null) {
            List<Integer> cards = cardService.dealCards(game, 1);

            for (Integer card : cards) {
                player.getHand().addCard(card);
            }
            player.getBuilds().remove(getCardDto);
        } else {
            final BuildDto getMcDto = player.getBuilds().stream().filter(build -> build.getType() == BuildType.BLUE_RED_OR_MC).findAny().orElse(null);

            if (getMcDto != null) {
                player.setMc(player.getMc() + 6);

                player.getBuilds().remove(getMcDto);
            }
        }

        return null;
    }
}
