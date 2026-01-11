package com.terraforming.ares.services.ai.turnProcessors.frontier;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.buffedCorporations.BuffedUnmiCorporation;
import com.terraforming.ares.cards.corporations.CelestiorCorporation;
import com.terraforming.ares.cards.corporations.HyperionSystemsCorporation;
import com.terraforming.ares.cards.corporations.ModproCorporation;
import com.terraforming.ares.cards.corporations.UnmiCorporation;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.turnProcessors.frontier.actions.*;
import lombok.RequiredArgsConstructor;
import org.nd4j.common.io.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class Frontier {
    public static final int NO_ACTION_ID = -2;
    private final CardService cardService;
    private static final int MAX_ACTIONS = 10;
    private static final int BEAM_WIDTH = 1000;
    static final int RESULTS_BEAM_WIDTH = 2000;

    public Map<State, Node> doFrontier(MarsGame game, Player player) {
        Map<Class<?>, Card> cardsPlayed = player.getPlayed().getCards().stream().map(cardService::getCard).collect(Collectors.toMap(Card::getClass, Function.identity()));

        StateContext stateContext = new StateContext(cardService, cardsPlayed, game.getPlanetAtTheStartOfThePhase(), player, game);

        Deck activatedBlueCards = player.getActivatedBlueCards();
        Deck activatedBlueCardsTwice = player.getActivatedBlueCardsTwice();
        int blueActionExtraActivationsLeft = player.getBlueActionExtraActivationsLeft();

        Map<Class<?>, Card> cardsThatCanStillBeActivated = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(Card::isActiveCard)
                .filter(card -> !activatedBlueCards.containsCard(card.getId()) || blueActionExtraActivationsLeft > 0 && !activatedBlueCardsTwice.containsCard(card.getId()))
                .collect(Collectors.toMap(Card::getClass, Function.identity()));
        boolean needUnmiForTraversal = player.isUnmiCorporation() && !player.isDidUnmiAction();
        if (cardsThatCanStillBeActivated.isEmpty() && !needUnmiForTraversal) {
            return Map.of();
        }

        List<Action> actionPoolThatCanBeActivated = collectActionsPool(cardsThatCanStillBeActivated, stateContext);
        if (needUnmiForTraversal) {
            actionPoolThatCanBeActivated.add(new UnmiAction(UNMI_ACTION_ID));
        }
        if (actionPoolThatCanBeActivated.isEmpty()) {
            return Map.of();
        }

        long usedActionsMask =
                collectActionsPool(
                        activatedBlueCards.getCards().stream().distinct().map(cardService::getCard).collect(Collectors.toMap(Card::getClass, Function.identity())), stateContext
                )
                        .stream()
                        .mapToLong(action -> 1L << action.id)
                        .reduce(0L, (a, b) -> a | b);


        return doFrontierSorting(player, actionPoolThatCanBeActivated, stateContext, usedActionsMask);
    }

    private List<Action> collectActionsPool(Map<Class<?>, Card> cards, StateContext stateContext) {
        List<Action> actions = new ArrayList<>();

        cards.keySet().stream().forEach(blueCard -> {
            List<BlueAction> blueActions = CARD_TO_BLUE_ACTION_MAPPING.get(blueCard);
            if (CollectionUtils.isEmpty(blueActions)) {
                if (!CARDS_WITHOUT_MAPPING.contains(blueCard)) {
                    throw new IllegalStateException("Unknow blue action for frontier " + blueCard);
                }
            } else {
                actions.addAll(blueActions);
            }
        });


        for (PutMicrobeEffect putMicrobeEffect : PutMicrobeEffect.values()) {
            if (putMicrobeEffect.shouldTraverseSpecialEffect(stateContext)) {
                if (cards.containsKey(ConservedBiome.class)) {
                    actions.add(new AddMicrobeAction(CONSERVED_BIOME_ACTION_ID, putMicrobeEffect));
                }
                if (cards.containsKey(ExtremeColdFungus.class)) {
                    actions.add(new AddMicrobeAction(EXTREME_COLD_FUNGUS_ACTION_ID, putMicrobeEffect));
                }
                if (cards.containsKey(SymbioticFungus.class)) {
                    actions.add(new AddMicrobeAction(SYMBIOTIC_FUNGUS_ACTION_ID, putMicrobeEffect));
                }
            }
        }

        for (ConsumeResourceEffect consumeResourceEffect : ConsumeResourceEffect.values()) {
            if (consumeResourceEffect.shouldTraverseSpecialEffect(stateContext)) {
                if (cards.containsKey(DecomposingFungus.class)) {
                    actions.add(new DecomposingFungusConsumeAction(DECOMPOSING_FUNGUS_ACTION_ID, consumeResourceEffect));
                }
            }
        }

        return actions;
    }


    private Map<State, Node> doFrontierSorting(
            Player player,
            List<Action> actionsPool,
            StateContext stateContext, long usedActionsMask) {

        State initialState = stateContext.getInitialState();

        long startTime = System.currentTimeMillis();

        Map<State, Node> frontier = new HashMap<>();
        frontier.put(initialState, new Node(initialState, usedActionsMask, 0L, player.getBlueActionExtraActivationsLeft()));

        boolean anyActionPossible = false;

        for (Node n : frontier.values()) {
            for (Action a : actionsPool) {
                if (a.canApplyInternal(stateContext, n.state)) {
                    anyActionPossible = true;
                    break;
                }
            }

            if (!anyActionPossible && stateContext.canUseHeatAsMc()) {
                State heatState = n.state.copy();
                heatState.heatAsMc = true;

                for (Action a : actionsPool) {
                    if (a.canApplyInternal(stateContext, heatState)) {
                        anyActionPossible = true;
                        break;
                    }
                }
            }

            if (anyActionPossible) break;
        }

        if (!anyActionPossible) {
            return Map.of();
        }

        Map<State, Node> results = new HashMap<>();

        Node doNothing = new Node(initialState.copy(), 0L, 0L, 0, NO_ACTION_ID, null);

        results.put(doNothing.state, doNothing);


        for (int step = 0; step < MAX_ACTIONS; step++) {

            if (step > 0) {
                for (Node node : frontier.values()) {
                    if (node.firstActionId >= 0) {
                        results.put(node.state, node);
                    }
                }

                results = beamFilter(stateContext, results, RESULTS_BEAM_WIDTH);
            }

            Map<State, Node> next = new HashMap<>();

            for (Node n : frontier.values()) {

                boolean isRoot = n.firstActionId == -1;

                for (Action a : actionsPool) {
                    long bit = 1L << a.id;

                    boolean usedOnce = (n.usedActionsMask & bit) != 0;
                    boolean usedRepeat = (n.usedRepeatMask & bit) != 0;

                    if (usedRepeat) continue;
                    if (usedOnce && n.repeatsLeft == 0) continue;

                    if (!a.canApplyInternal(stateContext, n.state)) continue;

                    State s = n.state.copy();
                    a.apply(stateContext, s);

                    long newUsedOnceMask = n.usedActionsMask | bit;
                    long newUsedRepeatMask = n.usedRepeatMask;
                    int newRepeatsLeft = n.repeatsLeft;

                    if (usedOnce) {
                        newUsedRepeatMask |= bit;
                        newRepeatsLeft--;
                    }

                    Node nextNode = new Node(
                            s,
                            newUsedOnceMask,
                            newUsedRepeatMask,
                            newRepeatsLeft,
                            isRoot ? a.id : n.firstActionId,
                            isRoot ? a.getContext(stateContext) : n.firstActionContext
                    );

                    next.put(s, nextNode);
                }

                if (!n.state.heatAsMc && stateContext.canUseHeatAsMc()) {
                    State s2 = n.state.copy();
                    s2.heatAsMc = true;

                    Node switched = new Node(
                            s2,
                            n.usedActionsMask,
                            n.usedRepeatMask,
                            n.repeatsLeft,
                            n.firstActionId,
                            n.firstActionContext
                    );

                    next.put(s2, switched);
                }
            }

            if (next.isEmpty()) break;

            frontier = beamFilter(stateContext, next, BEAM_WIDTH);
        }

        results.putAll(frontier);
        results = beamFilter(stateContext, results, RESULTS_BEAM_WIDTH);
        results.keySet().forEach(stateContext::applyMandatoryHeatAndPlants);

        for (Node node : results.values()) {
            if (node.firstActionId == -1) {
                throw new IllegalStateException("Found invalid action id in result set");
            }
        }

        if (results.size() == RESULTS_BEAM_WIDTH) {
            System.out.printf("MAX frontier scenarios %s. Time ms: %s%n", results.size(), (System.currentTimeMillis() - startTime));

        }

        return results;
    }

    static Map<State, Node> beamFilter(StateContext stateContext, Map<State, Node> input, int beamWidth) {
        // сортируем состояния по score (лучшие сверху)
        List<State> list = new ArrayList<>(input.keySet());
        for (State state : list) {
            state.scoreCache = stateContext.score(state);
        }
        list.sort(Comparator.comparingDouble((State state) -> state.scoreCache).reversed());

        // оставляем только top BEAM_WIDTH
        if (list.size() > beamWidth) {
            list = list.subList(0, beamWidth);
        }

        // собираем результат
        Map<State, Node> result = new HashMap<>(list.size());
        for (State s : list) {
            result.put(s, input.get(s));
        }

        return result;
    }

    public static final Map<Class<? extends Card>, List<BlueAction>> CARD_TO_BLUE_ACTION_MAPPING;
    public static final Map<Integer, Class<? extends Card>> ACTION_ID_TO_CARD_MAPPING;
    public static final Set<Class<? extends Card>> CARDS_WITHOUT_MAPPING = Set.of(
            ResearchGrant.class,
            PowerInfrastructure.class,
            VirtualEmployeeDevelopment.class,
            RedraftedContracts.class,
            DecomposingFungus.class,
            GreenHouses.class,
            SoftwareStreamlining.class,
            ExperimentalTechnology.class,
            SymbioticFungus.class
    );

    public static final int EXTREME_COLD_FUNGUS_ACTION_ID = 17;
    public static final int FARMING_COOPS_ACTION_ID = 19;
    public static final int FIBROUS_COMPOSITE_ACTION_ID = 20;
    public static final int GHG_PRODUCTION_ACTION_ID = 22;
    public static final int MATTER_GENERATOR_ACTION_ID = 26;
    public static final int NITRITE_REDUCTION_ACTION_ID = 29;
    public static final int REGOLITH_EATERS_ACTION_ID = 31;
    public static final int SELF_REPLICATING_BACTERIA = 32;
    public static final int CONSERVED_BIOME_ACTION_ID = 40;
    public static final int DECOMPOSING_FUNGUS_ACTION_ID = 41;
    public static final int SYMBIOTIC_FUNGUS_ACTION_ID = 42;
    public static final int UNMI_ACTION_ID = 43;

    static {
        Map<Class<? extends Card>, List<BlueAction>> map = new HashMap<>();

        map.put(AdvancedScreeningTechnology.class, List.of(new AdvancedScreeningTechAction(1)));
        map.put(AiCentral.class, List.of(new AiCentralAction(2)));
        map.put(AquiferPumping.class, List.of(new AquiferPumpingAction(3)));
        map.put(ArtificialJungle.class, List.of(new ArtificialJungleAction(4)));
        map.put(AssetLiquidation.class, List.of(new AssetLiquidationAction(5)));
        map.put(Birds.class, List.of(new BirdsAction(6)));
        map.put(BrainstormingSession.class, List.of(new BrainstormingSessionAction(7)));
        map.put(CaretakerContract.class, List.of(new CaretakerContractAction(8)));
        map.put(CelestiorCorporation.class, List.of(new CelestiorAction(9)));
        map.put(CircuitBoardFactory.class, List.of(new CircuitBoardFactoryAction(10)));
        map.put(CityCouncil.class, List.of(new CityCouncilAction(11)));
        map.put(CommunityAfforestation.class, List.of(new CommunityAfforestationAction(12)));
        map.put(CommunityGardens.class, List.of(new CommunityGardensAction(13)));
        map.put(DevelopedInfrastructure.class, List.of(new DevelopedInfrastructureAction(14)));
        map.put(DevelopmentCenter.class, List.of(new DevelopmentCenterAction(15)));
        map.put(DroneAssistedConstruction.class, List.of(new DroneAssistedConstructionAction(16)));
        map.put(ExtremeColdFungus.class, List.of(new ExtremeColdFungusAddPlantAction(EXTREME_COLD_FUNGUS_ACTION_ID)));
        map.put(FarmersMarket.class, List.of(new FarmersMarketAction(18)));
        map.put(FarmingCoops.class, List.of(new FarmingCoopsAction(FARMING_COOPS_ACTION_ID)));
        map.put(FibrousCompositeMaterial.class, List.of(new FibrousCompositeAction(FIBROUS_COMPOSITE_ACTION_ID)));
        map.put(GasCooledReactors.class, List.of(new GasCooledReactorsAction(21)));
        map.put(GhgProductionBacteria.class, List.of(new GhgProductionAddMicrobe(GHG_PRODUCTION_ACTION_ID), new GhgProductionConsumeMicrobe(GHG_PRODUCTION_ACTION_ID)));
        map.put(HydroElectricEnergy.class, List.of(new HydroElectricAction(23)));
        map.put(HyperionSystemsCorporation.class, List.of(new HyperionAction(24)));
        map.put(IronWorks.class, List.of(new IronWorksAction(25)));
        map.put(MatterGenerator.class, List.of(new MatterGeneratorAction(MATTER_GENERATOR_ACTION_ID)));
        map.put(MatterManufactoring.class, List.of(new MatterManufactoringAction(27)));
        map.put(ModproCorporation.class, List.of(new ModProAction(28)));
        map.put(NitriteReductingBacteria.class, List.of(new NitriteReductingAddMicrobe(NITRITE_REDUCTION_ACTION_ID), new NitriteReductingConsumeMicrobe(NITRITE_REDUCTION_ACTION_ID)));
        map.put(ProgressivePolicies.class, List.of(new ProgressivePoliciesAction(30)));
        map.put(RegolithEaters.class, List.of(new RegolithEatersAddMicrobe(REGOLITH_EATERS_ACTION_ID), new RegolithEatersConsumeMicrobe(REGOLITH_EATERS_ACTION_ID)));
        map.put(SelfReplicatingBacteria.class, List.of(new SelfReplicatingAddMicrobe(SELF_REPLICATING_BACTERIA)));
        map.put(SolarPunk.class, List.of(new SolarPunkAction(33)));
        map.put(Steelworks.class, List.of(new SteelWorksAction(34)));
        map.put(Tardigrades.class, List.of(new TardigradesAddMicrobe(35)));
        map.put(ThinkTank.class, List.of(new ThinkTankAction(36)));
        map.put(VolcanicPools.class, List.of(new VolcanicPoolsAction(37)));
        map.put(WaterImportFromEuropa.class, List.of(new WaterImportFromEuropaAction(38)));
        map.put(WoodBurningStoves.class, List.of(new WoodBurningStovesAction(39)));
        map.put(ConservedBiome.class, List.of(new ConservedBiomeAddWpAction(CONSERVED_BIOME_ACTION_ID)));

        CARD_TO_BLUE_ACTION_MAPPING = Collections.unmodifiableMap(map);

        // Инициализируем обратный маппинг
        Map<Integer, Class<? extends Card>> inverseMap = new HashMap<>();
        map.forEach((cardClass, actions) -> {
            for (BlueAction action : actions) {
                inverseMap.put(action.id, cardClass);
            }
        });

        ACTION_ID_TO_CARD_MAPPING = Collections.unmodifiableMap(inverseMap);
    }

}
