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
public class AtmosphereFiltering implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public AtmosphereFiltering(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Atmosphere Filtering")
                .description("Requires 2 Science tags. Raise oxygen 1 step.")
                .bonuses(List.of(Gain.of(GainType.OXYGEN, 1)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext
                .getTerraformingService()
                .raiseOxygen(marsContext.getGame(), marsContext.getPlayer());
        return null;
    }

    @Override
    public List<Tag> getTagRequirements() {
        return List.of(Tag.SCIENCE, Tag.SCIENCE);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT, Tag.SPACE);
    }

    @Override
    public int getPrice() {
        return 6;
    }

}
