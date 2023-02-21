package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.DiscoveryExpansionBlueCard;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@RequiredArgsConstructor
@Getter
public class VolcanicSoil implements DiscoveryExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public VolcanicSoil(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Volcanic Soil")
                .description("When you raise the temperature, gain 2 plants.")
                .cardAction(CardAction.VOLCANIC_SOIL)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void onTemperatureChangedEffect(Player player) {
        player.setPlants(player.getPlants() + 2);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.PLANT, Tag.JUPITER);
    }


    @Override
    public int getPrice() {
        return 24;
    }
}
