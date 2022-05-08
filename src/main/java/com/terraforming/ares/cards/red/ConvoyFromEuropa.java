package com.terraforming.ares.cards.red;

import com.terraforming.ares.model.MarsContext;
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
public class ConvoyFromEuropa implements BaseExpansionRedCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getTerraformingService().revealOcean(marsContext.getGame(), marsContext.getPlayer());

        for (Integer card : marsContext.getGame().getProjectsDeck().dealCards(1).getCards()) {
            marsContext.getPlayer().getHand().addCard(card);
        }

        return null;
    }

    @Override
    public String description() {
        return "Draw a Card. Flip an Ocean tile.";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 14;
    }

}
