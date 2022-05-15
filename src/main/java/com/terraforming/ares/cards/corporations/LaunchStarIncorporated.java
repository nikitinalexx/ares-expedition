package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@RequiredArgsConstructor
@Getter
public class LaunchStarIncorporated implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public LaunchStarIncorporated(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("LaunchStar Incorporated")
                .description("36 Mc. Not implemented yet")
                .build();
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().setMc(36);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.SCIENCE);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }
}
