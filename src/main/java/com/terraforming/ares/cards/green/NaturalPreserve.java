package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.parameters.ParameterColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class NaturalPreserve implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public NaturalPreserve(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Natural Preserve")
                .description("Requires red oxygen or higher. During the production phase, this produces 2 МС.")
                .incomes(List.of(Gain.of(GainType.MC, 2)))
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

        return null;
    }

    @Override
    public List<ParameterColor> getOxygenRequirement() {
        return List.of(ParameterColor.R, ParameterColor.Y, ParameterColor.W);
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 12;
    }
}
