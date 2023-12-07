package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.DiscoveryExpansionBlueCard;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 19.02.2022
 */
@RequiredArgsConstructor
@Getter
public class BuffedDroneAssistedConstruction implements DiscoveryExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public BuffedDroneAssistedConstruction(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Drone Assisted Construction")
                .description("Action: Gain 2 MC. *if you played an upgraded phase this turn, gain 2 MC.")
                .cardAction(CardAction.DRONE_ASSISTED_CONSTRUCTION)
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
        return Collections.singletonList(Tag.SPACE);
    }

    @Override
    public int getPrice() {
        return 7;
    }
}
