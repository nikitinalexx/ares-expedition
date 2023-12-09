package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.ExperimentExpansionBlueCard;
import com.terraforming.ares.model.CardAction;
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
public class MicroorganismIndustry implements ExperimentExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public MicroorganismIndustry(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Microorganism Industry")
                .description("When you add any number of microbes to ANOTHER* card, gain 2 MC.")
                .cardAction(CardAction.MICRO_INDUSTRY)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.BUILDING);
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.MICRO_INDUSTRY);
    }

    @Override
    public int getPrice() {
        return 5;
    }

}
