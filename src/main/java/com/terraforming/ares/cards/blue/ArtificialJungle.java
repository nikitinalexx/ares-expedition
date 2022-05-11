package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.Expansion;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@RequiredArgsConstructor
@Getter
public class ArtificialJungle implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ArtificialJungle(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Artificial Jungle")
                .description("Action: Spend 1 plant to draw a card.")
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public int getPrice() {
        return 5;
    }
}
