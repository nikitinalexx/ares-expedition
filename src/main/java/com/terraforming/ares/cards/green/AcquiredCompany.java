package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import lombok.Getter;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@Getter
public class AcquiredCompany implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public AcquiredCompany(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Acquired Company")
                .description("During the production phase, draw a card.")
                .incomes(List.of(Gain.of(GainType.CARD, 1)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        //todo use getIncomes
        player.setCardIncome(player.getCardIncome() + 1);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 11;
    }
}
