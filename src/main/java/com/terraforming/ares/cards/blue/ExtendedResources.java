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
public class ExtendedResources implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ExtendedResources(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Extended Resources")
                //TODO during the 5 phase
                .description("When you keep cards during the research phase, keep one additional card.")
                .cardAction(CardAction.EXTENDED_RESOURCES)
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
        return List.of(Tag.SCIENCE, Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 10;
    }
}
