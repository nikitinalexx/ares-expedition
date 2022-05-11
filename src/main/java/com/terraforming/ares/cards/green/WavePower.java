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
public class WavePower implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public WavePower(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Wave Power")
                .description("Requires 3 ocean tiles to be flipped. During the production phase, this produces 3 heat.")
                .incomes(List.of(Gain.of(GainType.HEAT, 3)))
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

        return null;
    }

    @Override
    public LongPredicate getOceanRequirement() {
        return currentNumberOfOceans -> currentNumberOfOceans >= 3;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 9;
    }
}
