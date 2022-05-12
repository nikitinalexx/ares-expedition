package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.CardAction;
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
public class TallStation implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public TallStation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Tall Station")
                .description("You may play a green card from your hand that has a printed cost of 9 MC or less without payind its MC cost. During the production phase, this produces 3 МС.")
                .incomes(List.of(Gain.of(GainType.MC, 3)))
                .cardAction(CardAction.TALL_STATION)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setMcIncome(player.getMcIncome() + 3);
        player.setCanBuildInFirstPhase(player.getCanBuildInFirstPhase() + 1);
        player.setCanBuildAnotherGreenWith9Discount(true);

        return null;
    }

    @Override
    public int getPrice() {
        return 16;
    }
}
