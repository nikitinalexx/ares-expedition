package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.ExperimentExpansionRedCard;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.parameters.ParameterColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class MartianSurvey implements ExperimentExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public MartianSurvey(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Martian Survey")
                .description("Draw 2 cards.")
                .bonuses(List.of(Gain.of(GainType.CARD, 2)))
                .cardAction(CardAction.CAPITALISE_DESCRIPTION)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : marsContext.getCardService().dealCards(marsContext.getGame(), 2)) {
            marsContext.getPlayer().getHand().addCard(card);
            resultBuilder.takenCard(CardDto.from(marsContext.getCardService().getCard(card)));
        }

        return resultBuilder.build();
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.EVENT);
    }

    @Override
    public List<ParameterColor> getOxygenRequirement() {
        return List.of(ParameterColor.P);
    }

    @Override
    public int getPrice() {
        return 9;
    }

}
