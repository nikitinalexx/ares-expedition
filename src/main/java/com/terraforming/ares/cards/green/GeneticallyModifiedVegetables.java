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
 * Creation date 17.02.2023
 */
@RequiredArgsConstructor
@Getter
public class GeneticallyModifiedVegetables implements DiscoveryExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public GeneticallyModifiedVegetables(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Genetically Modified Vegetables")
                .description("During the production phase, this produces 3 plants.")
                .incomes(List.of(Gain.of(GainType.PLANT, 3)))
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setPlants(player.getPlants() + 3);
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setPlantsIncome(player.getPlantsIncome() + 3);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.MICROBE, Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 26;
    }

}
