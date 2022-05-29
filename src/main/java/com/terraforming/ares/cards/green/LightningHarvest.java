package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
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
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(CardService cardService, MarsGame game, Player player, Card project, Map<Integer, List<Integer>> inputParams) {
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

        int scienceTagsCount = (int) player.getPlayed().getCards().stream().map(marsContext.getCardService()::getCard)
                .flatMap(card -> card.getTags().stream())
                .filter(Tag.SCIENCE::equals)
                .count();

        player.setMcIncome(player.getMcIncome() + scienceTagsCount + 1);

        return null;
    }

    @Override
    public void revertPlayedTags(CardService cardService, List<Tag> tags, Player player) {
        int scienceTagCount = (int) tags.stream().map(Tag.SCIENCE::equals).count();
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
