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
public class EnergyStorage implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public EnergyStorage(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Energy Storage")
                .description("Requires you to have 7 or more TR. During the production phase, draw 2 cards.")
                .incomes(List.of(Gain.of(GainType.CARD, 2)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setCardIncome(player.getCardIncome() + 2);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 18;
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }
}
