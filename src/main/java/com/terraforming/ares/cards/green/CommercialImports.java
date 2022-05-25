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
public class CommercialImports implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public CommercialImports(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Commercial Imports")
                .description("During the production phase, this produces 1 Heat per Energy tag you have.")
                .cardAction(CardAction.HEAT_ENERGY_INCOME)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, Card project, Map<Integer, List<Integer>> inputParams) {
        int energyTags = (int) project.getTags().stream().filter(Tag.ENERGY::equals).count();

        player.setHeatIncome(player.getHeatIncome() + energyTags);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        int energyTags = (int) marsContext.getPlayer()
                .getPlayed()
                .getCards().stream()
                .map(marsContext.getCardService()::getCard)
                .flatMap(card -> card.getTags().stream())
                .filter(Tag.ENERGY::equals).count();

        marsContext.getPlayer().setHeatIncome(marsContext.getPlayer().getHeatIncome() + energyTags);

        return null;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 36;
    }
}
