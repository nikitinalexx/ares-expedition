package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.MarsContext;
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
public class CommercialImports implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public CommercialImports(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Commercial Imports")
                .description("During the production phase, this produces 1 Card, 2 Heat and 2 Plants.")
                .incomes(
                        List.of(
                                Gain.of(GainType.CARD, 1),
                                Gain.of(GainType.HEAT, 2),
                                Gain.of(GainType.PLANT, 2)
                        )
                )
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 36;
    }
}
