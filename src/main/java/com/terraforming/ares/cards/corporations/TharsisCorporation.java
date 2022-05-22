package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;

import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 27.04.2022
 */
@Getter
public class TharsisCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.THARSIS_REPUBLIC);
    }

    public TharsisCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Tharsis Republic")
                .description("40 Mc. When you draw cards during the research phase, draw one additional card and keep one additional card")
                .cardAction(CardAction.THARSIS_CORPORATION)
                .build();
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(40);
        return null;
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

}
