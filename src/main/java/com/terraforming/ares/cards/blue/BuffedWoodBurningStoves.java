package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class BuffedWoodBurningStoves implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public BuffedWoodBurningStoves(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Wood Burning Stoves")
                .actionDescription("Action: Spend 4 plants to raise temperature 1 step. *if you chose the action phase this round, spend 3 plants.")
                .description("Gain 4 plants.")
                .bonuses(List.of(Gain.of(GainType.PLANT, 4)))
                .cardAction(CardAction.WOOD_BURNING_STOVES)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setPlants(player.getPlants() + 4);
        return null;
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.EXPERIMENTAL;
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 9;
    }
}
