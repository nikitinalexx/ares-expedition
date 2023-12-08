package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.ExperimentExpansionGreenCard;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class FlatMarsTheory implements ExperimentExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public FlatMarsTheory(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Flat Mars Theory")
                .description("Requires max 1 science. Increase your ME production 1 step for every previous generation(turn).")
                .cardAction(CardAction.MC_TURN_INCOME)
                .build();
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        int turns = marsContext.getGame().getTurns();

        Player player = marsContext.getPlayer();

        player.setMcIncome(player.getMcIncome() + turns - 1);

        return null;
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EARTH);
    }

    @Override
    public int getPrice() {
        return 8;
    }

}
