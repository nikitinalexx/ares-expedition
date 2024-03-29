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
public class DiverseHabitats implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public DiverseHabitats(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Diverse Habitats")
                .description("During the production phase, this produces 1 MC per Animal and Plant tag you have, including this.")
                .cardAction(CardAction.MC_ANIMAL_PLANT_INCOME)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        int tagCount = cardService.countPlayedTags(player, Set.of(Tag.ANIMAL, Tag.PLANT));

        player.setMc(player.getMc() + tagCount);
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
        int projectTagsCount = marsContext.getCardService().countCardTags(project, Set.of(Tag.ANIMAL, Tag.PLANT), inputParams);

        final Player player = marsContext.getPlayer();

        player.setMcIncome(player.getMcIncome() + projectTagsCount);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        int tagsPlayed = 1 + marsContext.getCardService().countPlayedTags(player, Set.of(Tag.ANIMAL, Tag.PLANT));

        player.setMcIncome(player.getMcIncome() + tagsPlayed);

        return null;
    }

    @Override
    public void revertPlayedTags(CardService cardService, Card card, Player player) {
        int animalPlantTagCount = cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.ANIMAL, Tag.PLANT));

        player.setMcIncome(player.getMcIncome() - animalPlantTagCount);
    }

    @Override
    public void revertCardIncome(MarsContext marsContext) {
        final Player player = marsContext.getPlayer();
        int tagCount = marsContext.getCardService().countPlayedTags(player, Set.of(Tag.ANIMAL, Tag.PLANT));
        player.setMcIncome(player.getMcIncome() - tagCount);
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 8;
    }
}
