package com.terraforming.ares.cards.green;

import com.terraforming.ares.mars.MarsGame;
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
public class AtmosphericInsulators implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard project, Map<Integer, List<Integer>> inputParams) {
        int earthTags = (int) project.getTags().stream().filter(Tag.EARTH::equals).count();

        player.setMcIncome(player.getMcIncome() + earthTags);
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public String description() {
        //TODO all tag effects should also include corporations
        return "During the production phase, this produces 1 heat per ERT you have, including this.";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 10;
    }
}
