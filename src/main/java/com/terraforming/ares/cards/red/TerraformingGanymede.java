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
public class TerraformingGanymede implements BaseExpansionRedCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        int jupiterTags = (int) player.getPlayed()
                .getCards()
                .stream()
                .map(marsContext.getCardService()::getProjectCard)
                .flatMap(projectCard -> projectCard.getTags().stream())
                .filter(Tag.JUPITER::equals).count();

        player.setTerraformingRating(player.getTerraformingRating() + jupiterTags + 1);

        return null;
    }

    @Override
    public String description() {
        return "Raise your TR 1 step per JPT you have, including this.";
    }

    @Override
    public int getWinningPoints() {
        return 2;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.JUPITER, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 28;
    }

}
