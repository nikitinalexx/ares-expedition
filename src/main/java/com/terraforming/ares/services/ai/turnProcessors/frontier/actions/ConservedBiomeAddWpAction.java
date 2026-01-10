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
        return bestVpFromAnimal(stateContext.getCards()) > 0;
    }

    @Override
    public void applyInternal(StateContext stateContext, State state) {
        int wp = bestVpFromAnimal(stateContext.getCards());
        if (wp == 2) {
            state.sacrificialWp += wp;
        } else {
            state.wp += wp;
        }

    }

    private static final List<Set<Class<?>>> ANIMAL_VP_PRIORITY = List.of(
            Set.of(Birds.class, Fish.class, Livestock.class, Zoos.class), // 6 VP
            Set.of(ArclightCorporation.class, BuffedArclightCorporation.class, EcologicalZone.class, Herbivores.class, SmallAnimals.class), // 3 VP
            Set.of(FilterFeeders.class) // 2 VP
    );

    private int getVpByPriorityIndex(int index) {
        return switch (index) {
            case 0 -> 6;
            case 1 -> 3;
            case 2 -> 2;
            default -> 0;
        };
    }

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

    // Метод для получения значения VP
    private int bestVpFromAnimal(Map<Class<?>, Card> cardClasses) {
        for (int i = 0; i < ANIMAL_VP_PRIORITY.size(); i++) {
            Set<Class<?>> group = ANIMAL_VP_PRIORITY.get(i);
            // Если хотя бы одна карта из этой группы есть в мапе
            if (group.stream().anyMatch(cardClasses::containsKey)) {
                return getVpByPriorityIndex(i);
            }
        }
        return 0;
    }

    @Override
    public Object getContext(StateContext stateContext) {
        return getBestAnimalClass(stateContext.getCards());
    }
}
