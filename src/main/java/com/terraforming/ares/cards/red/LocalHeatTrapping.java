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
public class LocalHeatTrapping implements BaseExpansionRedCard {
    private final int id;

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard project, Map<Integer, List<Integer>> input) {
        List<Integer> cardInput = input.get(InputFlag.LOCAL_HEAT_TRAPPING_PUT_RESOURCE.getId());


        Integer cardId = cardInput.get(0);

        if (cardId == InputFlag.SKIP_ACTION.getId()) {
            return;
        }

        ProjectCard inputCard = cardService.getProjectCard(cardId);

        player.getCardResourcesCount().put(
                inputCard.getClass(),
                player.getCardResourcesCount().get(inputCard.getClass()) + 2
        );
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return false;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setHeat(player.getHeat() - 3);
        player.setPlants(player.getPlants() + 4);

        return null;
    }

    @Override
    public String description() {
        return "Requires you to spend 3 heat. Gain 4 plants and add 2 animals or microbes to ANOTHER card.";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 0;
    }

}
