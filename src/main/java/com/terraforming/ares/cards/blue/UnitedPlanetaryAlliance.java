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
public class UnitedPlanetaryAlliance implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public UnitedPlanetaryAlliance(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("United Planetary Alliance")
                //TODO support 5 phase
                .description("When you draw cards during the research phase, draw one additional card and keep one additional card.")
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
        return false;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 11;
    }
}
