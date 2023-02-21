package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.DiscoveryExpansionBlueCard;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 21.02.2023
 */
@RequiredArgsConstructor
@Getter
public class ResearchGrant implements DiscoveryExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ResearchGrant(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Research Grant")
                .actionDescription("Action: Place a tag not already present on this card and add to this card (Max 3). This counts as playing that tag.")
                .description("You may play an additional blue or red card this phase.")
                .cardAction(CardAction.RESEARCH_GRANT)
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
        player.getCardToTag().put(this.getClass(), new ArrayList<>(List.of(Tag.DYNAMIC, Tag.DYNAMIC, Tag.DYNAMIC)));
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.DYNAMIC, Tag.DYNAMIC, Tag.DYNAMIC);
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public int getPrice() {
        return 3;
    }
}
