package com.terraforming.ares.services.ai.turnProcessors.frontier;

import java.util.Objects;

public class State {
    public int mc;
    public int heat;
    public int plants;
    public int cards;
    public int tr;
    public int wp;//1 real win point = 6 wp
    public int sacrificialWp;
    public double extraMcValue;

    //not displaying the collectable cards with win points only
    public int ghgBacteriaCount;
    public int nitriteReductingBacteria;
    public int regolithEaters;
    public int selfReplicatingBacteria;

    public int anaerobicMicroorganisms;
    public int bacterialAggregates;
    public int decomposers;
    public int decomposingFungus;

    public int extraForests;

    public boolean trRaisedThisPhase;
    public boolean unmiUsedThisPhase;

    public boolean heatAsMc;

    public double scoreCache;

    public State copy() {
        State s = new State();
        s.mc = mc;
        s.heat = heat;
        s.plants = plants;
        s.cards = cards;
        s.tr = tr;
        s.wp = wp;
        s.sacrificialWp = sacrificialWp;
        s.ghgBacteriaCount = ghgBacteriaCount;
        s.nitriteReductingBacteria = nitriteReductingBacteria;
        s.regolithEaters = regolithEaters;
        s.selfReplicatingBacteria = selfReplicatingBacteria;
        s.anaerobicMicroorganisms = anaerobicMicroorganisms;
        s.bacterialAggregates = bacterialAggregates;
        s.decomposers = decomposers;
        s.decomposingFungus = decomposingFungus;
        s.extraMcValue = extraMcValue;
        s.trRaisedThisPhase = trRaisedThisPhase;
        s.unmiUsedThisPhase = unmiUsedThisPhase;
        s.heatAsMc = heatAsMc;
        return s;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return mc == state.mc && heat == state.heat && plants == state.plants && cards == state.cards && tr == state.tr && wp == state.wp && sacrificialWp == state.sacrificialWp && ghgBacteriaCount == state.ghgBacteriaCount && nitriteReductingBacteria == state.nitriteReductingBacteria && regolithEaters == state.regolithEaters && selfReplicatingBacteria == state.selfReplicatingBacteria && anaerobicMicroorganisms == state.anaerobicMicroorganisms && bacterialAggregates == state.bacterialAggregates && decomposers == state.decomposers && decomposingFungus == state.decomposingFungus && extraForests == state.extraForests && extraMcValue == state.extraMcValue && trRaisedThisPhase == state.trRaisedThisPhase && unmiUsedThisPhase == state.unmiUsedThisPhase && heatAsMc == state.heatAsMc;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mc, heat, plants, cards, tr, wp, sacrificialWp, ghgBacteriaCount, nitriteReductingBacteria, regolithEaters, selfReplicatingBacteria, anaerobicMicroorganisms, bacterialAggregates, decomposers, decomposingFungus, extraForests, extraMcValue, trRaisedThisPhase, unmiUsedThisPhase, heatAsMc);
    }
}
