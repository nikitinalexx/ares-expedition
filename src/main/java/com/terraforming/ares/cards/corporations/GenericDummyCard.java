package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.CorporationCard;
import com.terraforming.ares.model.Expansion;
import lombok.Getter;

/**
 * Created by oleksii.nikitin
 * Creation date 03.01.2026
 */
@Getter
public class GenericDummyCard implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public GenericDummyCard(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder().name("Generic Dummy").build();
    }

    @Override
    public boolean isBlankCard() {
        return true;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public Expansion getExpansion() {
        return null;
    }

    @Override
    public int getPrice() {
        return 0;
    }
}
