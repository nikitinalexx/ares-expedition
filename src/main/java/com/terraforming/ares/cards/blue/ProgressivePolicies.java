package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class ProgressivePolicies implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ProgressivePolicies(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Progressive Policies")
                .description("Action: Spend 10 MC to raise the oxygen 1 step. Reduce this by 5 МС if you have 4 or more Event tags.")
                .cardAction(CardAction.PROGRESSIVE_POLICIES)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE);
    }

    @Override
    public int getPrice() {
        return 8;
    }
}
