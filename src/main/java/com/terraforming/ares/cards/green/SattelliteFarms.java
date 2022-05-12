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
public class SattelliteFarms implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public SattelliteFarms(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Sattellite Farms")
                .description("During the production phase, this produces 1 heat per Space you have, including this.")
                .cardAction(CardAction.HEAT_SPACE_INCOME)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard project, Map<Integer, List<Integer>> inputParams) {
        int spaceTags = (int) project.getTags().stream().filter(Tag.SPACE::equals).count();

        player.setHeatIncome(player.getHeatIncome() + spaceTags);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        int spaceTags = (int) marsContext.getPlayer()
                .getPlayed()
                .getCards().stream()
                .map(marsContext.getCardService()::getProjectCard)
                .flatMap(card -> card.getTags().stream())
                .filter(Tag.SPACE::equals).count();

        marsContext.getPlayer().setHeatIncome(marsContext.getPlayer().getHeatIncome() + spaceTags + 1);

        return null;
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
