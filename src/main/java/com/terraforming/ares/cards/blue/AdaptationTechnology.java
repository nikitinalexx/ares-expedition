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
public class AdaptationTechnology implements BlueCard {
    @Override
    public void buildProject(PlayerContext playerContext) {
        //no effect
    }

    @Override
    public String description() {
        return "You may consider the oxygen or temperature requirements one color higher or lower. This effect doesn't sum up with other similar effects.";
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.SCIENCE);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public int getPrice() {
        return 12;
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Collections.singleton(SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT);
    }
}
