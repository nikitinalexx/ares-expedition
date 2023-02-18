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
        int plantTagCount = (int) player
                .getPlayed()
                .getCards().stream()
                .map(cardService::getCard)
                .flatMap(card -> card.getTags().stream())
                .filter(Tag.PLANT::equals).count();

        player.setPlants(player.getPlants() + plantTagCount);
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(CardService cardService, MarsGame game, Player player, Card project, Map<Integer, List<Integer>> inputParams) {
        int plantTagsCount = (int) project.getTags().stream().filter(Tag.PLANT::equals).count();

        player.setPlantsIncome(player.getPlantsIncome() + plantTagsCount);
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        int plantTagsCount = (int) player.getPlayed().getCards().stream().map(marsContext.getCardService()::getCard)
                .flatMap(card -> card.getTags().stream())
                .filter(Tag.PLANT::equals)
                .count();

        player.setPlantsIncome(player.getPlantsIncome() + plantTagsCount);


        return null;
    }

    @Override
    public void revertPlayedTags(CardService cardService, List<Tag> tags, Player player) {
        int plantTagCount = (int) tags.stream().filter(Tag.PLANT::equals).count();
        player.setPlantsIncome(player.getPlantsIncome() - plantTagCount);
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
