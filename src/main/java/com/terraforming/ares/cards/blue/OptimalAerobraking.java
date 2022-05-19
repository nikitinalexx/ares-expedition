package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class OptimalAerobraking implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public OptimalAerobraking(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Optimal Aerobraking")
                .description("When you play an Event tag, you gain 2 heat and 2 plants.")
                .cardAction(CardAction.OPTIMAL_AEROBRAKING)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, Card project, Map<Integer, List<Integer>> inputParams) {
        if (project.getTags().contains(Tag.EVENT)) {
            player.setHeat(player.getHeat() + 2);
            player.setPlants(player.getPlants() + 2);
        }
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE);
    }

    @Override
    public int getPrice() {
        return 10;
    }
}
