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

import java.util.Arrays;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@RequiredArgsConstructor
@Getter
public class GeothermalPower implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public GeothermalPower(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Geothermal Power")
                .description("During the production phase, this produces 2 heat.")
                .incomes(List.of(Gain.of(GainType.HEAT, 2)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setHeatIncome(player.getHeatIncome() + 2);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return Arrays.asList(Tag.BUILDING, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 8;
    }
}
