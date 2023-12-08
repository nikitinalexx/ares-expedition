package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class FloydContinuum implements ExperimentExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public FloydContinuum(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Floyd Continuum")
                .description("Action: Gain 3 MC per completed terraforming parameter.")
                .cardAction(CardAction.FLOYD_CONTINUUM)
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
        return List.of(Tag.SCIENCE);
    }

    @Override
    public int getPrice() {
        return 4;
    }

}
