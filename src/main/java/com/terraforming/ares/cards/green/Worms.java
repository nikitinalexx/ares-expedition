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
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard project, Map<Integer, List<Integer>> inputParams) {
        int microbeTagCount = (int) project.getTags().stream().filter(Tag.MICROBE::equals).count();

        player.setPlantsIncome(player.getPlantsIncome() + microbeTagCount);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        int microbeTagCount = (int) marsContext.getPlayer()
                .getPlayed()
                .getCards().stream()
                .map(marsContext.getCardService()::getProjectCard)
                .flatMap(card -> card.getTags().stream())
                .filter(Tag.MICROBE::equals).count();

        marsContext.getPlayer().setPlantsIncome(marsContext.getPlayer().getPlantsIncome() + microbeTagCount + 1);

        return null;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public List<ParameterColor> getOxygenRequirement() {
        return List.of(ParameterColor.RED, ParameterColor.YELLOW, ParameterColor.WHITE);
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
