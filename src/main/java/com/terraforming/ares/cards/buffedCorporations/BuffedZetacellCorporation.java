package com.terraforming.ares.cards.buffedCorporations;

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
public class BuffedZetacellCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public BuffedZetacellCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Zetacell")
                .description("47 Mc. Take 5 card and then discard 4. When you flip an ocean, get 2 MC and 2 Plants.")
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
        player.setMc(47);
        marsContext.dealCards(5);
        player.addNextTurn(
                new DiscardCardsTurn(
                        player.getUuid(),
                        List.of(),
                        4,
                        false,
                        true
                )
        );
        return null;
    }

    @Override
    public void onOceanFlippedEffect(MarsContext context) {
        final Player player = context.getPlayer();
        player.setMc(player.getMc() + 2);
        player.setPlants(player.getPlants() + 2);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BUFFED_CORPORATION;
    }

    @Override
    public int getPrice() {
        return 47;
    }

}
