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
public class MedicalLab implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard project, Map<Integer, List<Integer>> inputParams) {
        int buildingTagCountBefore = (int) player
                .getPlayed()
                .getCards().stream()
                .map(cardService::getProjectCard)
                .flatMap(card -> card.getTags().stream())
                .filter(Tag.BUILDING::equals).count();

        int buildingTagCountAfter = buildingTagCountBefore +
                (int) project.getTags().stream().filter(Tag.BUILDING::equals).count();

        int mcIncomeDifference = (buildingTagCountAfter / 2) - (buildingTagCountBefore / 2);

        player.setMcIncome(player.getMcIncome() + mcIncomeDifference);
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        int buildingTagCount = (int) marsContext.getPlayer()
                .getPlayed()
                .getCards().stream()
                .map(marsContext.getCardService()::getProjectCard)
                .flatMap(card -> card.getTags().stream())
                .filter(Tag.BUILDING::equals).count();

        int mcIncomeExtra = (buildingTagCount + 1) / 2;

        marsContext.getPlayer().setMcIncome(marsContext.getPlayer().getMcIncome() + mcIncomeExtra);

        return null;
    }

    @Override
    public String description() {
        //TODO consider corporations tags
        return "During the production phase, this produces 1 MC per 2 Building you have, including this.";
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 15;
    }
}
