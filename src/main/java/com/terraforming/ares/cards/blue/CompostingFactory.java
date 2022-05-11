package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@RequiredArgsConstructor
@Getter
public class CompostingFactory implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public CompostingFactory(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Composting Factory")
                .description("Cards you discard for MC are worth an additional MC.")
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
    public Set<SpecialEffect> getSpecialEffects() {
        return Collections.singleton(SpecialEffect.SOLD_CARDS_COST_1_MC_MORE);
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.BUILDING);
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public int getPrice() {
        return 13;
    }
}
