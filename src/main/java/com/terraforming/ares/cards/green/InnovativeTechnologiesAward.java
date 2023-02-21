package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 21.02.2023
 */
@RequiredArgsConstructor
@Getter
public class InnovativeTechnologiesAward implements DiscoveryExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public InnovativeTechnologiesAward(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Innovative Technologies Award")
                .description("Raise your TR 1 step for each upgraded phase card you have.")
                .cardAction(CardAction.INNOVATIVE_TECHNOLOGIES)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        final Player player = marsContext.getPlayer();

        int upgradedPhaseCards = (int) player.getPhaseCards().stream().filter(phaseCard -> phaseCard != 0).count();

        player.setTerraformingRating(player.getTerraformingRating() + upgradedPhaseCards);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 16;
    }

}
