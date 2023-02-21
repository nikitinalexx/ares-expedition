package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        int eventTagsCount = marsContext.getCardService().countCardTags(project, Set.of(Tag.EVENT), inputParams);

        final Player player = marsContext.getPlayer();

        if (eventTagsCount > 0) {
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
