package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class TradingPost implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public TradingPost(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Trading Post")
                .description("Gain 3 plants. During the production phase, this produces 2 МС.")
                .incomes(List.of(Gain.of(GainType.MC, 2)))
                .bonuses(List.of(Gain.of(GainType.PLANT, 3)))
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setMc(player.getMc() + 2);
    }

    @Override
    public boolean canPayAgain() {
        return true;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setPlants(player.getPlants() + 3);
        player.setMcIncome(player.getMcIncome() + 2);

        return null;
    }

    @Override
    public int getPrice() {
        return 11;
    }
}
