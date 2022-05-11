package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.CardAction;
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
public class WaterImportFromEuropa implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public WaterImportFromEuropa(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Water Import from Europa")
                .description("Spend 12 MC to flip an ocean tile. Reduce this by 1 MC per titanium income you have. 1 VP per Jupiter you have.")
                .cardAction(CardAction.WATER_IMPORT)
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
        return List.of(Tag.SPACE, Tag.JUPITER);
    }

    @Override
    public int getPrice() {
        return 22;
    }
}
