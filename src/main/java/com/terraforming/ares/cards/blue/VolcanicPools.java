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
public class VolcanicPools implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public VolcanicPools(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Volcanic Pools")
                .description("Action: Spend 12 MC to flip an ocean tile. Reduce this by 1 MC per Energy tag you have.")
                .cardAction(CardAction.VOLCANIC_POOLS)
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
        return List.of(Tag.SPACE);
    }

    @Override
    public int getPrice() {
        return 17;
    }
}
