package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.parameters.ParameterColor;
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
public class Worms implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Worms(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Worms")
                .description("Requires red oxygen or higher. During the production phase, this produces 1 plant per Microbe tag you have, including this.")
                .cardAction(CardAction.PLANT_MICROBE_INCOME)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        int microbeTagCount = cardService.countPlayedTags(player, Set.of(Tag.MICROBE));

        player.setPlants(player.getPlants() + microbeTagCount);
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
        int microbeTagCount = marsContext.getCardService().countCardTags(project, Set.of(Tag.MICROBE), inputParams);

        final Player player = marsContext.getPlayer();
        player.setPlantsIncome(player.getPlantsIncome() + microbeTagCount);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        int microbeTagCount = marsContext.getCardService().countPlayedTags(marsContext.getPlayer(), Set.of(Tag.MICROBE));

        marsContext.getPlayer().setPlantsIncome(marsContext.getPlayer().getPlantsIncome() + microbeTagCount + 1);

        return null;
    }

    @Override
    public void revertPlayedTags(CardService cardService, Card card, Player player) {
        int microbeTagCount = cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.MICROBE));

        player.setPlantsIncome(player.getPlantsIncome() - microbeTagCount);
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public List<ParameterColor> getOxygenRequirement() {
        return List.of(ParameterColor.R, ParameterColor.Y, ParameterColor.W);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.MICROBE);
    }

    @Override
    public int getPrice() {
        return 11;
    }
}
