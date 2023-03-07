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
public class Plantation implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Plantation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Plantation")
                .description("Requires 4 science tags. Build 2 forests and raise oxygen 2 steps.")
                .bonuses(List.of(Gain.of(GainType.FOREST, 2)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        for (int i = 0; i < 2; i++) {
            marsContext.getTerraformingService().buildForest(marsContext);
        }
        return null;
    }

    @Override
    public List<Tag> getTagRequirements() {
        return List.of(Tag.SCIENCE, Tag.SCIENCE, Tag.SCIENCE, Tag.SCIENCE);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.PLANT, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 22;
    }

}
