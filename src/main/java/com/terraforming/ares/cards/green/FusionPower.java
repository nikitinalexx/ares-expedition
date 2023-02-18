package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 29.04.2022
 */
@RequiredArgsConstructor
@Getter
public class FusionPower implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public FusionPower(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Fusion Power")
                .description("Requires 2 Energy tags. During the production phase, draw a card.")
                .incomes(List.of(Gain.of(GainType.CARD, 1)))
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.getHand().addCards(cardService.dealCards(game, 1));
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setCardIncome(player.getCardIncome() + 1);
        return null;
    }

    @Override
    public List<Tag> getTagRequirements() {
        return List.of(Tag.ENERGY, Tag.ENERGY);
    }

    @Override
    public List<Tag> getTags() {
        return Arrays.asList(Tag.SCIENCE, Tag.BUILDING, Tag.ENERGY);
    }

    @Override
    public int getPrice() {
        return 7;
    }
}
