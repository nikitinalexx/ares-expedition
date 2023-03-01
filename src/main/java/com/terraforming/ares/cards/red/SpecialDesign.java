package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
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
public class SpecialDesign implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public SpecialDesign(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Special Design")
                //TODO reflect in ui
                .description("You may play an additional blue or red card this phase. For the next card you play this phase, you may consider the oxygen or temperature one color higher or lower.")
                .cardAction(CardAction.SPECIAL_DESIGN)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setBuiltSpecialDesignLastTurn(true);
        marsContext.getBuildService().addExtraActionFromSecondPhase(player);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 3;
    }

}
