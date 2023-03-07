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
public class MirandaResort implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public MirandaResort(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Miranda Resort")
                .description("During the production phase, this produces 1 MC per Earth tag you have, including this.")
                .cardAction(CardAction.MC_EARTH_INCOME)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        int earthTagCount = cardService.countPlayedTags(player, Set.of(Tag.EARTH));

        player.setMc(player.getMc() + earthTagCount);
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        final Player player = marsContext.getPlayer();

        int earthTags = marsContext.getCardService().countCardTags(project, Set.of(Tag.EARTH), inputParams);

        player.setMcIncome(player.getMcIncome() + earthTags);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        final Player player = marsContext.getPlayer();

        int earthTagCount = marsContext.getCardService().countPlayedTags(player, Set.of(Tag.EARTH));

        player.setMcIncome(player.getMcIncome() + earthTagCount + 1);

        return null;
    }

    @Override
    public void revertPlayedTags(CardService cardService, Card card, Player player) {
        int earthTagCount = cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.EARTH));

        player.setMcIncome(player.getMcIncome() - earthTagCount);
    }

    @Override
    public void revertCardIncome(MarsContext marsContext) {
        final Player player = marsContext.getPlayer();
        int earthTagCount = marsContext.getCardService().countPlayedTags(player, Set.of(Tag.EARTH));
        player.setMcIncome(player.getMcIncome() - earthTagCount);
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.EARTH, Tag.JUPITER);
    }

    @Override
    public int getPrice() {
        return 15;
    }
}
