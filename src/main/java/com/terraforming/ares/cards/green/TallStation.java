package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
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
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setMc(player.getMc() + 3);
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

        player.setMcIncome(player.getMcIncome() + 3);
        player.getBuilds().add(new BuildDto(BuildType.GREEN, 9, 9));

        return null;
    }

    @Override
    public int getPrice() {
        return 16;
    }
}
