package com.terraforming.ares.cards.red;

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
    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setBuiltWorkCrewsLastTurn(true);
        player.setCanBuildInSecondPhase(player.getCanBuildInSecondPhase() + 1);
        return null;
    }

    private final int id;

    @Override
    public String description() {
        return "You may play an additional blue or red card this phase. You pay 11 MC less for the next card you play this phase.";
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
