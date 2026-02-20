package com.terraforming.ares.services.ai.turnProcessors.frontier;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.buffedCorporations.BuffedArclightCorporation;
import com.terraforming.ares.cards.buffedCorporations.BuffedEcolineCorporation;
import com.terraforming.ares.cards.buffedCorporations.BuffedHelionCorporation;
import com.terraforming.ares.cards.corporations.ArclightCorporation;
import com.terraforming.ares.cards.corporations.EcolineCorporation;
import com.terraforming.ares.cards.corporations.HelionCorporation;
import com.terraforming.ares.cards.corporations.ZetacellCorporation;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiConstants;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

@Getter
public class StateContext {
    private int mcOnOcean;
    private int plantsOnOcean;
    private boolean ecolineDiscount;
    private boolean filterFeeders;
    private int plantsOnTemperature;

    private final boolean isOxygenMax;
    private final boolean isOceansMax;
    private final boolean isTemperatureMax;

    private final boolean chose3rdPhase;
    private final boolean hasAssemblyLines;
    private final Player player;
    private final Map<Class<?>, Card> cards;
    private final int blueCardsCount;
    private final int eventTagCount;
    private final int energyTagCount;
    private final int milestoneAchieved;

    private boolean transformsHeatIntoMc;
    private boolean isHelionCorp;

    private final int forestPrice;

    public static final int wpScore = 5;//TODO is 5 okay?

    private boolean conservedBiome;
    private boolean smallAnimals;
    private boolean fish;
    private boolean herbivores;
    private boolean livestock;
    private boolean physixComplex;

    public StateContext(CardService cardService, Map<Class<?>, Card> cardsPlayed, Planet planet, Player player, MarsGame game) {
        this.cards = cardsPlayed;
        this.player = player;
        this.isOxygenMax = planet.isOxygenMax();
        this.isOceansMax = planet.isOceansMax();
        this.isTemperatureMax = planet.isTemperatureMax();
        this.chose3rdPhase = player.getChosenPhase() == 3;
        this.hasAssemblyLines = cardsPlayed.containsKey(AssemblyLines.class);
        this.blueCardsCount = (int) cardsPlayed.values().stream().filter(card -> card.getColor() == CardColor.BLUE).count();
        this.eventTagCount = cardService.countPlayedTags(player, Set.of(Tag.EVENT));
        this.energyTagCount = cardService.countPlayedTags(player, Set.of(Tag.ENERGY));
        this.milestoneAchieved = (int) game.getMilestones().stream().filter(milestone -> milestone.isAchieved(player)).count();

        if (cardsPlayed.containsKey(ZetacellCorporation.class)) {
            mcOnOcean += 2;
            plantsOnOcean += 2;
        }
        if (cardsPlayed.containsKey(EcolineCorporation.class) || cardsPlayed.containsKey(BuffedEcolineCorporation.class)) {
            ecolineDiscount = true;
        }
        if (cardsPlayed.containsKey(HelionCorporation.class) || cardsPlayed.containsKey(BuffedHelionCorporation.class)) {
            transformsHeatIntoMc = true;
            isHelionCorp = true;
        }
        if (cardsPlayed.containsKey(PowerInfrastructure.class)) {
            transformsHeatIntoMc = true;
        }
        if (cardsPlayed.containsKey(ArcticAlgae.class)) {
            plantsOnOcean += 4;
        }
        if (cardsPlayed.containsKey(ConservedBiome.class)) {
            conservedBiome = true;
        }
        if (cardsPlayed.containsKey(FilterFeeders.class) || cardsPlayed.containsKey(BuffedFilterFeeders.class)) {
            filterFeeders = true;
        }
        if (cardsPlayed.containsKey(Fish.class)) {
            fish = true;
        }
        if (cardsPlayed.containsKey(Herbivores.class)) {
            herbivores = true;
        }
        if (cardsPlayed.containsKey(Livestock.class)) {
            livestock = true;
        }
        if (cardsPlayed.containsKey(PhysicsComplex.class)) {
            physixComplex = true;
        }
        if (cardsPlayed.containsKey(SmallAnimals.class)) {
            smallAnimals = true;
        }
        if (cardsPlayed.containsKey(VolcanicSoil.class)) {
            plantsOnTemperature += 2;
        }

        this.forestPrice = ecolineDiscount ? 7 : 8;
    }

    public boolean canUseHeatAsMc() {
        return transformsHeatIntoMc;
    }

    public void applyMandatoryHeatAndPlants(State state) {
        if (!isTemperatureMax) {
            while (state.heat >= 8) {
                state.heat -= 8;
                temperatureBuilt(state);
            }
        }
        while (state.plants >= forestPrice) {
            state.plants -= (short) forestPrice;
            forestBuilt(state);
        }
    }

    public double score(State state) {
        state = state.copy();
        applyMandatoryHeatAndPlants(state);

        return state.mc +
                state.heat * 2 +
                state.extraForests * wpScore +
                state.plants * ((isOxygenMax ? 1 : 2) * ((double) 8 / (forestPrice))) + ((double) (state.plants / forestPrice) * wpScore / 2 * (conservedBiome ? 1 : 0)) +
                state.cards * 3 +
                state.tr * 10 +
                state.ghgBacteriaCount * (isTemperatureMax ? 1 : 2.5) +
                state.nitriteReductingBacteria * (isOceansMax ? 1 : 2.5) +
                state.regolithEaters * (isOxygenMax ? 1 : 2.5) +
                state.selfReplicatingBacteria * 3.5 +
                state.anaerobicMicroorganisms * 4 +
                state.bacterialAggregates * 2 +
                state.decomposers * 1.5 +
                state.decomposingFungus * 3 +
                state.fibrousComposite * 1.5 +
                (state.birds + (float) state.filterFeeders / 3 + state.fish + state.livestock + state.zoos + (float) state.ecologicalZone / 2 + (float) state.herbivores / 2 + (float) state.smallAnimals / 2 + (float) state.tardigrades / 3 + (float) state.arclight / 2 + (float) state.physixComplex / 2 ) * wpScore +
                state.extraMcValue;
    }

    public void oceanBuilt(State state) {
        state.mc += (short) mcOnOcean;
        state.plants += (short) plantsOnOcean;
        state.tr += 1;
        if (fish) {
            state.fish++;
        }
        if (herbivores) {
            state.herbivores++;
        }
        state.trRaisedThisPhase = true;
        state.extraMcValue += 2;
    }

    public void oxygenBuilt(State state) {
        state.tr += 1;
        if (herbivores) {
            state.herbivores++;
        }
        state.trRaisedThisPhase = true;
    }

    public void temperatureBuilt(State state) {
        state.tr += 1;
        if (livestock) {
            state.livestock++;
        }
        if (herbivores) {
            state.herbivores++;
        }
        if (physixComplex) {
            state.physixComplex++;
        }
        state.plants += (short) plantsOnTemperature;
        state.trRaisedThisPhase = true;
    }

    public void forestBuilt(State state) {
        if (!isOxygenMax) {
            oxygenBuilt(state);
        }
        if (smallAnimals) {
            state.smallAnimals++;
        }
        state.extraForests++;
    }

    public void onMicrobeGained(State state, int count) {
        if (filterFeeders) {
            state.filterFeeders++;
        }
    }

    public State getInitialState() {
        State state = new State();
        state.mc = (short) player.getMc();
        state.heat = (short) player.getHeat();
        state.plants =(short) player.getPlants();
        state.cards =(short) player.getHand().size();
        state.tr =(short) player.getTerraformingRating();

        state.ghgBacteriaCount = (short) (player.getCardResourcesCount().getOrDefault(GhgProductionBacteria.class, 0).shortValue() + player.getCardResourcesCount().getOrDefault(BuffedGhgProductionBacteria.class, 0).shortValue());
        state.nitriteReductingBacteria = player.getCardResourcesCount().getOrDefault(NitriteReductingBacteria.class, 0).shortValue();
        state.regolithEaters = (short) (player.getCardResourcesCount().getOrDefault(RegolithEaters.class, 0).shortValue() + player.getCardResourcesCount().getOrDefault(BuffedRegolithEaters.class, 0).shortValue());
        state.selfReplicatingBacteria = player.getCardResourcesCount().getOrDefault(SelfReplicatingBacteria.class, 0).shortValue();

        state.anaerobicMicroorganisms = player.getCardResourcesCount().getOrDefault(AnaerobicMicroorganisms.class, 0).shortValue();
        state.bacterialAggregates = player.getCardResourcesCount().getOrDefault(BacterialAggregates.class, 0).shortValue();
        state.decomposers = player.getCardResourcesCount().getOrDefault(Decomposers.class, 0).shortValue();
        state.decomposingFungus = player.getCardResourcesCount().getOrDefault(DecomposingFungus.class, 0).shortValue();

        state.trRaisedThisPhase = player.isHasUnmiAction();
        state.unmiUsedThisPhase = player.isDidUnmiAction();

        state.birds = player.getCardResourcesCount().getOrDefault(Birds.class, 0).shortValue();
        state.filterFeeders = player.getCardResourcesCount().getOrDefault(FilterFeeders.class, 0).shortValue();
        state.fish = player.getCardResourcesCount().getOrDefault(Fish.class, 0).shortValue();
        state.livestock = player.getCardResourcesCount().getOrDefault(Livestock.class, 0).shortValue();
        state.zoos = player.getCardResourcesCount().getOrDefault(Zoos.class, 0).shortValue();
        state.ecologicalZone = player.getCardResourcesCount().getOrDefault(EcologicalZone.class, 0).shortValue();
        state.herbivores = player.getCardResourcesCount().getOrDefault(Herbivores.class, 0).shortValue();
        state.smallAnimals = player.getCardResourcesCount().getOrDefault(SmallAnimals.class, 0).shortValue();
        state.tardigrades = player.getCardResourcesCount().getOrDefault(Tardigrades.class, 0).shortValue();
        state.arclight = (short) (player.getCardResourcesCount().getOrDefault(ArclightCorporation.class, 0).shortValue() + player.getCardResourcesCount().getOrDefault(BuffedArclightCorporation.class, 0).shortValue());
        state.physixComplex = player.getCardResourcesCount().getOrDefault(PhysicsComplex.class, 0).shortValue();
        state.fibrousComposite = player.getCardResourcesCount().getOrDefault(FibrousCompositeMaterial.class, 0).shortValue();

        return state;
    }

}
