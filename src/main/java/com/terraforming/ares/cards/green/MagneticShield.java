package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
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
public class MagneticShield implements ExperimentExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public MagneticShield(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Magnetic Shield")
                .description("Increase TR by 4.")
                .bonuses(List.of(Gain.of(GainType.TERRAFORMING_RATING, 4)))
                .cardAction(CardAction.CAPITALISE_DESCRIPTION)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public int getPrice() {
        return 24;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        final Player player = marsContext.getPlayer();

        player.setTerraformingRating(player.getTerraformingRating() + 4);

        return null;
    }

    @Override
    public List<Tag> getTagRequirements() {
        return List.of(Tag.ENERGY, Tag.ENERGY, Tag.ENERGY, Tag.ENERGY);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING);
    }

}
