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
public class SolarPunk implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public SolarPunk(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Solarpunk")
                .description("Action: Spend 15 MC to build a Forest and raise oxygen 1 step. Reduce this by 2 MC per titanium you have.")
                .cardAction(CardAction.SOLAR_PUNK)
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
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 15;
    }
}
