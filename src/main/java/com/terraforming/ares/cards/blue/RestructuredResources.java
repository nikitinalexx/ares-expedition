package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.SpecialEffect;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class RestructuredResources implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public RestructuredResources(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Restructured Resources")
                .description("When you play a card, you may spend 1 plant to reduce that card's cost by 5 MC.")
                .cardAction(CardAction.RESTRUCTURED_RESOURCES)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.RESTRUCTURED_RESOURCES);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public int getPrice() {
        return 7;
    }
}
