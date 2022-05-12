package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@RequiredArgsConstructor
@Getter
public class DevTechs implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public DevTechs(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("DevTechs")
                .description("40 Mc. Draw 5 cards from the deck and take all green cards.")
                .build();
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(40);

        List<Integer> cards = marsContext.getGame().dealCards(5);
        for (Integer card : cards) {
            if (marsContext.getCardService().getProjectCard(card).getColor() == CardColor.GREEN) {
                player.getHand().addCard(card);
            }
        }

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.BUILDING);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }
}
