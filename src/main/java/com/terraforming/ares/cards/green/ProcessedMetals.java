package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class ProcessedMetals implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ProcessedMetals(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Processed Metals")
                .description("Draw a card for each Energy tag you have, including this. When you play a Space tag, you pay 6 MC less for it.")
                .incomes(List.of(Gain.of(GainType.TITANIUM, 2)))
                .cardAction(CardAction.PROCESSED_METALS)
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

        player.setTitaniumIncome(player.getTitaniumIncome() + 2);

        int energyTagsCount = 1 + cardService.countPlayedTags(player, Set.of(Tag.ENERGY));

        return marsContext.dealCards(energyTagsCount);
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 27;
    }
}
