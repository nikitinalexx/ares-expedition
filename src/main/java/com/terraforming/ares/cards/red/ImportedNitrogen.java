package com.terraforming.ares.cards.red;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
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
public class ImportedNitrogen implements BaseExpansionRedCard {
    private final int id;

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard project, Map<Integer, List<Integer>> input) {
        List<Integer> animalsInput = input.get(InputFlag.IMPORTED_NITROGEN_ADD_ANIMALS.getId());
        List<Integer> microbesInput = input.get(InputFlag.IMPORTED_NITROGEN_ADD_MICROBES.getId());


        Integer animalsCardId = animalsInput.get(0);
        Integer microbesCardId = microbesInput.get(0);

        if (animalsCardId != InputFlag.SKIP_ACTION.getId()) {
            ProjectCard animalsCard = cardService.getProjectCard(animalsCardId);
            player.getCardResourcesCount().put(animalsCard.getClass(), player.getCardResourcesCount().get(animalsCard.getClass()) + 2);
        }

        if (microbesCardId != InputFlag.SKIP_ACTION.getId()) {
            ProjectCard microbeCard = cardService.getProjectCard(animalsCardId);
            player.getCardResourcesCount().put(microbeCard.getClass(), player.getCardResourcesCount().get(microbeCard.getClass()) + 3);
        }
    }

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
        Player player = marsContext.getPlayer();
        player.setTerraformingRating(player.getTerraformingRating() + 1);
        player.setPlants(player.getPlants() + 4);
        return null;
    }

    @Override
    public String description() {
        return "Raise your TR 1 step. Gain 4 plants. Add 2 animals to ANOTHER card.Add 3 microbes to ANOTHER card.";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.EARTH, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 20;
    }

}
