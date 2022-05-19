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
public class AsteroidMiningConsortium implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public AsteroidMiningConsortium(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Asteroid Mining Consortium")
                .description("When you play a Space, you pay 3 MC less for it.")
                .incomes(List.of(Gain.of(GainType.TITANIUM, 1)))
                .build();
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setTitaniumIncome(player.getTitaniumIncome() + 1);

        return null;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public int getPrice() {
        return 6;
    }
}
