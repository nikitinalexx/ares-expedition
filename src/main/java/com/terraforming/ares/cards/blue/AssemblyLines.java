package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@RequiredArgsConstructor
@Getter
public class AssemblyLines implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public AssemblyLines(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Assembly Lines")
                .description("When you use an \"Action:\" effect on one of your cards, gain 1 MC.")
                .cardAction(CardAction.ASSEMBLY_LINES)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.SPACE);
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
    public Set<SpecialEffect> getSpecialEffects() {
        return Collections.singleton(SpecialEffect.ASSEMBLY_LINES);
    }

    @Override
    public int getPrice() {
        return 13;
    }
}
