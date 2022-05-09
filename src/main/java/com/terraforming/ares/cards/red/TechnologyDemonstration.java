package com.terraforming.ares.cards.red;

import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.TerraformingService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class TechnologyDemonstration implements BaseExpansionRedCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        TerraformingService terraformingService = marsContext.getTerraformingService();

        terraformingService.revealOcean(marsContext.getGame(), marsContext.getPlayer());

        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : marsContext.getGame().dealCards(2)) {
            marsContext.getPlayer().getHand().addCard(card);

            ProjectCard projectCard = marsContext.getCardService().getProjectCard(card);
            resultBuilder.takenCard(CardDto.from(projectCard));
        }

        return resultBuilder.build();
    }

    @Override
    public String description() {
        return "Flip an ocean tile. Draw 2 cards";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.SCIENCE, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 17;
    }

}
