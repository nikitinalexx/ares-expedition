package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.MarsContext;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.TurnResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class WorkCrews implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public WorkCrews(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Work Crews")
                .description("You may play an additional blue or red card this phase. You pay 11 MC less for the next card you play this phase.")
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setBuiltWorkCrewsLastTurn(true);
        player.setCanBuildInSecondPhase(player.getCanBuildInSecondPhase() + 1);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 5;
    }

}
