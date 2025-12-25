package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.CorporationCard;
import com.terraforming.ares.model.Expansion;
import lombok.Getter;

/**
 * Created by oleksii.nikitin
 * Creation date 27.04.2022
 */
@Getter
public class DummyCard implements CorporationCard {

    @Override
    public boolean isBlankCard() {
        return true;
    }

    @Override
    public int getId() {
        return -1;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return null;
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
