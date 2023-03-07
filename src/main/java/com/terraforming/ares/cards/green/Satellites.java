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
public class Satellites implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Satellites(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Satellites")
                .description("During the production phase, this produces 1 MC per Space you have, including this.")
                .cardAction(CardAction.MC_SPACE_INCOME)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        int spaceTagCount = cardService.countPlayedTags(player, Set.of(Tag.SPACE));

        player.setMc(player.getMc() + spaceTagCount);
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        int spaceTags = marsContext.getCardService().countCardTags(project, Set.of(Tag.SPACE), inputParams);

        final Player player = marsContext.getPlayer();
        player.setMcIncome(player.getMcIncome() + spaceTags);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        int spaceTags = marsContext.getCardService().countPlayedTags(marsContext.getPlayer(), Set.of(Tag.SPACE));

        marsContext.getPlayer().setMcIncome(marsContext.getPlayer().getMcIncome() + spaceTags + 1);

        return null;
    }

    @Override
    public void revertPlayedTags(CardService cardService, Card card, Player player) {
        int spaceTagCount = cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.SPACE));

        player.setMcIncome(player.getMcIncome() - spaceTagCount);
    }

    @Override
    public void revertCardIncome(MarsContext marsContext) {
        final Player player = marsContext.getPlayer();
        int spaceTagCount = marsContext.getCardService().countPlayedTags(player, Set.of(Tag.SPACE));
        player.setMcIncome(player.getMcIncome() - spaceTagCount);
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE);
    }

    @Override
    public int getPrice() {
        return 14;
    }
}
