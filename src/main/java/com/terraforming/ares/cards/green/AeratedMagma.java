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
public class AeratedMagma implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public AeratedMagma(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Aerated Magma")
                .description("Requires red oxygen or higher. During the production phase you draw a card and this produces 2 heat.")
                .incomes(List.of(Gain.of(GainType.CARD, 1), Gain.of(GainType.HEAT, 2)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setCardIncome(player.getCardIncome() + 1);
        player.setHeatIncome(player.getHeatIncome() + 2);

        return null;
    }

    @Override
    public List<ParameterColor> getOxygenRequirement() {
        return List.of(ParameterColor.R, ParameterColor.Y, ParameterColor.W);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 18;
    }
}
