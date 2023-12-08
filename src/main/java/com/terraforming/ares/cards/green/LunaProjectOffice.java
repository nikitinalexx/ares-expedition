package com.terraforming.ares.cards.green;

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
public class LunaProjectOffice implements ExperimentExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public LunaProjectOffice(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Luna Project Office")
                .description("Next 2 plays of phase 5, see and take 1 more card.")
                .cardAction(CardAction.CAPITALISE_DESCRIPTION)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public int getPrice() {
        return 4;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        final Player player = marsContext.getPlayer();

        player.setLunaProjectOffice(2);

        return null;
    }

    @Override
    public List<Tag> getTagRequirements() {
        return List.of(Tag.SCIENCE, Tag.SCIENCE);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE);
    }

}
