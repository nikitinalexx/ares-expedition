package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class MedicalLab implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public MedicalLab(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Medical Lab")
                .description("During the production phase, this produces 1 MC per 2 Building you have, including this.")
                .cardAction(CardAction.MC_2_BUILDING_INCOME)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        int buildingTagCount = cardService.countPlayedTags(player, Set.of(Tag.BUILDING));

        player.setMc(player.getMc() + buildingTagCount / 2);
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        final Player player = marsContext.getPlayer();

        int buildingTagCountBefore = marsContext.getCardService().countPlayedTags(player, Set.of(Tag.BUILDING));

        int buildingTagCountAfter = buildingTagCountBefore +
                marsContext.getCardService().countCardTags(project, Set.of(Tag.BUILDING), inputParams);

        int mcIncomeDifference = (buildingTagCountAfter / 2) - (buildingTagCountBefore / 2);

        player.setMcIncome(player.getMcIncome() + mcIncomeDifference);
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        int buildingTagCount = marsContext.getCardService().countPlayedTags(marsContext.getPlayer(), Set.of(Tag.BUILDING));

        int mcIncomeExtra = (buildingTagCount + 1) / 2;

        marsContext.getPlayer().setMcIncome(marsContext.getPlayer().getMcIncome() + mcIncomeExtra);

        return null;
    }

    @Override
    public void revertPlayedTags(CardService cardService, Card card, Player player) {
        int buildingTagCountBefore = cardService.countPlayedTags(player, Set.of(Tag.BUILDING));

        int buildingTagCountAfter = buildingTagCountBefore -
                cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.BUILDING));

        int mcIncomeDifference = (buildingTagCountBefore / 2) - (buildingTagCountAfter / 2);

        if (mcIncomeDifference > 0) {
            player.setMcIncome(player.getMcIncome() - mcIncomeDifference);
        }
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
