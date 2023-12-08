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
public class Harvest implements ExperimentExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Harvest(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Harvest")
                .description("Requires that you have 3 forests. Gain 12 MC.")
                .bonuses(List.of(Gain.of(GainType.MC, 12)))
                .cardAction(CardAction.HARVEST)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setMc(player.getMc() + 12);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.PLANT, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 4;
    }

}
