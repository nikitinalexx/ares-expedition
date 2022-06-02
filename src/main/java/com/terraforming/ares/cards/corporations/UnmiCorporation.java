package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@RequiredArgsConstructor
@Getter
public class UnmiCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public UnmiCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Unmi")
                .description("35 Mc. When you first raise RT during the phase, you may spend 6 MC to get 1 extra RT.")
                .cardAction(CardAction.UNMI_CORPORATION)
                .build();
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(35);
        player.setUnmiCorporation(true);
        return null;
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

}
