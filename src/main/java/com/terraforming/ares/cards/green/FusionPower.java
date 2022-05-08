package com.terraforming.ares.cards.green;

import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@RequiredArgsConstructor
@Getter
public class FusionPower implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setCardIncome(player.getCardIncome() + 1);
        return null;
    }

    @Override
    public List<Tag> getTagRequirements() {
        return List.of(Tag.ENERGY, Tag.ENERGY);
    }

    @Override
    public String description() {
        return "During the production phase, draw a card.";
    }

    @Override
    public List<Tag> getTags() {
        return Arrays.asList(Tag.SCIENCE, Tag.BUILDING, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 7;
    }
}
