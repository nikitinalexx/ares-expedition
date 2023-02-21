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
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class ProtectedValley implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ProtectedValley(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Protected Valley")
                .description("Build a forest and raise oxygen 1 step. During the production phase, this produces 2 МС.")
                .incomes(List.of(Gain.of(GainType.MC, 2)))
                .bonuses(List.of(Gain.of(GainType.FOREST, 1)))
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setMc(player.getMc() + 2);
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        TerraformingService terraformingService = marsContext.getTerraformingService();
        Player player = marsContext.getPlayer();

        terraformingService.buildForest(marsContext.getGame(), player);

        player.setMcIncome(player.getMcIncome() + 2);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 22;
    }
}
