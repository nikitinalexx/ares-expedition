package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 19.02.2023
 */
@RequiredArgsConstructor
@Getter
public class NuclearDetonationSite implements DiscoveryExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public NuclearDetonationSite(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Nuclear Detonation Site")
                .description("During the production phase, this produces 3 heat.")
                .incomes(List.of(Gain.of(GainType.HEAT, 3)))
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setHeat(player.getHeat() + 3);
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setHeatIncome(player.getHeatIncome() + 3);

        return null;
    }

    @Override
    public int getPrice() {
        return 12;
    }
}
