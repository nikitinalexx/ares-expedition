package com.terraforming.ares.cards.green;

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
public class Insects implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard project, Map<Integer, List<Integer>> inputParams) {
        int plantTagsCount = (int) project.getTags().stream().filter(Tag.PLANT::equals).count();

        player.setPlantsIncome(player.getPlantsIncome() + plantTagsCount);
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        //TODO consider corporations with PLANT tag

        int plantTagsCount = (int) player.getPlayed().getCards().stream().map(marsContext.getCardService()::getProjectCard)
                .flatMap(card -> card.getTags().stream())
                .filter(Tag.PLANT::equals)
                .count();

        player.setPlantsIncome(player.getPlantsIncome() + plantTagsCount);


        return null;
    }

    @Override
    public String description() {
        return "During the production phase, this produces 1 plant per Plant tag you have.";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.MICROBE);
    }

    @Override
    public int getPrice() {
        return 10;
    }
}
