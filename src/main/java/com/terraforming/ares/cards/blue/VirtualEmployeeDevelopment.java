package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.DiscoveryExpansionBlueCard;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 19.02.2022
 */
@RequiredArgsConstructor
@Getter
public class VirtualEmployeeDevelopment implements DiscoveryExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public VirtualEmployeeDevelopment(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Virtual Employee Development")
                .description("Action: Upgrade a phase card.")
                .cardAction(CardAction.VIRTUAL_EMPLOYEE_DEVELOPMENT)
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
    public List<Tag> getTagRequirements() {
        return List.of(Tag.SCIENCE, Tag.SCIENCE, Tag.SCIENCE);
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public int getPrice() {
        return 10;
    }
}
