package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@RequiredArgsConstructor
@Getter
public class BrainstormingSession implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public BrainstormingSession(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Brainstorming Session")
                .description("Action: Reveal the top card of the deck. If it is green, discard it and gain 1 MC. Otherwise, draw it.")
                .cardAction(CardAction.BRAINSTORMING_SESSION)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.SPACE);
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
        return 8;
    }
}
