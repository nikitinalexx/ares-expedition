package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.ExperimentExpansionRedCard;
import com.terraforming.ares.model.*;
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
public class DiversitySupport implements ExperimentExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public DiversitySupport(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Diversity Support")
                .description("Requires 9 different tags.")
                .bonuses(List.of(Gain.of(GainType.TERRAFORMING_RATING, 2)))
                .cardAction(CardAction.DIVERSITY_SUPPORT)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setTerraformingRating(player.getTerraformingRating() + 2);
        return null;
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 6;
    }

}
