package com.terraforming.ares.cards.buffedCorporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 27.04.2022
 */
@Getter
public class BuffedSaturnSystemsCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.JUPITER);
    }

    public BuffedSaturnSystemsCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Saturn Systems")
                .description("28 Mc. 1 Titanium income. 1 Space card. Whenever you play a Jupiter tag, excluding this, gain 1 TR.")
                .cardAction(CardAction.SATURN_SYSTEMS_CORPORATION)
                .build();
    }

    @Override
    public void postProjectBuiltEffect(CardService cardService, MarsGame game, Player player, Card project, Map<Integer, List<Integer>> inputParams) {
        if (project.getTags().contains(Tag.JUPITER)) {
            player.setTerraformingRating(player.getTerraformingRating() + 1);
        }
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }


    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(28);
        player.setTitaniumIncome(player.getTitaniumIncome() + 1);

        player.getHand().addCard(
                marsContext.getCardService().dealCardWithTag(Tag.SPACE, marsContext.getGame())
        );

        return null;
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BUFFED_CORPORATION;
    }

    @Override
    public int getPrice() {
        return 28;
    }

}
