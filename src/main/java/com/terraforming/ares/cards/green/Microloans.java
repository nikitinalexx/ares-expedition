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
import com.terraforming.ares.services.TerraformingService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 04.12.2023
 */
@RequiredArgsConstructor
@Getter
public class Microloans implements InfrastructureExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Microloans(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Microloans")
                .description("Raise the infrastructure 1 step. During the production phase, this produces 2 MC.")
                .incomes(List.of(Gain.of(GainType.MC, 2)))
                .bonuses(List.of(Gain.of(GainType.INFRASTRUCTURE, 1)))
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
        TerraformingService terraformingService = marsContext.getTerraformingService();

        terraformingService.increaseInfrastructure(marsContext);
        player.setMcIncome(player.getMcIncome() + 2);

        return null;
    }

    @Override
    public int getPrice() {
        return 16;
    }

}
