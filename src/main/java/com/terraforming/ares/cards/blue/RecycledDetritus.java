package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 07.05.2022
 */
@RequiredArgsConstructor
@Getter
public class RecycledDetritus implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public RecycledDetritus(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Recycled Detritus")
                .description("When you play an Event, draw 2 cards.")
                .cardAction(CardAction.RECYCLED_DETRITUS)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        int eventTagCount = marsContext.getCardService().countCardTags(project, Set.of(Tag.EVENT), inputParams);

        final MarsGame game = marsContext.getGame();
        final Player player = marsContext.getPlayer();

        if (eventTagCount > 0) {
            for (Integer card : marsContext.getCardService().dealCards(game, 2)) {
                player.getHand().addCard(card);
            }
        }
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public int getPrice() {
        return 24;
    }
}
