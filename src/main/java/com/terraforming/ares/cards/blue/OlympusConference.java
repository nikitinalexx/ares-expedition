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
public class OlympusConference implements BlueCard {
    private final int id;

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard project, Map<Integer, List<Integer>> inputParams) {
        long scienceTags = project.getTags().stream().map(Tag.SCIENCE::equals).count();

        if (scienceTags == 0) {
            return;
        }

        for (Integer card : game.getProjectsDeck().dealCards((int) scienceTags).getCards()) {
            player.getHand().addCard(card);
        }

    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        //TODO test self trigger
        return true;
    }

    @Override
    public String description() {
        return "When you play Science tag, including this, draw a card.";
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
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.BUILDING, Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 15;
    }
}
