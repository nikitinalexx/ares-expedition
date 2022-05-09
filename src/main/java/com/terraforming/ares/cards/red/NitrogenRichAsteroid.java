package com.terraforming.ares.cards.red;

import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
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
public class NitrogenRichAsteroid implements BaseExpansionRedCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        TerraformingService terraformingService = marsContext.getTerraformingService();
        CardService cardService = marsContext.getCardService();

        player.setTerraformingRating(player.getTerraformingRating() + 2);
        terraformingService.increaseTemperature(marsContext.getGame(), marsContext.getPlayer());
        player.setPlants(player.getPlants() + 2);

        long plantsMin3 = player.getPlayed().getCards().stream().map(cardService::getProjectCard)
                .flatMap(card -> card.getTags().stream())
                .filter(Tag.PLANT::equals)
                .limit(3)
                .count();

        if (plantsMin3 == 3) {
            player.setPlants(player.getPlants() + 4);
        }

        return null;
    }

    @Override
    public String description() {
        return "Raise your TR 2 steps. Raise the temperature 1 step. Gain 2 plants. If you have 3 or more Plant, gain 4 additional plants.";
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
