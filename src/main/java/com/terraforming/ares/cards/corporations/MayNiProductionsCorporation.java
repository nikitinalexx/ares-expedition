package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 27.04.2022
 */
@Getter
public class MayNiProductionsCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public MayNiProductionsCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Teractor Corporation")
                .description("48 Mc. You may play a card from hand that costs 12 MC or less. Whenever you play a Green card, take a card and discard a card.")
                .cardAction(CardAction.MAY_NI_PRODUCTIONS_CORPORATION)
                .build();
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(48);

        player.getBuilds().add(new BuildDto(BuildType.GREEN_OR_BLUE, 12, 12));

        return null;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        if (project.getColor() == CardColor.GREEN) {
            final MarsGame game = marsContext.getGame();
            final Player player = marsContext.getPlayer();

            List<Integer> cards = marsContext.getCardService().dealCards(game, 1);
            player.getHand().addCards(cards);
            player.addFirstTurn(
                    new DiscardCardsTurn(
                            player.getUuid(),
                            List.of(),
                            1,
                            false,
                            true
                    )
            );
        }
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.EARTH);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public int getPrice() {
        return 48;
    }

}
