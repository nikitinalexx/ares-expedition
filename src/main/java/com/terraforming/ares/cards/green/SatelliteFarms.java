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
public class SatelliteFarms implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public SatelliteFarms(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Sattellite Farms")
                .description("During the production phase, this produces 1 heat per Space you have, including this.")
                .cardAction(CardAction.HEAT_SPACE_INCOME)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        int spaceTagCount = cardService.countPlayedTags(player, Set.of(Tag.SPACE));

        player.setHeat(player.getHeat() + spaceTagCount);
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        int spaceTags = marsContext.getCardService().countCardTags(project, Set.of(Tag.SPACE), inputParams);

        final Player player = marsContext.getPlayer();
        player.setHeatIncome(player.getHeatIncome() + spaceTags);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        int spaceTagCount = marsContext.getCardService().countPlayedTags(marsContext.getPlayer(), Set.of(Tag.SPACE));

        marsContext.getPlayer().setHeatIncome(marsContext.getPlayer().getHeatIncome() + spaceTagCount + 1);

        return null;
    }

    @Override
    public void revertPlayedTags(CardService cardService, Card card, Player player) {
        int spaceTagCount = cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.SPACE));

        player.setHeatIncome(player.getHeatIncome() - spaceTagCount);
    }

    @Override
    public void revertCardIncome(MarsContext marsContext) {
        final Player player = marsContext.getPlayer();
        int spaceTagCount = marsContext.getCardService().countPlayedTags(player, Set.of(Tag.SPACE));
        player.setHeatIncome(player.getHeatIncome() - spaceTagCount);
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
        return 17;
    }
}
