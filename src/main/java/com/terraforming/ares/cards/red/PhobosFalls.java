package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
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
public class PhobosFalls implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public PhobosFalls(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Phobos Falls")
                .description("Raise the temperature 1 step. Flip an ocean tile. Draw two cards.")
                .bonuses(List.of(
                        Gain.of(GainType.TEMPERATURE, 1),
                        Gain.of(GainType.OCEAN, 1),
                        Gain.of(GainType.CARD, 2)
                ))
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        TerraformingService terraformingService = marsContext.getTerraformingService();

        terraformingService.increaseTemperature(marsContext.getGame(), marsContext.getPlayer());
        terraformingService.revealOcean(marsContext.getGame(), marsContext.getPlayer());

        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : marsContext.getGame().dealCards(2)) {
            marsContext.getPlayer().getHand().addCard(card);
            resultBuilder.takenCard(CardDto.from(marsContext.getCardService().getProjectCard(card)));
        }

        return resultBuilder.build();
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 32;
    }

}
