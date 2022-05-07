package com.terraforming.ares.cards.blue;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
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
public class EnergySubsidies implements BlueCard {
    private final int id;

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.ENERGY_SUBSIDIES_DISCOUNT_4);
    }

    @Override
    public String description() {
        return "When you play an Energy tag, you pay 4 МС less for it and you draw a card.";
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public void onProjectBuiltEffect(MarsGame game, PlayerContext player, ProjectCard project, Map<Integer, Integer> inputParams) {
        if (!project.getTags().contains(Tag.ENERGY)) {
            return;
        }

        for (Integer cardId : game.getProjectsDeck().dealCards(1).getCards()) {
            player.getHand().addCard(cardId);
        }
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE);
    }

    @Override
    public int getPrice() {
        return 5;
    }
}
