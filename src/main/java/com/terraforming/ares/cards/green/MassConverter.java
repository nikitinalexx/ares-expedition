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
public class MassConverter implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public MassConverter(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Mass Converter")
                .description("Requires 4 Science tags. During the production phase, this produces 3 heat. When you play Space, you pay 3 MC less for it.")
                .incomes(List.of(
                        Gain.of(GainType.HEAT, 3),
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

        player.setHeatIncome(player.getHeatIncome() + 3);
        player.setTitaniumIncome(player.getTitaniumIncome() + 1);

        return null;
    }

    @Override
    public List<Tag> getTagRequirements() {
        return List.of(Tag.SCIENCE, Tag.SCIENCE, Tag.SCIENCE, Tag.SCIENCE);
    }

    @Override
    public int getWinningPoints() {
        return 2;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.BUILDING, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 20;
    }
}
