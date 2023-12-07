package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class Advertising implements ExperimentExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Advertising(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Advertising")
                .description("When you play a card with printed cost 20+, get 1 MC production.")
                .cardAction(CardAction.ADVERTISING)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        if (project.getPrice() >= 20) {
            Player player = marsContext.getPlayer();
            player.setMcIncome(player.getMcIncome() + 1);
        }
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 4;
    }
}
