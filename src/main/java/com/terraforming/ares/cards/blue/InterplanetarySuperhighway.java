package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.InfrastructureExpansionBlueCard;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 03.12.2023
 */
@RequiredArgsConstructor
@Getter
public class InterplanetarySuperhighway implements InfrastructureExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public InterplanetarySuperhighway(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Interplanetary Superhighway")
                .description("Action: Spend 10 MC to raise infrastructure. Reduce this by 5 MC if you have at least 4 science.")
                .cardAction(CardAction.INTERPLANETARY_SUPERHIGHWAY)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.SPACE, Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 15;
    }
}
