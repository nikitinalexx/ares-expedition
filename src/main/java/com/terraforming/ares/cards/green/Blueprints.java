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
public class Blueprints implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Blueprints(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Blueprints")
                .description("During the production phase you draw a card and this produces 2 MC.")
                .incomes(List.of(
                        Gain.of(GainType.CARD, 1),
                        Gain.of(GainType.MC, 2)
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

        player.setMcIncome(player.getMcIncome() + 2);

        return marsContext.dealCards(1);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 17;
    }
}
