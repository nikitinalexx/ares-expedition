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
public class LightningHarvest implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public LightningHarvest(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Lightning Harvest")
                .description("During the production phase, this produces 1 MC per Science tag you have, including this.")
                .cardAction(CardAction.MC_SCIENCE_INCOME)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        int scienceTagCount = cardService.countPlayedTags(player, Set.of(Tag.SCIENCE));

        player.setMc(player.getMc() + scienceTagCount);
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
        int scienceTagsCount = marsContext.getCardService().countCardTags(project, Set.of(Tag.SCIENCE), inputParams);

        final Player player = marsContext.getPlayer();

        player.setMcIncome(player.getMcIncome() + scienceTagsCount);
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        int scienceTagsCount = marsContext.getCardService().countPlayedTags(marsContext.getPlayer(), Set.of(Tag.SCIENCE));

        player.setMcIncome(player.getMcIncome() + scienceTagsCount + 1);

        return null;
    }

    @Override
    public void revertPlayedTags(CardService cardService, Card card, Player player) {
        int scienceTagCount = cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.SCIENCE));

        player.setMcIncome(player.getMcIncome() - scienceTagCount);
    }

    @Override
    public void revertCardIncome(MarsContext marsContext) {
        final Player player = marsContext.getPlayer();
        int scienceTagCount = marsContext.getCardService().countPlayedTags(player, Set.of(Tag.SCIENCE));
        player.setMcIncome(player.getMcIncome() - scienceTagCount);
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
