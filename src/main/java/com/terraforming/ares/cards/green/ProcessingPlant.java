package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class ProcessingPlant implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ProcessingPlant(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Processing Plant")
                .description("Reveal cards from the top of the deck until you reveal a Building tag. Place it into your hand. Discard the rest. When you play a Building, you pay 4 MC less for it.")
                .incomes(List.of(Gain.of(GainType.STEEL, 2)))
                .cardAction(CardAction.PROCESSING_PLANT)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        CardService cardService = marsContext.getCardService();

        player.setSteelIncome(player.getSteelIncome() + 2);

        Card card;
        do {
            List<Integer> cards = cardService.dealCards(marsContext.getGame(), 1);
            card = cardService.getCard(cards.get(0));
        } while (!card.getTags().contains(Tag.BUILDING));

        player.getHand().addCard(card.getId());

        return null;
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 19;
    }
}
