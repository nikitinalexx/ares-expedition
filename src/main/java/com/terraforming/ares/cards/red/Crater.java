package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class Crater implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Crater(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Crater")
                .description("Requires 3 EVT. Flip an ocean tile.")
                .bonuses(List.of(Gain.of(GainType.OCEAN, 1)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getTerraformingService().revealOcean(marsContext);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT);
    }

    @Override
    public List<Tag> getTagRequirements() {
        return List.of(Tag.EVENT, Tag.EVENT, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 7;
    }

}
