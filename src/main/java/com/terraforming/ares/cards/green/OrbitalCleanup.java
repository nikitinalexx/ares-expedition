package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 17.02.2023
 */
@RequiredArgsConstructor
@Getter
public class OrbitalCleanup implements ExperimentExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public OrbitalCleanup(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Orbital Cleanup")
                .actionDescription("Action: Gain 1 ME per science tag you have.")
                .description("Reduce ME production by 2.")
                .incomes(List.of(Gain.of(GainType.MC, -2)))
                .cardAction(CardAction.ORBITAL_CLEANUP)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setMcIncome(player.getMcIncome() - 2);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EARTH, Tag.SPACE);
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public int getPrice() {
        return 14;
    }

    @Override
    public int getWinningPoints() {
        return 2;
    }

}
