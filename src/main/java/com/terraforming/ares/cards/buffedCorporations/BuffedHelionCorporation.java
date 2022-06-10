package com.terraforming.ares.cards.buffedCorporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 27.04.2022
 */
@Getter
public class BuffedHelionCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public BuffedHelionCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Helion Corporation")
                .description("36 Mc. 3 Heat income. You may use Heat as Mc, but not vise versa.")
                .cardAction(CardAction.HELION_CORPORATION)
                .build();
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(36);
        player.setHeatIncome(3);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.SPACE);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BUFFED_CORPORATION;
    }

    @Override
    public int getPrice() {
        return 36;
    }

}
