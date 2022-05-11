package com.terraforming.ares.cards.green;

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
public class ArtificialPhotosynthesis implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ArtificialPhotosynthesis(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Artificial Photosynthesis")
                .description("During the production phase, this produces 1 plant and 1 heat.")
                .incomes(List.of(
                        Gain.of(GainType.PLANT, 1),
                        Gain.of(GainType.HEAT, 1)
                ))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setPlantsIncome(player.getPlantsIncome() + 1);
        player.setHeatIncome(player.getHeatIncome() + 1);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE);
    }

    @Override
    public int getPrice() {
        return 11;
    }
}
