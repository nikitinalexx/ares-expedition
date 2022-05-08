package com.terraforming.ares.cards.red;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.Tag;
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
public class CeosFavoriteProject implements BaseExpansionRedCard {
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
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard project, Map<Integer, List<Integer>> inputParams) {
        List<Integer> cardInput = inputParams.get(InputFlag.CEOS_FAVORITE_PUT_RESOURCES.getId());

        int targetCardId = cardInput.get(0);

        ProjectCard targetCard = cardService.getProjectCard(targetCardId);

        player.getCardResourcesCount().put(targetCard.getClass(), player.getCardResourcesCount().get(targetCard.getClass()) + 2);
    }

    @Override
    public String description() {
        return "Add 2 resources to a card that holds resources.";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 3;
    }

}
