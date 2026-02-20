package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;


public enum PutMicrobeEffect implements StateEffect {

    GHG(s -> s.ghgBacteriaCount++, List.of(GhgProductionBacteria.class, BuffedGhgProductionBacteria.class)),
    NITRITE(s -> s.nitriteReductingBacteria++, List.of(NitriteReductingBacteria.class)),
    REGOLITH(s -> s.regolithEaters++, List.of(RegolithEaters.class, BuffedRegolithEaters.class)),
    SELF_REPLICATING(s -> s.selfReplicatingBacteria++, List.of(SelfReplicatingBacteria.class)),
    ANAEROBIC(s -> s.anaerobicMicroorganisms++, List.of(AnaerobicMicroorganisms.class)),
    AGGREGATES(s -> s.bacterialAggregates++, List.of(BacterialAggregates.class),
            sm -> sm.getPlayer().getCardResourcesCount().getOrDefault(BacterialAggregates.class, 0) < 5),
    DECOMPOSERS(s -> s.decomposers++, List.of(Decomposers.class)),
    DECOMPOSING_FUNGUS(s -> s.decomposingFungus++, List.of(DecomposingFungus.class)),
    TARDIGRADES(s -> s.tardigrades++, List.of(Tardigrades.class));

    private final Consumer<State> applyFn;
    @Getter
    private final List<Class<? extends Card>> targetClasses;
    private final Function<StateContext, Boolean> isApplicable;

    PutMicrobeEffect(Consumer<State> applyFn, List<Class<? extends Card>> targetClasses) {
        this(applyFn, targetClasses, (sm) -> true);
    }

    PutMicrobeEffect(Consumer<State> applyFn, List<Class<? extends Card>> targetClasses, Function<StateContext, Boolean> isApplicable) {
        this.applyFn = applyFn;
        this.targetClasses = targetClasses;
        this.isApplicable = isApplicable;
    }

    @Override
    public void apply(State state) {
        applyFn.accept(state);
    }

    @Override
    public boolean isApplicable(StateContext stateContext, State state) {
        return isApplicable.apply(stateContext);
    }

    @Override
    public boolean shouldTraverseSpecialEffect(StateContext stateContext) {
        Map<Class<?>, Integer> resourceMap = stateContext.getPlayer().getCardResourcesCount();
        for (Class<? extends Card> clazz : targetClasses) {
            if (resourceMap.containsKey(clazz)) {
                return true;
            }
        }
        return false;
    }
}

