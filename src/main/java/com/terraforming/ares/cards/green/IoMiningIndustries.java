package com.terraforming.ares.cards.green;

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
public class IoMiningIndustries implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setMcIncome(player.getMcIncome() + 2);
        player.setTitaniumIncome(player.getTitaniumIncome() + 2);

        return null;
    }

    @Override
    public String description() {
        //TODO
        return "1 VP per JPT you have. During the production phase, this produces 2 МС. When you play Space, you pay 6 MC less for it.";
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.SPACE, Tag.JUPITER);
    }

    @Override
    public int getPrice() {
        return 37;
    }
}
