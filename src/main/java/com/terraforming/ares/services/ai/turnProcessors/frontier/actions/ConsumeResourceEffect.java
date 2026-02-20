package com.terraforming.ares.services.ai.turnProcessors.frontier.actions;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.buffedCorporations.BuffedArclightCorporation;
import com.terraforming.ares.cards.corporations.ArclightCorporation;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.StateContext;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public enum ConsumeResourceEffect implements StateEffect {
    GHG(s -> s.ghgBacteriaCount--, List.of(GhgProductionBacteria.class, BuffedGhgProductionBacteria.class), s -> s.ghgBacteriaCount > 0),
    NITRITE(s -> s.nitriteReductingBacteria--, List.of(NitriteReductingBacteria.class), s -> s.nitriteReductingBacteria > 0),
    REGOLITH(s -> s.regolithEaters--, List.of(RegolithEaters.class, BuffedRegolithEaters.class), s -> s.regolithEaters > 0),
    SELF_REPLICATING(s -> s.selfReplicatingBacteria--, List.of(SelfReplicatingBacteria.class), s -> s.selfReplicatingBacteria > 0),
    ANAEROBIC(s -> s.anaerobicMicroorganisms--, List.of(AnaerobicMicroorganisms.class), s -> s.anaerobicMicroorganisms > 0),
    AGGREGATES(s -> s.bacterialAggregates--, List.of(BacterialAggregates.class), s -> s.bacterialAggregates > 0),
    DECOMPOSERS(s -> s.decomposers--, List.of(Decomposers.class), s -> s.decomposers > 0),
    DECOMPOSING_FUNGUS(s -> s.decomposingFungus--, List.of(DecomposingFungus.class), s -> s.decomposingFungus > 0),
    FILTER_FEEDERS(s -> s.filterFeeders--, List.of(FilterFeeders.class), s -> s.filterFeeders > 0),
    TARDIGRADES(s -> s.tardigrades--, List.of(Tardigrades.class), s -> s.tardigrades > 0),
    ARCLIGHT(s -> s.arclight--, List.of(ArclightCorporation.class, BuffedArclightCorporation.class), s -> s.arclight > 0),
    ECOLOGICAL_ZONE(s -> s.ecologicalZone--, List.of(EcologicalZone.class), s -> s.ecologicalZone > 0),
    HERBIVORES(s -> s.herbivores--, List.of(Herbivores.class), s -> s.herbivores > 0),
    SMALL_ANIMALS(s -> s.smallAnimals--, List.of(SmallAnimals.class), s -> s.smallAnimals > 0),
    ;

    private final Consumer<State> applyFn;
    @Getter
    private final List<Class<? extends Card>> targetClasses;
    private final Predicate<State> isApplicable;

    ConsumeResourceEffect(Consumer<State> applyFn, List<Class<? extends Card>> targetClasses, Predicate<State> isApplicable) {
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
        return isApplicable.test(state);
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

