package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.dto.ProjectCardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.ProjectCard;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.turn.DiscardCardsTurn;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class BusinessContracts implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public BusinessContracts(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Business Contracts")
                .description("Draw 4 cards. Then discard 2 cards.")
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : marsContext.getGame().dealCards(4)) {
            marsContext.getPlayer().getHand().addCard(card);

            ProjectCard projectCard = marsContext.getCardService().getProjectCard(card);
            resultBuilder.takenCard(ProjectCardDto.from(projectCard));
        }

        marsContext.getPlayer().setNextTurn(
                new DiscardCardsTurn(
                        marsContext.getPlayer().getUuid(),
                        List.of(),
                        2,
                        false
                )
        );

        return resultBuilder.build();
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EARTH, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 5;
    }

}
