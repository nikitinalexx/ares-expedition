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
public class VentureCapitalism implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard project, Map<Integer, List<Integer>> inputParams) {
        int eventTagCount = (int) project.getTags().stream().filter(Tag.EVENT::equals).count();

        player.setMcIncome(player.getMcIncome() + eventTagCount);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        int eventTagCount = (int) marsContext.getPlayer()
                .getPlayed()
                .getCards().stream()
                .map(marsContext.getCardService()::getProjectCard)
                .flatMap(card -> card.getTags().stream())
                .filter(Tag.EVENT::equals).count();

        marsContext.getPlayer().setMcIncome(marsContext.getPlayer().getMcIncome() + eventTagCount);

        return null;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public String description() {
        return "During the production phase, this produces 1 МС per Event tag you have.";
    }

    @Override
    public int getPrice() {
        return 11;
    }
}
