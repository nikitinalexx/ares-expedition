package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.DiscoveryExpansionBlueCard;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 21.02.2023
 */
@RequiredArgsConstructor
@Getter
public class GasCooledReactors implements DiscoveryExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public GasCooledReactors(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Gas-Cooled Reactors")
                .description("Action: Spend 12 MC to raise the temperature. Reduce this by 2 MC for every upgraded phase card you have.")
                .cardAction(CardAction.GAS_COOLED_REACTORS)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 16;
    }
}
