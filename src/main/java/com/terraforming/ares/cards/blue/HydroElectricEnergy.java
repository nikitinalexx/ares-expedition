package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class HydroElectricEnergy implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public HydroElectricEnergy(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Hydro-Electric Energy")
                .description("Spend 1 MC to get 2 heat.  *if you chose the action phase this round, gain 1 additional heat.")
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
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 11;
    }
}
