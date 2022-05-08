package com.terraforming.ares.cards.green;

import com.terraforming.ares.model.SpecialEffect;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class AsteroidMiningConsortium implements BaseExpansionGreenCard {
    private final int id;

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.SPACE_DISCOUNT_3);
    }

    @Override
    public String description() {
        return "When you play a Space, you pay 3 MC less for it.";
    }

    @Override
    public int getPrice() {
        return 6;
    }
}
