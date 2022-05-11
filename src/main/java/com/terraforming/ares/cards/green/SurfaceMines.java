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
public class SurfaceMines implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public SurfaceMines(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Surface Mines")
                .description("When you play a Building, you pay 2 less for it. When you play a Space, you pay 3 less for it.")
                .incomes(List.of(
                        Gain.of(GainType.STEEL, 1),
                        Gain.of(GainType.TITANIUM, 1)
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

        player.setSteelIncome(player.getSteelIncome() + 1);
        player.setTitaniumIncome(player.getTitaniumIncome() + 1);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 13;
    }
}
