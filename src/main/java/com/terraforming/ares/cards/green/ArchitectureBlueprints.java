package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 04.12.2023
 */
@RequiredArgsConstructor
@Getter
public class ArchitectureBlueprints implements InfrastructureExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ArchitectureBlueprints(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Architecture Blueprints")
                .description("Raise infrastructure 1 step. Draw 2 cards. Then, discard a card")
                .bonuses(List.of(Gain.of(GainType.INFRASTRUCTURE, 1)))
                .cardAction(CardAction.CAPITALISE_DESCRIPTION)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getTerraformingService().increaseInfrastructure(marsContext);

        for (Integer card : marsContext.getCardService().dealCards(marsContext.getGame(), 2)) {
            marsContext.getPlayer().getHand().addCard(card);
        }

        marsContext.getPlayer().addNextTurn(
                new DiscardCardsTurn(
                        marsContext.getPlayer().getUuid(),
                        List.of(),
                        1,
                        false,
                        true,
                        List.of()
                )
        );

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 14;
    }

}
