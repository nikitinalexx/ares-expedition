package com.terraforming.ares.cards.blue;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class RecycledDetritus implements BlueCard {
    private final int id;

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard project, Map<Integer, List<Integer>> inputParams) {
        if (project.getTags().contains(Tag.EVENT)) {
            for (Integer card : game.getProjectsDeck().dealCards(2).getCards()) {
                player.getHand().addCard(card);
            }
        }
    }

    @Override
    public String description() {
        return "When you play an Event, draw 2 cards.";
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public int getPrice() {
        return 24;
    }
}
