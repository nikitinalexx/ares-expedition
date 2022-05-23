package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
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
    private final CardMetadata cardMetadata;

    public ConvoyFromEuropa(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Convoy from Europa")
                .description("Draw a card. Flip an ocean tile.")
                .bonuses(List.of(Gain.of(GainType.CARD, 1), Gain.of(GainType.OCEAN, 1)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        MarsGame game = marsContext.getGame();
        marsContext.getTerraformingService().revealOcean(game, marsContext.getPlayer());

        for (Integer card : marsContext.getCardService().dealCards(game, 1)) {
            marsContext.getPlayer().getHand().addCard(card);
        }

        return null;
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
