package com.terraforming.ares.services.ai.turnProcessors.frontier;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.buffedCorporations.BuffedEcolineCorporation;
import com.terraforming.ares.cards.corporations.EcolineCorporation;
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
    private int halfWpOnOcean;
    private int halfWpOnOxygen;
    private int halfWpOnTemperature;
    private int halfWpOnForest;
    private int plantsOnTemperature;

    private final boolean isOxygenMax;
    private final boolean isOceansMax;
    private final boolean isTemperatureMax;

    private boolean chose3rdPhase;
    private boolean hasAssemblyLines;
    private Player player;
    private final Map<Class<?>, Card> cards;
    private int blueCardsCount;
    private int eventTagCount;
    private int energyTagCount;
    private int milestoneAchieved;

    public static final int wpScore = 5;

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
        if (cardsPlayed.containsKey(ArcticAlgae.class)) {
            plantsOnOcean += 4;
        }
        if (cardsPlayed.containsKey(ConservedBiome.class)) {
            halfWpOnForest++;
        }
        if (cardsPlayed.containsKey(FilterFeeders.class) || cardsPlayed.containsKey(BuffedFilterFeeders.class)) {
            filterFeeders = true;
        }
        if (cardsPlayed.containsKey(Fish.class)) {
            halfWpOnOcean += 2;
        }
        if (cardsPlayed.containsKey(Herbivores.class)) {
            halfWpOnOcean++;
            halfWpOnOxygen++;
            halfWpOnTemperature++;
        }
        if (cardsPlayed.containsKey(Livestock.class)) {
            halfWpOnTemperature += 2;
        }
        if (cardsPlayed.containsKey(PhysicsComplex.class)) {
            halfWpOnTemperature++;
        }
        if (cardsPlayed.containsKey(SmallAnimals.class)) {
            halfWpOnForest++;
        }
        if (cardsPlayed.containsKey(VolcanicSoil.class)) {
            plantsOnTemperature += 2;
        }
    }

    public double score(State state) {
        int forestPrice = ecolineDiscount ? 7 : 8;
        return state.mc +
                state.heat * 2 +
                state.plants * ((isOxygenMax ? 1 : 2) * ((double) 8 / (forestPrice))) + ((double) (state.plants / forestPrice) * wpScore / 2 * halfWpOnForest) +
                state.cards * 3 +
                state.tr * 10 +
                (double) (state.wp + state.sacrificialWp) / AiConstants.WP_DENOMINATOR_FOR_FRONTIER * wpScore +
                state.ghgBacteriaCount * (isTemperatureMax ? 1 : 2.5) +
                state.nitriteReductingBacteria * (isOceansMax ? 1 : 2.5) +
                state.regolithEaters * (isOxygenMax ? 1 : 2.5) +
                state.selfReplicatingBacteria * 3.5 +
                state.anaerobicMicroorganisms * 4 +
                state.bacterialAggregates * 2 +
                state.decomposers * 1.5 +
                state.decomposingFungus * 3 +
                state.extraMcValue;
    }

    public void oceanBuilt(State state) {
        state.mc += mcOnOcean;
        state.plants += plantsOnOcean;
        state.tr += 1;
        state.wp += halfWpOnOcean * 3;
    }

    public void oxygenBuilt(State state) {
        state.tr += 1;
        state.wp += halfWpOnOxygen * 3;
    }

    public void temperatureBuilt(State state) {
        state.tr += 1;
        state.wp += halfWpOnTemperature * 3;
        state.plants += plantsOnTemperature;
    }

    public void forestBuilt(State state) {
        if (!isOxygenMax) {
            oxygenBuilt(state);
        }
        state.wp += 6;
        state.wp += halfWpOnForest * 3;
        state.extraForests++;
    }

    public void onMicrobeGained(State state, int count) {
        if (filterFeeders) {
            state.sacrificialWp += count * 2;
        }
    }

    public State getInitialState() {
        State state = new State();
        state.mc = player.getMc();
        state.heat = player.getHeat();
        state.plants = player.getPlants();
        state.cards = player.getHand().size();
        state.tr = player.getTerraformingRating();
        state.sacrificialWp = (player.getCardResourcesCount().getOrDefault(FilterFeeders.class, 0) + player.getCardResourcesCount().getOrDefault(Tardigrades.class, 0)) * 2;
        state.wp = -state.sacrificialWp;

        state.ghgBacteriaCount = player.getCardResourcesCount().getOrDefault(GhgProductionBacteria.class, 0) + player.getCardResourcesCount().getOrDefault(BuffedGhgProductionBacteria.class, 0);
        state.nitriteReductingBacteria = player.getCardResourcesCount().getOrDefault(NitriteReductingBacteria.class, 0);
        state.regolithEaters = player.getCardResourcesCount().getOrDefault(RegolithEaters.class, 0) + player.getCardResourcesCount().getOrDefault(BuffedRegolithEaters.class, 0);
        state.selfReplicatingBacteria = player.getCardResourcesCount().getOrDefault(SelfReplicatingBacteria.class, 0);

        state.anaerobicMicroorganisms = player.getCardResourcesCount().getOrDefault(AnaerobicMicroorganisms.class, 0);
        state.bacterialAggregates = player.getCardResourcesCount().getOrDefault(BacterialAggregates.class, 0);
        state.decomposers = player.getCardResourcesCount().getOrDefault(Decomposers.class, 0);
        state.decomposingFungus = player.getCardResourcesCount().getOrDefault(DecomposingFungus.class, 0);

        return state;
    }

}
