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
public class Windmills implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Windmills(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Windmills")
                .description("During the production phase, this produces 1 heat per Energy tag you have.")
                .cardAction(CardAction.HEAT_ENERGY_INCOME)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        int energyTagCount = cardService.countPlayedTags(player, Set.of(Tag.ENERGY));

        player.setHeat(player.getHeat() + energyTagCount);
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        int energyTagsCount = marsContext.getCardService().countCardTags(project, Set.of(Tag.ENERGY), inputParams);

        final Player player = marsContext.getPlayer();

        player.setHeatIncome(player.getHeatIncome() + energyTagsCount);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        final Player player = marsContext.getPlayer();

        int energyTagCount = marsContext.getCardService().countPlayedTags(player, Set.of(Tag.ENERGY));

        player.setHeatIncome(player.getHeatIncome() + energyTagCount + 1);

        return null;
    }

    @Override
    public void revertPlayedTags(CardService cardService, Card card, Player player) {
        int energyTagCount = cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.ENERGY));

        player.setHeatIncome(player.getHeatIncome() - energyTagCount);
    }

    @Override
    public void revertCardIncome(MarsContext marsContext) {
        final Player player = marsContext.getPlayer();
        int energyTagCount = marsContext.getCardService().countPlayedTags(player, Set.of(Tag.ENERGY));
        player.setHeatIncome(player.getHeatIncome() - energyTagCount);
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
        return List.of(Tag.BUILDING, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 10;
    }
}
