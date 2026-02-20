package com.terraforming.ares.services.ai.turnProcessors.frontier;

import java.util.Objects;

public class State {
    public short mc;
    public short heat;
    public short plants;
    public short cards;
    public short tr;
    public double extraMcValue;

    //not displaying the collectable cards with win points only
    public short ghgBacteriaCount;
    public short nitriteReductingBacteria;
    public short regolithEaters;
    public short selfReplicatingBacteria;

    public short anaerobicMicroorganisms;
    public short bacterialAggregates;
    public short decomposers;
    public short decomposingFungus;

    public short birds;
    public short filterFeeders;
    public short fish;
    public short livestock;
    public short zoos;
    public short ecologicalZone;
    public short herbivores;
    public short smallAnimals;
    public short tardigrades;
    public short arclight;
    public short physixComplex;
    public short fibrousComposite;


    public short extraForests;

    public boolean trRaisedThisPhase;
    public boolean unmiUsedThisPhase;

    public boolean heatAsMc;

    public float scoreCache;

    public State copy() {
        State s = new State();
        s.mc = mc;
        s.heat = heat;
        s.plants = plants;
        s.cards = cards;
        s.tr = tr;
        s.extraMcValue = extraMcValue;

        s.ghgBacteriaCount = ghgBacteriaCount;
        s.nitriteReductingBacteria = nitriteReductingBacteria;
        s.regolithEaters = regolithEaters;
        s.selfReplicatingBacteria = selfReplicatingBacteria;

        s.anaerobicMicroorganisms = anaerobicMicroorganisms;
        s.bacterialAggregates = bacterialAggregates;
        s.decomposers = decomposers;
        s.decomposingFungus = decomposingFungus;

        s.birds = birds;
        s.filterFeeders = filterFeeders;
        s.fish = fish;
        s.livestock = livestock;
        s.zoos = zoos;
        s.ecologicalZone = ecologicalZone;
        s.herbivores = herbivores;
        s.smallAnimals = smallAnimals;
        s.tardigrades = tardigrades;
        s.arclight = arclight;
        s.physixComplex = physixComplex;
        s.fibrousComposite = fibrousComposite;

        s.extraForests = extraForests;

        s.trRaisedThisPhase = trRaisedThisPhase;
        s.unmiUsedThisPhase = unmiUsedThisPhase;
        s.heatAsMc = heatAsMc;
        return s;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return mc == state.mc && heat == state.heat && plants == state.plants && cards == state.cards && tr == state.tr && Double.compare(extraMcValue, state.extraMcValue) == 0 && ghgBacteriaCount == state.ghgBacteriaCount && nitriteReductingBacteria == state.nitriteReductingBacteria && regolithEaters == state.regolithEaters && selfReplicatingBacteria == state.selfReplicatingBacteria && anaerobicMicroorganisms == state.anaerobicMicroorganisms && bacterialAggregates == state.bacterialAggregates && decomposers == state.decomposers && decomposingFungus == state.decomposingFungus && birds == state.birds && filterFeeders == state.filterFeeders && fish == state.fish && livestock == state.livestock && zoos == state.zoos && ecologicalZone == state.ecologicalZone && herbivores == state.herbivores && smallAnimals == state.smallAnimals && tardigrades == state.tardigrades && arclight == state.arclight && extraForests == state.extraForests && trRaisedThisPhase == state.trRaisedThisPhase && unmiUsedThisPhase == state.unmiUsedThisPhase && heatAsMc == state.heatAsMc && physixComplex == state.physixComplex && fibrousComposite == state.fibrousComposite;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mc, heat, plants, cards, tr, extraMcValue, ghgBacteriaCount, nitriteReductingBacteria, regolithEaters, selfReplicatingBacteria, anaerobicMicroorganisms, bacterialAggregates, decomposers, decomposingFungus, birds, filterFeeders, fish, livestock, zoos, ecologicalZone, herbivores, smallAnimals, tardigrades, arclight, extraForests, trRaisedThisPhase, unmiUsedThisPhase, heatAsMc, physixComplex, fibrousComposite);
    }
}
