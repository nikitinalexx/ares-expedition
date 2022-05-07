package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.model.CorporationCard;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 27.04.2022
 */
@RequiredArgsConstructor
@Getter
public class HelionCorporation implements CorporationCard {
    private final int id;

    @Override
    public void buildProject(Player player) {
        player.setMc(28);
        player.setHeatIncome(3);
    }

    @Override
    public String description() {
        return "MegaCredits: 28. HeatIncome: 3. You may use heat as MC. You may not use MC as heat.";
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.SPACE);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

}
