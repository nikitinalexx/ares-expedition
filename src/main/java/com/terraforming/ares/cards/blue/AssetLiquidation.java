package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@RequiredArgsConstructor
@Getter
public class AssetLiquidation implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public AssetLiquidation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Asset Liquidation")
                .description("You may play an additional blue or red card this phase. Action: Spend 1 TR to draw 3 cards.")
                .cardAction(CardAction.ASSET_LIQUIDATION)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setActionsInSecondPhase(player.getActionsInSecondPhase() + 1);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.SCIENCE);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public int getPrice() {
        return 0;
    }
}
