package com.terraforming.ares.cards.blue;

import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.PlayerContext;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.model.Tag;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
public class AdvancedAlloys implements BlueCard {
    @Override
    public void buildProject(PlayerContext playerContext) {
        //nothing
    }

    @Override
    public String description() {
        return "Each steel and titanium you have is valued 1 MC more.";
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.BUILDING);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Collections.singleton(SpecialEffect.ADVANCED_ALLOYS);
    }

    @Override
    public int getPrice() {
        return 9;
    }

}
