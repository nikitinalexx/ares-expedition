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
import java.util.function.LongPredicate;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class GreatDam implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public GreatDam(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Great Dam")
                .description("Requires 2 ocean tiles to be flipped. During the production phase, this produces 2 heat.")
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
    public LongPredicate getOceanRequirement() {
        return currentNumberOfOceans -> currentNumberOfOceans >= 2;
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 12;
    }
}
