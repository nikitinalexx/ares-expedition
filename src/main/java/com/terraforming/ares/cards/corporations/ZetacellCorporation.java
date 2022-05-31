package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import lombok.Getter;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 27.04.2022
 */
@Getter
public class ZetacellCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ZetacellCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Zetacell")
                .description("43 Mc. Take 5 card and then discard 4. When you flip an ocean, get 2 MC and 2 Plants.")
                .cardAction(CardAction.ZETACELL_CORPORATION)
                .build();
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(43);
        marsContext.dealCards(5);
        player.setNextTurn(
                new DiscardCardsTurn(
                        player.getUuid(),
                        List.of(),
                        4,
                        false
                )
        );
        return null;
    }

    @Override
    public void onOceanFlippedEffect(Player player) {
        player.setMc(player.getMc() + 2);
        player.setPlants(player.getPlants() + 2);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

}
