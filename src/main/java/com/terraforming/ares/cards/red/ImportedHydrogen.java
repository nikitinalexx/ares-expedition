package com.terraforming.ares.cards.red;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.TerraformingService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class ImportedHydrogen implements BaseExpansionRedCard {
    private final int id;

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return false;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        TerraformingService terraformingService = marsContext.getTerraformingService();

        terraformingService.revealOcean(marsContext.getGame(), marsContext.getPlayer());

        return null;
    }

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard project, Map<Integer, List<Integer>> input) {
        if (input.containsKey(InputFlag.IMPORTED_HYDROGEN_PICK_PLANT.getId())) {
            player.setPlants(player.getPlants() + 3);
            return;
        }

        int inputCardId = input.get(InputFlag.IMPORTED_HYDROGEN_PUT_RESOURCE.getId()).get(0);

        ProjectCard inputCard = cardService.getProjectCard(inputCardId);
        int resourcedToAdd = 2;
        if (inputCard.getCollectableResource() == CardCollectableResource.MICROBE) {
            resourcedToAdd = 3;
        }

        player.getCardResourcesCount().put(
                inputCard.getClass(),
                player.getCardResourcesCount().get(inputCard.getClass()) + resourcedToAdd
        );
    }

    @Override
    public String description() {
        return "Flip an ocean tile. Gain 3 plants, or add 3 microbes or 2 animals to ANOTHER card.";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.EARTH, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 17;
    }

}
