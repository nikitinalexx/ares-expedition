package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class AssortedEnterprises implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public AssortedEnterprises(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Assorted Enterprises")
                .description("You may play an additional card this phase. That card may be a green card. You pay 2 MC less for the next card you play this phase.")
                .cardAction(CardAction.CAPITALISE_DESCRIPTION)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.getBuilds().add(new BuildDto(BuildType.GREEN_OR_BLUE, 2));
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 2;
    }

}
