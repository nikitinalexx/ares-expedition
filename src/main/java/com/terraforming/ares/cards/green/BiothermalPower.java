package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
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
public class BiothermalPower implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public BiothermalPower(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Biothermal Power")
                .description("Build a forest and raise oxygen 1 step. During the production phase, this produces 1 heat.")
                .incomes(List.of(Gain.of(GainType.HEAT, 1)))
                .bonuses(List.of(Gain.of(GainType.FOREST, 1)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        TerraformingService terraformingService = marsContext.getTerraformingService();

        player.setHeatIncome(player.getHeatIncome() + 1);
        terraformingService.buildForest(marsContext.getGame(), player);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 18;
    }
}
