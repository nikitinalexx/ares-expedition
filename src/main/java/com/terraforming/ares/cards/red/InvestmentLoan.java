package com.terraforming.ares.cards.red;

import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class InvestmentLoan implements BaseExpansionRedCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setTerraformingRating(player.getTerraformingRating() - 1);
        player.setMc(player.getMc() + 10);

        return null;
    }

    @Override
    public String description() {
        return "Lose 1 TR. Get 10 MC.";
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EARTH, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 1;
    }

}
