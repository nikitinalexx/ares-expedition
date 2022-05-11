package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
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
public class BribedComittee implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public BribedComittee(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Bribed Comittee")
                .description("Raise your TR 2 steps.")
                .bonuses(List.of(Gain.of(GainType.TERRAFORMING_RATING, 2)))
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
    public int getWinningPoints() {
        return -2;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EARTH, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 5;
    }

}
