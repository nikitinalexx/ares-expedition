package com.terraforming.ares.cards.blue;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class InterplanetaryConference implements BlueCard {
    private final int id;

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.INTERPLANETARY_CONFERENCE);
    }

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard project, Map<Integer, List<Integer>> inputParams) {
        int cardsToGiveCount = 0;

        if (project.getTags().contains(Tag.EARTH)) {
            cardsToGiveCount++;
        }

        if (project.getTags().contains(Tag.JUPITER)) {
            cardsToGiveCount++;
        }

        if (cardsToGiveCount == 0) {
            return;
        }

        for (Integer card : game.getProjectsDeck().dealCards(cardsToGiveCount).getCards()) {
            player.getHand().addCard(card);
        }
    }

    @Override
    public String description() {
        return "When you play an ERT or JPT, excluding this, you pay 3 MC less and draw a card.";
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
    public List<Tag> getTags() {
        return List.of(Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 6;
    }
}
