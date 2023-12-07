package com.terraforming.ares.processors.turn;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import com.terraforming.ares.model.turn.Turn;
import com.terraforming.ares.model.turn.TurnType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
@RequiredArgsConstructor
public class DiscardCardsProcessor implements TurnProcessor<DiscardCardsTurn> {
    private final Random random = new Random();

    @Override
    public TurnResponse processTurn(DiscardCardsTurn turn, MarsGame game) {
        List<Integer> leftCards = turn.getCards();

        Player player = game.getPlayerUuidToPlayer().get(turn.getPlayerUuid());

        player.getHand().removeCards(leftCards);

        List<String> alreadyDraftedPlayers = turn.getAlreadyDrafted();
        if (!CollectionUtils.isEmpty(alreadyDraftedPlayers)) {
            List<String> playersWhoDidntDraft = new ArrayList<>(game.getPlayerUuidToPlayer().keySet());
            playersWhoDidntDraft.removeAll(alreadyDraftedPlayers);

            if (playersWhoDidntDraft.size() > 0) {
                String nextPlayerUuid = playersWhoDidntDraft.get(random.nextInt(playersWhoDidntDraft.size()));

                Player nextPlayer = game.getPlayerByUuid(nextPlayerUuid);
                nextPlayer.getHand().addCards(leftCards);

                List<String> allWhoDrafted = new ArrayList<>(turn.getAlreadyDrafted());
                allWhoDrafted.add(nextPlayerUuid);

                nextPlayer.addNextTurn(
                        new DiscardCardsTurn(
                                nextPlayer.getUuid(),
                                new ArrayList<>(leftCards),
                                leftCards.size() - 1,
                                true,
                                true,
                                allWhoDrafted
                        )
                );
            }
        }

        return null;
    }

    @Override
    public TurnType getType() {
        return TurnType.DISCARD_CARDS;
    }
}
