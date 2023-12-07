package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.ExperimentExpansionRedCard;
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
public class Conscription implements ExperimentExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Conscription(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Conscription")
                .description("You may play an additional card this phase. That card may be a green card. You pay 16 MC less for the next card you play this phase.")
                .cardAction(CardAction.CONSCRIPTION)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.getBuilds().add(new BuildDto(BuildType.GREEN_OR_BLUE, 16));
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EARTH, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 5;
    }

    @Override
    public int getWinningPoints() {
        return -1;
    }

    @Override
    public List<Tag> getTagRequirements() {
        return List.of(Tag.EARTH, Tag.EARTH);
    }

}
