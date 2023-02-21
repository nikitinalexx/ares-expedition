package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.TerraformingService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class NitrogenRichAsteroid implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public NitrogenRichAsteroid(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Nitrogen-Rich Asteroid")
                .description("Raise your TR 2 steps. Raise the temperature 1 step. Gain 2 plants. If you have 3 or more Plant, gain 4 additional plants.")
                .bonuses(List.of(
                        Gain.of(GainType.TERRAFORMING_RATING, 2),
                        Gain.of(GainType.TEMPERATURE, 1),
                        Gain.of(GainType.PLANT, 2)
                ))
                .cardAction(CardAction.NITROGEN_RICH_ASTEROID)
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
        CardService cardService = marsContext.getCardService();

        player.setTerraformingRating(player.getTerraformingRating() + 2);
        terraformingService.increaseTemperature(marsContext.getGame(), marsContext.getPlayer());
        player.setPlants(player.getPlants() + 2);

        long plantsMin3 = cardService.countPlayedTags(player, Set.of(Tag.PLANT));

        if (plantsMin3 == 3) {
            player.setPlants(player.getPlants() + 4);
        }

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 30;
    }

}
