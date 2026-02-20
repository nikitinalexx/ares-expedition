package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.buffedCorporations.BuffedArclightCorporation;
import com.terraforming.ares.cards.corporations.ArclightCorporation;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConservedBiomeAddWpAction extends BlueAction {
    public ConservedBiomeAddWpAction(int id) {
        super(id);
    }

    @Override
    public boolean canApplyInternal(StateContext stateContext, State state) {
        return hasAnimalForWp(stateContext.getCards());
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        addAnimalToState(stateContext.getCards(), state);
    }

    private void addAnimalToState(Map<Class<?>, Card> cards, State state) {
        if (cards.containsKey(Birds.class)) {
            state.birds++;
        } else if (cards.containsKey(Fish.class)) {
            state.fish++;
        } else if (cards.containsKey(Livestock.class)) {
            state.livestock++;
        } else if (cards.containsKey(Zoos.class)) {
            state.zoos++;
        } else if (cards.containsKey(ArclightCorporation.class) || cards.containsKey(BuffedArclightCorporation.class)) {
            state.arclight++;
        } else if (cards.containsKey(EcologicalZone.class)) {
            state.ecologicalZone++;
        } else if (cards.containsKey(Herbivores.class)) {
            state.herbivores++;
        } else if (cards.containsKey(SmallAnimals.class)) {
            state.smallAnimals++;
        } else if (cards.containsKey(FilterFeeders.class)) {
            state.filterFeeders++;
        }
    }

    private static final List<Set<Class<?>>> ANIMAL_VP_PRIORITY = List.of(
            Set.of(Birds.class, Fish.class, Livestock.class, Zoos.class), // 6 VP
            Set.of(ArclightCorporation.class, BuffedArclightCorporation.class, EcologicalZone.class, Herbivores.class, SmallAnimals.class), // 3 VP
            Set.of(FilterFeeders.class) // 2 VP
    );

    // Метод для получения класса лучшего животного
    private Class<?> getBestAnimalClass(Map<Class<?>, Card> cardClasses) {
        for (Set<Class<?>> group : ANIMAL_VP_PRIORITY) {
            for (Class<?> clazz : group) {
                if (cardClasses.containsKey(clazz)) {
                    return clazz;
                }
            }
        }
        return null;
    }

    private boolean hasAnimalForWp(Map<Class<?>, Card> cardClasses) {
        for (Set<Class<?>> group : ANIMAL_VP_PRIORITY) {
            // Если хотя бы одна карта из этой группы есть в мапе
            for (Class<?> targetClass : group) {
                if (cardClasses.containsKey(targetClass)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Object getContext(StateContext stateContext) {
        return getBestAnimalClass(stateContext.getCards());
    }
}
