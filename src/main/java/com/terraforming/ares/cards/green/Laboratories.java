package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class Laboratories implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Laboratories(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Laboratories")
                .description("During the production phase, draw a card for every 3 Science tags you have, including this.")
                .cardAction(CardAction.CARD_SCIENCE_INCOME)
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        int scienceTagCount = cardService.countPlayedTags(player, Set.of(Tag.SCIENCE));

        if (scienceTagCount >= 3) {
            player.getHand().addCards(cardService.dealCards(game, scienceTagCount / 3));
        }
    }

    @Override
    public boolean canPayAgain() {
        return true;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        int projectTagsCount = marsContext.getCardService().countCardTags(project, Set.of(Tag.SCIENCE), inputParams);

        final Player player = marsContext.getPlayer();

        int tagsPlayedBefore = marsContext.getCardService().countPlayedTags(player, Set.of(Tag.SCIENCE));
        int tagsPlayedAfter = tagsPlayedBefore + projectTagsCount;

        int incomeBefore = tagsPlayedBefore / 3;
        int incomeAfter = tagsPlayedAfter / 3;

        if (incomeAfter > incomeBefore) {
            player.setCardIncome(player.getCardIncome() + (incomeAfter - incomeBefore));
        }
    }

    @Override
    public void revertPlayedTags(CardService cardService, Card card, Player player) {
        int scienceTagCount = cardService.countCardTagsWithDynamic(card, player, Set.of(Tag.SCIENCE));

        int tagsPlayedBefore = cardService.countPlayedTags(player, Set.of(Tag.SCIENCE));
        int tagsPlayedAfter = tagsPlayedBefore - scienceTagCount;

        int incomeBefore = tagsPlayedBefore / 3;
        int incomeAfter = tagsPlayedAfter / 3;

        if (incomeAfter < incomeBefore) {
            player.setCardIncome(player.getCardIncome() - (incomeBefore - incomeAfter));
        }
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        int tagsPlayed = 1 + marsContext.getCardService().countPlayedTags(player, Set.of(Tag.SCIENCE));

        if (tagsPlayed >= 3) {
            player.setCardIncome(player.getCardIncome() + (tagsPlayed / 3));
        }

        return null;
    }

    @Override
    public void revertCardIncome(MarsContext marsContext) {
        final Player player = marsContext.getPlayer();
        int scienceTagCount = marsContext.getCardService().countPlayedTags(player, Set.of(Tag.SCIENCE));
        player.setCardIncome(player.getCardIncome() - scienceTagCount / 3);
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE);
    }

    @Override
    public int getPrice() {
        return 8;
    }
}
