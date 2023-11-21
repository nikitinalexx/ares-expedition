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
public class Insects implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Insects(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Insects")
                .description("During the production phase, this produces 1 plant per Plant you have.")
                .cardAction(CardAction.PLANT_PLANT_INCOME)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        int plantTagCount = cardService.countPlayedTags(player, Set.of(Tag.PLANT));

        player.setPlants(player.getPlants() + plantTagCount);
    }

    @Override
    public boolean canPayAgain() {
        return true;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        int plantTagsCount = marsContext.getCardService().countCardTags(project, Set.of(Tag.PLANT), inputParams);

        final Player player = marsContext.getPlayer();
        player.setPlantsIncome(player.getPlantsIncome() + plantTagsCount);
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        int plantTagsCount = marsContext.getCardService().countPlayedTags(marsContext.getPlayer(), Set.of(Tag.PLANT));

        player.setPlantsIncome(player.getPlantsIncome() + plantTagsCount);

        return null;
    }

    @Override
    public void revertPlayedTags(CardService cardService, Card card, Player player) {
        int plantTagCount = cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.PLANT));

        player.setPlantsIncome(player.getPlantsIncome() - plantTagCount);
    }

    @Override
    public void revertCardIncome(MarsContext marsContext) {
        final Player player = marsContext.getPlayer();
        int plantTags = marsContext.getCardService().countPlayedTags(player, Set.of(Tag.PLANT));
        player.setPlantsIncome(player.getPlants() - plantTags);
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
