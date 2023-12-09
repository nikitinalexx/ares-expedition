package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class ProjectInspection implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ProjectInspection(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Project Inspection")
                .description("Next 3rd phase you may use additional action twice.")
                .cardAction(CardAction.CAPITALISE_DESCRIPTION)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().setProjectInspection(1);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 0;
    }

}
