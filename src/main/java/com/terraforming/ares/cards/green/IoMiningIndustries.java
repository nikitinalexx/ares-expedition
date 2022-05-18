package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class IoMiningIndustries implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public IoMiningIndustries(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Io Mining Industries")
                .description("During the production phase, this produces 2 МС. When you play Space, you pay 6 MC less for it. 1 VP per Jupiter tag you have.")
                .incomes(List.of(
                        Gain.of(GainType.MC, 2),
                        Gain.of(GainType.TITANIUM, 2)
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
        player.setTitaniumIncome(player.getTitaniumIncome() + 2);

        return null;
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.IO_MINING_INDUSTRIES);
    }

    @Override
    public int getWinningPoints() {
        //TODO vp
        return 0;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.SPACE, Tag.JUPITER);
    }

    @Override
    public int getPrice() {
        return 37;
    }
}
