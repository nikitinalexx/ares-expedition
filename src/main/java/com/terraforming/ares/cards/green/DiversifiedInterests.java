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
public class DiversifiedInterests implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public DiversifiedInterests(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Diversified Interests")
                .description("Gain 3 plants and 3 heat. During the production phase, this produces 1 plant.")
                .incomes(List.of(Gain.of(GainType.PLANT, 1)))
                .bonuses(List.of(
                        Gain.of(GainType.PLANT, 3),
                        Gain.of(GainType.HEAT, 3)
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
        player.setPlants(player.getPlants() + 3);
        player.setHeat(player.getHeat() + 3);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 15;
    }
}
