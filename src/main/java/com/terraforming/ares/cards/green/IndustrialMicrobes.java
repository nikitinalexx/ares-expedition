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
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class IndustrialMicrobes implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public IndustrialMicrobes(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Industrial Microbes")
                .description("During the production phase, this produces 1 heat. When you play a Building, you pay 2 MC less for it.")
                .incomes(List.of(
                        Gain.of(GainType.HEAT, 1),
                        Gain.of(GainType.STEEL, 1)
                ))
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setHeat(player.getHeat() + 1);
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

        player.setHeatIncome(player.getHeatIncome() + 1);
        player.setSteelIncome(player.getSteelIncome() + 1);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.MICROBE);
    }

    @Override
    public int getPrice() {
        return 9;
    }
}
