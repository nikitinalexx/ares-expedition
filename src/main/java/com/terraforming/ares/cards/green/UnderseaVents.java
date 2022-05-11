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
public class UnderseaVents implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public UnderseaVents(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Undersea Vents")
                .description("During the production phase, you draw a card and this produces 4 heat.")
                .incomes(List.of(
                        Gain.of(GainType.CARD, 1),
                        Gain.of(GainType.HEAT, 4)
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

        player.setHeatIncome(player.getHeatIncome() + 4);
        player.setCardIncome(player.getCardIncome() + 1);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 31;
    }
}
