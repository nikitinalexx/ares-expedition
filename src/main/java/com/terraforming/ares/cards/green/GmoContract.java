package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class GmoContract implements ExperimentExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public GmoContract(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Gmo Contract")
                .description("Requires a Plant tag. Whenever you play Animal,Plant or Microbe tag, you get a discount of 3.")
                .cardAction(CardAction.GMO_CONTRACT)
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
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.GMO_CONTRACT_DISCOUNT_3);
    }


    @Override
    public List<Tag> getTagRequirements() {
        return List.of(Tag.PLANT);
    }


    @Override
    public List<Tag> getTags() {
        return List.of(Tag.MICROBE, Tag.SCIENCE);
    }

    @Override
    public int getPrice() {
        return 3;
    }
}
