package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
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
    private final CardMetadata cardMetadata;

    public TechnologyDemonstration(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Technology Demonstration")
                .description("Flip an ocean tile. Draw 2 cards")
                .bonuses(List.of(Gain.of(GainType.OCEAN, 1), Gain.of(GainType.CARD, 2)))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        TerraformingService terraformingService = marsContext.getTerraformingService();

        terraformingService.revealOcean(marsContext);

        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : marsContext.getCardService().dealCards(marsContext.getGame(), 2)) {
            marsContext.getPlayer().getHand().addCard(card);

            Card projectCard = marsContext.getCardService().getCard(card);
            resultBuilder.takenCard(CardDto.from(projectCard));
        }

        return resultBuilder.build();
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
