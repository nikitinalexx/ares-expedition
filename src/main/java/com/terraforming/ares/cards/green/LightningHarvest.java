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
public class LightningHarvest implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard project, Map<Integer, List<Integer>> inputParams) {
        int scienceTagsCount = (int) project.getTags().stream()
                .filter(Tag.SCIENCE::equals)
                .count();

        player.setMcIncome(player.getMcIncome() + scienceTagsCount);
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        int scienceTagsCount = (int) player.getPlayed().getCards().stream().map(marsContext.getCardService()::getProjectCard)
                .flatMap(card -> card.getTags().stream())
                .filter(Tag.SCIENCE::equals)
                .count();

        player.setMcIncome(player.getMcIncome() + scienceTagsCount + 1);

        return null;
    }

    @Override
    public String description() {
        //TODO consider corporations
        return "During the production phase, this produces 1 MC per Science tag you have, including this.";
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 13;
    }
}
