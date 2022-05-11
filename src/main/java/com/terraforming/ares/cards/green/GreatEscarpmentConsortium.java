package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
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
public class GreatEscarpmentConsortium implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public GreatEscarpmentConsortium(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Great Escarpment Consortium")
                .description("When you play a Building, you pay 2 MC less for it.")
                .incomes(List.of(Gain.of(GainType.STEEL, 1)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setSteelIncome(player.getSteelIncome() + 1);

        return null;
    }

    @Override
    public int getPrice() {
        return 3;
    }
}
