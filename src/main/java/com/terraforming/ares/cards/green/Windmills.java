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
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(CardService cardService, MarsGame game, Player player, Card project, Map<Integer, List<Integer>> inputParams) {
        int energyTagsCount = (int) project.getTags().stream().filter(Tag.ENERGY::equals).count();

        player.setHeatIncome(player.getHeatIncome() + energyTagsCount);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        int energyTagsCount = (int) marsContext.getPlayer()
                .getPlayed()
                .getCards().stream()
                .map(marsContext.getCardService()::getCard)
                .flatMap(card -> card.getTags().stream())
                .filter(Tag.ENERGY::equals).count();

        marsContext.getPlayer().setHeatIncome(marsContext.getPlayer().getHeatIncome() + energyTagsCount + 1);

        return null;
    }

    @Override
    public void revertPlayedTags(CardService cardService, List<Tag> tags, Player player) {
        int energyTagCount = (int) tags.stream().map(Tag.ENERGY::equals).count();
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
