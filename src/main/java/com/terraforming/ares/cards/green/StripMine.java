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
public class StripMine implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public StripMine(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Strip Mine")
                .description("Requires you to spend 1 TR. When you play a Building, you pay 4 less for it. When you play a Space, you pay 3 less for it.")
                .incomes(List.of(
                        Gain.of(GainType.STEEL, 2),
                        Gain.of(GainType.TITANIUM, 1)
                ))
                //todo validation
                .incomes(List.of(Gain.of(GainType.TERRAFORMING_RATING, -1)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setSteelIncome(player.getSteelIncome() + 2);
        player.setTitaniumIncome(player.getTitaniumIncome() + 1);
        player.setTerraformingRating(player.getTerraformingRating() - 1);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 12;
    }
}
