package com.terraforming.ares.services.ai.turnProcessors.network2;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.corporations.CelestiorCorporation;
import com.terraforming.ares.cards.corporations.HyperionSystemsCorporation;
import com.terraforming.ares.cards.corporations.ModproCorporation;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.StandardProjectService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.AiEndgameService;
import com.terraforming.ares.services.ai.advanced.IDataCollect;
import com.terraforming.ares.services.ai.dl4j.NNService;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import com.terraforming.ares.services.ai.turnProcessors.frontier.Frontier;
import com.terraforming.ares.services.ai.turnProcessors.frontier.Node;
import com.terraforming.ares.services.ai.turnProcessors.frontier.RequiresMcPayment;
import com.terraforming.ares.services.ai.turnProcessors.frontier.State;
import com.terraforming.ares.services.ai.turnProcessors.frontier.actions.ConsumeResourceEffect;
import com.terraforming.ares.services.ai.turnProcessors.frontier.actions.PutMicrobeEffect;
import com.terraforming.ares.services.ai.turnProcessors.network2.buildParams.*;
import com.terraforming.ares.services.ai.turnProcessors.network2.dto.BestFutureOptions;
import com.terraforming.ares.services.ai.turnProcessors.network2.dto.CardWithChanceModifier;
import com.terraforming.ares.services.ai.turnProcessors.network2.projection.SelfReplicatingBacteriaService;
import lombok.RequiredArgsConstructor;
import org.nd4j.common.io.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class Network2ThirdPhaseActionProcessor {
    private static final double EPS = 5e-4;

    private final CardService cardService;
    private final AiTurnService aiTurnService;
    private final AiInputOptimizer aiInputOptimizer;
    private final ActionProjectionDataCollector actionProjectionDataCollector;
    private final IDataCollect iDataCollect;
    private final NNService nnService;
    private final AiOptimalBuildService aiOptimalBuildService;
    private final AiCardBuildInputAnalyzer aiCardBuildInputAnalyzer;
    private final Network2DraftCardsProjectionService network2DraftCardsProjectionService;
    private final AiMarsUniversityInputHandler aiMarsUniversityInputHandler;
    private final StandardProjectService standardProjectService;
    private final Frontier frontier;
    private final AiEndgameService aiEndgameService;

    private final Set<Class<?>> DO_IMMEDIATELY_UNCONDITIONALLY = Set.of(
            CelestiorCorporation.class,
            HyperionSystemsCorporation.class,
            ModproCorporation.class,
            AdvancedScreeningTechnology.class,
            AiCentral.class,
            Birds.class,
            BrainstormingSession.class,
            CircuitBoardFactory.class,
            CommunityGardens.class,
            Tardigrades.class,
            CityCouncil.class,
            DroneAssistedConstruction.class,
            SoftwareStreamlining.class
    );
    private final SelfReplicatingBacteriaService selfReplicatingBacteriaService;

    public boolean processTurn(MarsGame game, Player player, List<TurnType> possibleTurns) {
        Deck activatedBlueCards = player.getActivatedBlueCards();

        Map<Class<?>, Card> neverActivatedBlueCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(Card::isActiveCard)
                .filter(card -> !activatedBlueCards.containsCard(card.getId()))
                .collect(Collectors.toMap(Card::getClass, Function.identity()));

        if (doImmediateUnconditionalActions(game, player, neverActivatedBlueCards)) {
            return true;
        }

        prefilterCompletelyUnusableCards(game, player, neverActivatedBlueCards);

        if (doActionsWhereChoiceHasNoChoice(game, player, neverActivatedBlueCards)) {
            return true;
        }

        if (processConservedBiomeAndResearchGrant(game, player, neverActivatedBlueCards)) {
            return true;
        }

        if (processPhaseUpgradeCards(game, player, neverActivatedBlueCards)) {
            return true;
        }

        if (buyCardsForMoney(game, player, neverActivatedBlueCards)) {
            return true;
        }

        int blueActionExtraActivationsLeft = player.getBlueActionExtraActivationsLeft();
        Deck activatedBlueCardsTwice = player.getActivatedBlueCardsTwice();

        Map<Class<?>, Card> blueCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .filter(Card::isActiveCard)
                .filter(card -> !activatedBlueCards.containsCard(card.getId()) || blueActionExtraActivationsLeft > 0 && !activatedBlueCardsTwice.containsCard(card.getId()))
                .collect(Collectors.toMap(Card::getClass, Function.identity()));

        prefilterCompletelyUnusableCards(game, player, blueCards);


        MarsGame potentialMarsAfterDoingSelfReplicatingBacteria = null;
        if (blueCards.containsKey(SelfReplicatingBacteria.class)) {
            potentialMarsAfterDoingSelfReplicatingBacteria = selfReplicatingBacteriaService.simulateSelfReplicatingBacteriaFinalActions(blueCards, game, player);
        }

        BestFutureOptions bestRegularFutureOptions = getBestFutureOptions(game, player);
        if (potentialMarsAfterDoingSelfReplicatingBacteria != null) {
            BestFutureOptions bestFutureOptionsAfterReplicatingBacteria = getBestFutureOptions(potentialMarsAfterDoingSelfReplicatingBacteria, potentialMarsAfterDoingSelfReplicatingBacteria.getPlayerByUuid(player.getUuid()));

            double bestChanceFromReplicatingBacteria = bestFutureOptionsAfterReplicatingBacteria.getBestChance();
            if (bestFutureOptionsAfterReplicatingBacteria.getBestFutureOptions().isEmpty()) {
                bestChanceFromReplicatingBacteria = nnService.predictBatch(List.of(iDataCollect.collectData(potentialMarsAfterDoingSelfReplicatingBacteria, potentialMarsAfterDoingSelfReplicatingBacteria.getPlayerByUuid(player.getUuid()))), NNService.ModelType.OPTIMIZED).getFirst().baseProb;
            }

            double bestChanceFromRegularOptions = bestRegularFutureOptions.getBestChance();
            if (bestRegularFutureOptions.getBestFutureOptions().isEmpty()) {
                bestChanceFromRegularOptions = nnService.predictBatch(List.of(iDataCollect.collectData(game, player)), NNService.ModelType.OPTIMIZED).getFirst().baseProb;
            }

            if (bestChanceFromReplicatingBacteria > bestChanceFromRegularOptions) {
                selfReplicatingBacteriaService.doSelfReplicatingBacteriaFinalActions(blueCards, game, player);
                return true;
            } else if (performBestActionFromBestSortedOptions(game, player, bestRegularFutureOptions)) {
                return true;
            }
        } else if (performBestActionFromBestSortedOptions(game, player, bestRegularFutureOptions)) {
            return true;
        }

        if (doMandatoryResourceIntoTerraformingActions(possibleTurns, game, player)) {
            return true;
        }

        if (game.getPlanetAtTheStartOfThePhase().isTemperatureMax() && player.getHeat() > 0 && isAbleToConvertHeat(player)) {
            convertHeat(game, player, player.getHeat());
        }

        if (doRedraftedContracts(blueCards, game, player, activatedBlueCards)) {
            return true;
        }

        if (processPhaseUpgradeCards(game, player, blueCards)) {
            return true;
        }

        if (buyCardsForMoney(game, player, blueCards)) {
            return true;
        }


        //software streamlining/redrafted contracts/research grant
        if (blueCards.containsKey(SoftwareStreamlining.class)) {
            aiTurnService.performBlueAction(game, player, blueCards.get(SoftwareStreamlining.class).getId(), Map.of());
            return true;
        }
        if (blueCards.containsKey(RedraftedContracts.class) && doRedraftedContracts(blueCards, game, player, activatedBlueCardsTwice)) {
            return true;
        }

        if (processConservedBiomeAndResearchGrant(game, player, blueCards)) {
            return true;
        }


        if (!blueCards.isEmpty()) {
            for (Class<?> blueCardClass : blueCards.keySet()) {
                if (!CARDS_THAT_CAN_SKIP_FOR_ACTION.contains(blueCardClass)) {
//                    System.out.println("Blue cards for skip, need a check " + blueCardClass);
                }
            }
        }

        if (doStandardTurnIfBetterThanChance(game, player)) {
            return true;
        }


        if (aiEndgameService.doFinalActionsIfGameFinished(game, player)) {
            return true;
        }

        if (aiEndgameService.isFinishingGame(game, player)) {
            return true;
        }

        aiTurnService.skipTurn(player);
        return true;
    }

    private boolean doStandardTurnIfBetterThanChance(MarsGame game, Player player) {
        List<float[]> dataToCheck = new ArrayList<>();
        List<StandardProjectType> standardProjectTypes = new ArrayList<>();

        for (StandardProjectType standardProjectType : List.of(StandardProjectType.FOREST, StandardProjectType.OCEAN, StandardProjectType.TEMPERATURE)) {
            String validationResult = standardProjectService.validateStandardProject(game, player, standardProjectType);
            if (validationResult != null) {
                continue;
            }
            float[] standardProjectState = projectPlayStandardAction(game, player.getUuid(), standardProjectType);
            dataToCheck.add(standardProjectState);
            standardProjectTypes.add(standardProjectType);
        }

        if (dataToCheck.isEmpty()) {
            return false;
        }

        dataToCheck.add(iDataCollect.collectData(game, player));

        List<Prediction> predictions = nnService.predictBatch(dataToCheck, NNService.ModelType.OPTIMIZED);
        double baseChance = predictions.removeLast().baseProb;

        StandardProjectType bestProject = null;
        for (int i = 0; i < standardProjectTypes.size(); i++) {
            if (predictions.get(i).baseProb > baseChance) {
                baseChance = predictions.get(i).baseProb;
                bestProject = standardProjectTypes.get(i);
            }
        }

        if (bestProject != null) {
            aiTurnService.standardProjectTurn(game, player, bestProject);
            return true;
        }
        return false;
    }

    private float[] projectPlayStandardAction(MarsGame game, String playerUuid, StandardProjectType type) {
        game = new MarsGame(game);
        Player player = game.getPlayerByUuid(playerUuid);

        int mc = player.getMc();

        if (type == StandardProjectType.OCEAN && mc >= standardProjectService.getProjectPrice(player, type)) {
            aiTurnService.standardProjectTurn(game, player, StandardProjectType.OCEAN);

            return iDataCollect.collectData(game, player);
        }

        if (type == StandardProjectType.FOREST && mc >= standardProjectService.getProjectPrice(player, type)) {
            aiTurnService.standardProjectTurn(game, player, StandardProjectType.FOREST);
            return iDataCollect.collectData(game, player);
        }

        if (type == StandardProjectType.TEMPERATURE && mc >= standardProjectService.getProjectPrice(player, type)) {
            aiTurnService.standardProjectTurn(game, player, StandardProjectType.TEMPERATURE);
            return iDataCollect.collectData(game, player);
        }

        throw new IllegalStateException("Invalid standard project type");
    }

    private final Set<Class<?>> CARDS_THAT_CAN_SKIP_FOR_ACTION = Set.of(
            VolcanicPools.class,
            CommunityAfforestation.class,
            MatterGenerator.class,
            PowerInfrastructure.class,
            AquiferPumping.class,
            DecomposingFungus.class,
            SolarPunk.class,
            DevelopmentCenter.class,
            FarmingCoops.class,
            DevelopedInfrastructure.class,
            ArtificialJungle.class,
            GasCooledReactors.class,
            FarmersMarket.class,//TODO strange
            ProgressivePolicies.class,
            HydroElectricEnergy.class,//TODO strange
            WaterImportFromEuropa.class,
            Steelworks.class,
            WoodBurningStoves.class,
            IronWorks.class,
            GreenHouses.class,
            SymbioticFungus.class,//TODO double check
            AssetLiquidation.class
    );

    private List<Integer> getBadCardsUpToCount(MarsGame game, Player player, int count) {
        if (player.getHand().isEmpty()) {
            return List.of();
        }
        return network2DraftCardsProjectionService.cardsToDiscardByProjectedValue(game, player)
                .stream()
                .map(CardWithChanceModifier::getCard)
                .limit(count)
                .map(Card::getId)
                .toList();
    }

    private BestFutureOptions getBestFutureOptions(MarsGame game, Player player) {
        Map<State, Node> stateNodeMap = frontier.doFrontier(game, player);
        if (stateNodeMap.isEmpty()) return new BestFutureOptions(List.of(), 0);

        return projectStateAndGetBestFutureOptions(stateNodeMap, game, player);
    }

    private boolean performBestActionFromBestSortedOptions(MarsGame game, Player player, BestFutureOptions bestFutureOptions) {
        List<Map.Entry<State, Node>> sortedOptions = bestFutureOptions.getBestFutureOptions();

        if (sortedOptions.isEmpty()) return false;

        Map<Class<?>, Card> allPlayedCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .collect(Collectors.toMap(Card::getClass, Function.identity()));

        boolean ignoreMatterGenerator = false;

        for (Map.Entry<State, Node> entry : sortedOptions) {
            Node node = entry.getValue();
            int actionId = node.firstActionId;
            Object context = node.firstActionContext;

            switch (actionId) {
                case Frontier.NO_ACTION_ID -> {
                    return false;
                }
                case Frontier.MATTER_GENERATOR_ACTION_ID, Frontier.FARMING_COOPS_ACTION_ID -> {
                    if (ignoreMatterGenerator) continue;
                    Card cardToSell = network2DraftCardsProjectionService.cardsToDiscardByProjectedValue(game, player)
                            .stream()
                            .map(CardWithChanceModifier::getCard)
                            .findFirst().orElse(null);
                    if (cardToSell != null) {
                        // Определяем, какую именно карту из синих карт игрока мы активируем
                        Class<? extends Card> activeCardClass = (actionId == Frontier.MATTER_GENERATOR_ACTION_ID)
                                ? MatterGenerator.class
                                : FarmingCoops.class;

                        return perform(allPlayedCards.get(activeCardClass), InputFlag.CARD_CHOICE, cardToSell.getId(), game, player);
                    }
                    ignoreMatterGenerator = true;
                }

                case Frontier.EXTREME_COLD_FUNGUS_ACTION_ID -> {
                    if (context == null) {
                        return perform(allPlayedCards.get(ExtremeColdFungus.class), InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT, 1, game, player);
                    }
                    int targetId = findTargetId(((PutMicrobeEffect) context).getTargetClasses(), allPlayedCards);
                    return perform(allPlayedCards.get(ExtremeColdFungus.class), InputFlag.EXTREME_COLD_FUNGUS_PUT_MICROBE, targetId, game, player);
                }

                case Frontier.GHG_PRODUCTION_ACTION_ID, Frontier.REGOLITH_EATERS_ACTION_ID,
                     Frontier.NITRITE_REDUCTION_ACTION_ID, Frontier.SELF_REPLICATING_BACTERIA,
                     Frontier.FIBROUS_COMPOSITE_ACTION_ID -> {
                    Class<?> clazz = getCardClassByActionId(actionId);
                    return perform(allPlayedCards.get(clazz), InputFlag.ADD_DISCARD_MICROBE, (int) context, game, player);
                }

                case Frontier.CONSERVED_BIOME_ACTION_ID -> {
                    int targetId = (context instanceof PutMicrobeEffect pme)
                            ? findTargetId(pme.getTargetClasses(), allPlayedCards)
                            : allPlayedCards.get((Class<?>) context).getId();
                    return perform(allPlayedCards.get(ConservedBiome.class), InputFlag.CARD_CHOICE, targetId, game, player);
                }

                case Frontier.DECOMPOSING_FUNGUS_ACTION_ID, Frontier.SYMBIOTIC_FUNGUS_ACTION_ID -> {
                    Class<?> cardClass = (actionId == Frontier.DECOMPOSING_FUNGUS_ACTION_ID) ? DecomposingFungus.class : SymbioticFungus.class;
                    List<Class<? extends Card>> targets = (context instanceof PutMicrobeEffect pme) ? pme.getTargetClasses() : ((ConsumeResourceEffect) context).getTargetClasses();
                    return perform(allPlayedCards.get(cardClass), InputFlag.CARD_CHOICE, findTargetId(targets, allPlayedCards), game, player);
                }
                case Frontier.UNMI_ACTION_ID -> {
                    convertHeatIfPossible(game, player, context);
                    aiTurnService.unmiRtCorporationTurn(game, player);
                    return true;
                }

                default -> {
                    convertHeatIfPossible(game, player, context);
                    Class<? extends Card> cardByActionId = Frontier.ACTION_ID_TO_CARD_MAPPING.get(actionId);
                    if (cardByActionId == null) {
                        throw new IllegalStateException("Unknown actionId: " + actionId);
                    }
                    Card card = allPlayedCards.get(cardByActionId);
                    if (card == null) {
                        throw new IllegalStateException("Card for action not present in available blueCards, actionId: " + actionId);
                    }
                    aiTurnService.performBlueAction(game, player, card.getId(), Map.of());
                    return true;
                }
            }
        }
        return false;
    }

    private void convertHeatIfPossible(MarsGame game, Player player, Object context) {
        if (!(context instanceof RequiresMcPayment requiredPayment)) {
            return;
        }
        if (player.getMc() >= requiredPayment.mc) {
            return;
        }
        int howMuchHeatToConvert = requiredPayment.mc - player.getMc();
        convertHeat(game, player, howMuchHeatToConvert);
    }

    private void convertHeat(MarsGame game, Player player, int howMuchHeatToConvert) {
        assert player.getHeat() >= howMuchHeatToConvert;
        boolean isHelionConversion = player.getPlayed().containsCard(10000) || player.getPlayed().containsCard(10100);
        boolean isCardConversion = !isHelionConversion && player.getPlayed().containsCard(Constants.POWER_INFRASTRUCTURE_CARD_ID);

        assert isCardConversion || isHelionConversion;
        if (isCardConversion && !player.getActivatedBlueCards().containsCard(Constants.POWER_INFRASTRUCTURE_CARD_ID)) {
            aiTurnService.performBlueAction(game, player, Constants.POWER_INFRASTRUCTURE_CARD_ID, Map.of(InputFlag.DISCARD_HEAT.getId(), List.of(howMuchHeatToConvert)));
        } else {
            player.setMc(player.getMc() + howMuchHeatToConvert);
            player.setHeat(player.getHeat() - howMuchHeatToConvert);
        }
    }

    private boolean isAbleToConvertHeat(Player player) {
        boolean isHelionConversion = player.getPlayed().containsCard(10000) || player.getPlayed().containsCard(10100);
        boolean isCardConversion = !isHelionConversion && player.getPlayed().containsCard(Constants.POWER_INFRASTRUCTURE_CARD_ID);

        return isHelionConversion || isCardConversion;
    }

    // Универсальный метод для вызова экшена, чтобы не писать одну и ту же простыню
    private boolean perform(Card card, InputFlag flag, int value, MarsGame game, Player player) {
        aiTurnService.performBlueAction(game, player, card.getId(), Map.of(flag.getId(), List.of(value)));
        return true;
    }

    // Поиск первой подходящей карты на столе
    private int findTargetId(List<Class<? extends Card>> targetClasses, Map<Class<?>, Card> allPlayedCards) {
        return targetClasses.stream()
                .map(allPlayedCards::get)
                .filter(Objects::nonNull)
                .map(Card::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to process action: no target cards found"));
    }

    // Маппинг ID действия на класс карты
    private Class<?> getCardClassByActionId(int actionId) {
        return switch (actionId) {
            case Frontier.GHG_PRODUCTION_ACTION_ID -> GhgProductionBacteria.class;
            case Frontier.REGOLITH_EATERS_ACTION_ID -> RegolithEaters.class;
            case Frontier.NITRITE_REDUCTION_ACTION_ID -> NitriteReductingBacteria.class;
            case Frontier.SELF_REPLICATING_BACTERIA -> SelfReplicatingBacteria.class;
            case Frontier.FIBROUS_COMPOSITE_ACTION_ID -> FibrousCompositeMaterial.class;
            default -> throw new IllegalArgumentException("Unknown action id");
        };
    }


    private BestFutureOptions projectStateAndGetBestFutureOptions(Map<State, Node> stateNodeMap, MarsGame game, Player player) {

        MarsGame gameCopy = new MarsGame(game);
        Player playerCopy = gameCopy.getPlayerByUuid(player.getUuid());

        List<Map.Entry<State, Node>> stateNodeList = new ArrayList<>();
        List<float[]> states = new ArrayList<>();

        states.add(iDataCollect.collectData(game, player));

        for (Map.Entry<State, Node> entryNode : stateNodeMap.entrySet()) {
            stateNodeList.add(entryNode);
            State deltaState = entryNode.getKey();
            applyDeltaState(playerCopy, deltaState);


            float[] data = iDataCollect.collectData(gameCopy, playerCopy);
            iDataCollect.modifyPlayerHandSize(data, deltaState.cards);
            iDataCollect.modifyPlayerWinPoints(data, (float) (deltaState.wp + deltaState.sacrificialWp) / AiConstants.WP_DENOMINATOR_FOR_FRONTIER);
            states.add(data);
            restorePlayerState(playerCopy, player);
        }

        List<Prediction> predictions = nnService.predictBatch(states, NNService.ModelType.OPTIMIZED);
        double initialProbability = predictions.removeFirst().baseProb;

        record ScoredNode(Map.Entry<State, Node> entry, double score) {
        }

        List<ScoredNode> scoredNodes = new ArrayList<>(stateNodeList.size());

        for (int i = 0; i < stateNodeList.size(); i++) {
            double newProbability = predictions.get(i).baseProb;
            if (newProbability + EPS > initialProbability) {
                scoredNodes.add(new ScoredNode(stateNodeList.get(i), newProbability));
            }
        }

        if (scoredNodes.isEmpty()) {
            return new BestFutureOptions(List.of(), 0);
        }

        // 2. Сортируем (O(N log N)) - используем примитивное сравнение для скорости
        scoredNodes.sort((a, b) -> Double.compare(b.score, a.score));

        // 3. Пересобираем итоговый список (O(N))
        List<Map.Entry<State, Node>> sortedList = new ArrayList<>(scoredNodes.size());
        for (ScoredNode sn : scoredNodes) {
            sortedList.add(sn.entry);
        }

        return new BestFutureOptions(sortedList, scoredNodes.getFirst().score);
    }

    private void applyDeltaState(Player copyPlayer, State state) {
        copyPlayer.setMc((int) (state.mc + state.extraMcValue));
        copyPlayer.setHeat(state.heat);
        copyPlayer.setPlants(state.plants);
        copyPlayer.setTerraformingRating(state.tr);
        copyPlayer.setForests(copyPlayer.getForests() + state.extraForests);

        Map<Class<?>, Integer> cardResourcesCount = copyPlayer.getCardResourcesCount();
        if (cardResourcesCount.containsKey(GhgProductionBacteria.class)) {
            cardResourcesCount.put(GhgProductionBacteria.class, state.ghgBacteriaCount);
        }
        if (cardResourcesCount.containsKey(NitriteReductingBacteria.class)) {
            cardResourcesCount.put(NitriteReductingBacteria.class, state.nitriteReductingBacteria);
        }
        if (cardResourcesCount.containsKey(RegolithEaters.class)) {
            cardResourcesCount.put(RegolithEaters.class, state.regolithEaters);
        }
        if (cardResourcesCount.containsKey(SelfReplicatingBacteria.class)) {
            cardResourcesCount.put(SelfReplicatingBacteria.class, state.selfReplicatingBacteria);
        }
        if (cardResourcesCount.containsKey(AnaerobicMicroorganisms.class)) {
            cardResourcesCount.put(AnaerobicMicroorganisms.class, state.anaerobicMicroorganisms);
        }
        if (cardResourcesCount.containsKey(BacterialAggregates.class)) {
            cardResourcesCount.put(BacterialAggregates.class, state.bacterialAggregates);
        }
        if (cardResourcesCount.containsKey(Decomposers.class)) {
            cardResourcesCount.put(Decomposers.class, state.decomposers);
        }
        if (cardResourcesCount.containsKey(DecomposingFungus.class)) {
            cardResourcesCount.put(DecomposingFungus.class, state.decomposingFungus);
        }
    }

    private void restorePlayerState(Player copyPlayer, Player originalPlayer) {
        copyPlayer.setMc(originalPlayer.getMc());
        copyPlayer.setHeat(originalPlayer.getHeat());
        copyPlayer.setPlants(originalPlayer.getPlants());
        copyPlayer.setTerraformingRating(originalPlayer.getTerraformingRating());
        copyPlayer.setForests(originalPlayer.getForests());

        Map<Class<?>, Integer> originalResourceCount = originalPlayer.getCardResourcesCount();
        if (!originalResourceCount.isEmpty()) {
            copyPlayer.setCardResourcesCount(new HashMap<>(originalResourceCount));
        }
    }

    private boolean buyCardsForMoney(MarsGame game, Player player, Map<Class<?>, Card> blueCards) {
        Card matterManufactoring = blueCards.get(MatterManufactoring.class);
        Card thinkTank = blueCards.get(ThinkTank.class);

        if (matterManufactoring == null && thinkTank == null) {
            return false;
        }

        assert matterManufactoring != null || player.getMc() > 1;
        assert thinkTank != null || player.getMc() > 2;

        if (player.getHand().isEmpty()) {
            return false;
        }

        List<Integer> badCards = getBadCardsUpToCount(game, player, 1);
        if (badCards.isEmpty()) {
            return false;
        }
        aiTurnService.sellCards(player, game, badCards);
        aiTurnService.performBlueAction(game, player, (matterManufactoring != null ? matterManufactoring : thinkTank).getId(), Map.of());
        return true;
    }


    private boolean processConservedBiomeAndResearchGrant(MarsGame game, Player player, Map<Class<?>, Card> blueCards) {
        // Мгновенный поиск вместо цикла
        Card conservedBiome = blueCards.get(ConservedBiome.class);
        Card researchGrant = blueCards.get(ResearchGrant.class);

        if (conservedBiome == null && researchGrant == null) {
            return false;
        }

        SharedInputAnalysis sharedInputAnalysis = new SharedInputAnalysis();

        if (conservedBiome != null) {
            Set<CardCollectableResource> resources = player.getPlayed().getCards().stream()
                    .map(cardService::getCard)
                    .map(Card::getCollectableResource)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // Логика: нужны животные, если они есть на столе, а микробов нет
            sharedInputAnalysis.setRequiresAnimalInput(resources.contains(CardCollectableResource.ANIMAL) && !resources.contains(CardCollectableResource.MICROBE));
        }

        if (researchGrant != null) {
            sharedInputAnalysis.setRequiresTagChoice(true);
            aiCardBuildInputAnalyzer.analyzeActiveEffectRequirements(player, List.of(AiConstants.RESEARCH_GRANT_DUMMY_CARD), sharedInputAnalysis);
        }

        OptimizedInputDecisions decisions = aiInputOptimizer.optimizeInputDecisions(game, player, sharedInputAnalysis);

        // Обработка Research Grant
        if (sharedInputAnalysis.isRequiresTagChoice() && researchGrant != null) {
            List<Tag> tagsAlreadyOnCard = player.getCardToTag().getOrDefault(ResearchGrant.class, List.of());

            // Ищем лучший тег из топа, которого еще нет на карте
            DecisionWithPrediction<Tag> bestNewTag = decisions.getBestTagDecision().stream()
                    .filter(d -> !tagsAlreadyOnCard.contains(d.getValue()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No suitable tags found for Research Grant"));

            decisions.setBestTagDecision(List.of(bestNewTag));

            CardWithInputParams cardWithInputParams = aiOptimalBuildService.generateInputBasedOnOptimizedDecisions(player, AiConstants.RESEARCH_GRANT_DUMMY_CARD, decisions);
            Map<Integer, List<Integer>> inputParams = cardWithInputParams.getInputParamsVariations().getFirst();

            aiMarsUniversityInputHandler.updateMarsUniversityInput(game, player, cardWithInputParams.getCard().getId(), inputParams);
            aiTurnService.performBlueAction(game, player, researchGrant.getId(), inputParams);
            return true;
        }

        // Обработка Conserved Biome
        if (sharedInputAnalysis.isRequiresAnimalInput() && conservedBiome != null) {
            var animalDecision = decisions.getBestResourceCardPerType().get(InputRequirementType.ANIMAL_INPUT);
            if (animalDecision != null) {
                Card bestAnimalCard = animalDecision.getValue();
                aiTurnService.performBlueAction(game, player, conservedBiome.getId(), Map.of(InputFlag.CARD_CHOICE.getId(), List.of(bestAnimalCard.getId())));
                return true;
            }
        }

        return false;
    }

    private boolean processPhaseUpgradeCards(MarsGame game, Player player, Map<Class<?>, Card> blueCards) {
        // Используем входную мапу напрямую
        Card experimentalTech = player.getTerraformingRating() > 0 ? blueCards.get(ExperimentalTechnology.class) : null;
        Card virtualEmployee = blueCards.get(VirtualEmployeeDevelopment.class);
        // Для FibrousComposite проверяем наличие 3+ ресурсов
        Card fibrousComposite = player.getCardResourcesCount().getOrDefault(FibrousCompositeMaterial.class, 0) >= 3
                ? blueCards.get(FibrousCompositeMaterial.class) : null;

        if (experimentalTech == null && virtualEmployee == null && fibrousComposite == null) {
            return false;
        }

        SharedInputAnalysis sharedInputAnalysis = new SharedInputAnalysis();
        sharedInputAnalysis.setRequiresPhaseUpgrade(true);

        OptimizedInputDecisions optimizedDecisions = aiInputOptimizer.optimizeInputDecisions(game, player, sharedInputAnalysis);

        // Тут берем лучший апгрейд фазы из оптимизатора
        var bestPhaseUpgrade = optimizedDecisions.getBestPhaseUpgradeOverall().getValue();

        List<float[]> simulationData = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();

        // Собираем данные для нейронки: текущее состояние + варианты действий
        simulationData.add(iDataCollect.collectData(game, player));

        if (experimentalTech != null) {
            simulationData.add(actionProjectionDataCollector.experimentalTechnology(game, player, bestPhaseUpgrade));
            actions.add(() -> aiTurnService.performBlueAction(game, player, experimentalTech.getId(), Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(bestPhaseUpgrade))));
        }

        if (virtualEmployee != null) {
            simulationData.add(actionProjectionDataCollector.virtualEmployee(game, player, bestPhaseUpgrade));
            actions.add(() -> aiTurnService.performBlueAction(game, player, virtualEmployee.getId(), Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(bestPhaseUpgrade))));
        }

        if (fibrousComposite != null) {
            simulationData.add(actionProjectionDataCollector.fibrousCompositeMaterial(game, player, bestPhaseUpgrade));
            actions.add(() -> aiTurnService.performBlueAction(game, player, fibrousComposite.getId(), Map.of(
                    InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(3),
                    InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(bestPhaseUpgrade))));
        }

        // Запрос в модель (Batch)
        List<Prediction> allPredictions = nnService.predictBatch(simulationData,
                player.isFirstBot() ? NNService.ModelType.BASE : NNService.ModelType.OPTIMIZED);

        double bestWinChance = allPredictions.removeFirst().baseProb; // Базовый шанс без действий
        int bestActionIdx = -1;

        for (int i = 0; i < actions.size(); i++) {
            double currentProb = allPredictions.get(i).baseProb;
            if (currentProb > bestWinChance) {
                bestWinChance = currentProb;
                bestActionIdx = i;
            }
        }

        if (bestActionIdx != -1) {
            actions.get(bestActionIdx).run();
            return true;
        }

        // Если ни одно действие не улучшило винрейт — помечаем карты как "отработанные"
        if (virtualEmployee != null) {
            markCardAsUsed(player, virtualEmployee);
        }
        if (experimentalTech != null) {
            markCardAsUsed(player, experimentalTech);
        }
        if (fibrousComposite != null) {
            // Особенность Fibrous: если не выгодно тратить 3 ресурса на апгрейд, просто добавляем 1 ресурс
            aiTurnService.performBlueAction(game, player, fibrousComposite.getId(),
                    Map.of(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(1)));
            return true;
        }

        return false;
    }

    private void markCardAsUsed(Player player, Card card) {
        player.getActivatedBlueCards().addCard(card.getId());
        player.getActivatedBlueCardsTwice().addCard(card.getId());
    }

    private void prefilterCompletelyUnusableCards(MarsGame game, Player player, Map<Class<?>, Card> blueCards) {
        Set<Class<?>> completelyUnusableCards = new HashSet<>();
        if (game.getPlanetAtTheStartOfThePhase().isOceansMax()) {
            completelyUnusableCards.add(AquiferPumping.class);
            completelyUnusableCards.add(VolcanicPools.class);
            completelyUnusableCards.add(WaterImportFromEuropa.class);
        }
        if (game.getPlanetAtTheStartOfThePhase().isTemperatureMax()) {
            completelyUnusableCards.add(DevelopedInfrastructure.class);
            completelyUnusableCards.add(WoodBurningStoves.class);
            completelyUnusableCards.add(GasCooledReactors.class);
        }
        if (game.getPlanetAtTheStartOfThePhase().isOxygenMax()) {
            completelyUnusableCards.add(IronWorks.class);
            completelyUnusableCards.add(ProgressivePolicies.class);
        }
        Set<CardCollectableResource> collectableResources = player.getPlayed().getCards().stream().map(cardService::getCard).map(Card::getCollectableResource).filter(Objects::nonNull).collect(Collectors.toSet());
        if (!collectableResources.contains(CardCollectableResource.MICROBE)) {
            completelyUnusableCards.add(SymbioticFungus.class);
            if (!collectableResources.contains(CardCollectableResource.ANIMAL)) {
                completelyUnusableCards.add(ConservedBiome.class);
            }
        }

        final List<Tag> cardTags = player.getCardToTag().getOrDefault(ResearchGrant.class, List.of());
        if (!CollectionUtils.isEmpty(cardTags) && cardTags.stream().noneMatch(tag -> tag == Tag.DYNAMIC)) {
            completelyUnusableCards.add(ResearchGrant.class);
        }

        blueCards.forEach((key, unusableCard) -> {
            if (completelyUnusableCards.contains(key)) {
                player.getActivatedBlueCards().addCard(unusableCard.getId());
                player.getActivatedBlueCardsTwice().addCard(unusableCard.getId());
            }
        });

        for (Class<?> completelyUnusableCard : completelyUnusableCards) {
            blueCards.remove(completelyUnusableCard);
        }
    }

    private boolean doImmediateUnconditionalActions(MarsGame game, Player player, Map<Class<?>, Card> availableCards) {
        for (Map.Entry<Class<?>, Card> cardEntry : availableCards.entrySet()) {
            if (DO_IMMEDIATELY_UNCONDITIONALLY.contains(cardEntry.getKey())) {
                aiTurnService.performBlueAction(game, player, cardEntry.getValue().getId(), Map.of());
                return true;
            }
        }
        return false;
    }

    private boolean doActionsWhereChoiceHasNoChoice(MarsGame game, Player player, Map<Class<?>, Card> availableCards) {
        // 1. Extreme Cold Fungus
        Card fungus = availableCards.getOrDefault(ExtremeColdFungus.class, availableCards.get(BuffedExtremeColdFungus.class));
        if (fungus != null) {
            if (player.getPlayed().getCards().stream().map(cardService::getCard).noneMatch(c -> c.getCollectableResource() == CardCollectableResource.MICROBE)) {
                aiTurnService.performBlueAction(game, player, fungus.getId(), Map.of(InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.getId(), List.of(0)));
                return true;
            }
        }

        // 2. Matter Manufacturing & Think Tank
        Card matter = availableCards.get(MatterManufactoring.class);
        if (matter != null && player.getMc() > 0) {
            aiTurnService.performBlueAction(game, player, matter.getId(), Map.of());
            return true;
        }
        Card thinkTank = availableCards.get(ThinkTank.class);
        if (thinkTank != null && player.getMc() > 1) {
            aiTurnService.performBlueAction(game, player, thinkTank.getId(), Map.of());
            return true;
        }

        // 3. Aquifer Pumping & Solar Punk
        Card aquifer = availableCards.get(AquiferPumping.class);
        if (aquifer != null && player.getSteelIncome() >= 5) {
            aiTurnService.performBlueAction(game, player, aquifer.getId(), Map.of());
            return true;
        }
        Card solarPunk = availableCards.get(SolarPunk.class);
        if (solarPunk != null && player.getTitaniumIncome() >= 8) {
            aiTurnService.performBlueAction(game, player, solarPunk.getId(), Map.of());
            return true;
        }

        // 4. Bacteria & Microbe management
        if (performMicrobeAction(game, player, availableCards, NitriteReductingBacteria.class, game.getPlanetAtTheStartOfThePhase().isOceansMax()))
            return true;
        if (performMicrobeAction(game, player, availableCards, GhgProductionBacteria.class, game.getPlanetAtTheStartOfThePhase().isTemperatureMax()))
            return true;
        if (performMicrobeAction(game, player, availableCards, BuffedGhgProductionBacteria.class, game.getPlanetAtTheStartOfThePhase().isTemperatureMax()))
            return true;
        if (performMicrobeAction(game, player, availableCards, RegolithEaters.class, game.getPlanetAtTheStartOfThePhase().isOxygenMax()))
            return true;
        if (performMicrobeAction(game, player, availableCards, BuffedRegolithEaters.class, game.getPlanetAtTheStartOfThePhase().isOxygenMax()))
            return true;

        // 5. Fibrous Composite Material
        Card fibrous = availableCards.get(FibrousCompositeMaterial.class);
        if (fibrous != null && player.getCardResourcesCount().getOrDefault(FibrousCompositeMaterial.class, 0) < 3) {
            aiTurnService.performBlueAction(game, player, fibrous.getId(), Map.of(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(1)));
            return true;
        }

        return false;
    }

    private boolean doMandatoryResourceIntoTerraformingActions(List<TurnType> possibleTurns, MarsGame game, Player player) {
        if (possibleTurns.contains(TurnType.INCREASE_INFRASTRUCTURE)) {
            aiTurnService.increaseInfrastructure(player, game, Map.of());
            return true;
        }
        if (possibleTurns.contains(TurnType.INCREASE_TEMPERATURE)) {
            aiTurnService.increaseTemperature(game, player);
            return true;
        }
        if (possibleTurns.contains(TurnType.PLANT_FOREST)) {
            aiTurnService.plantForest(game, player);
            return true;
        }
        return false;
    }

    // Вспомогательный метод, чтобы не дублировать логику по бактериям
    private boolean performMicrobeAction(MarsGame game, Player player, Map<Class<?>, Card> availableCards, Class<?> clazz, boolean condition) {
        Card card = availableCards.get(clazz);
        if (card != null && condition) {
            aiTurnService.performBlueAction(game, player, card.getId(), Map.of(InputFlag.ADD_DISCARD_MICROBE.getId(), List.of(1)));
            return true;
        }
        return false;
    }

    private boolean doRedraftedContracts(Map<Class<?>, Card> blueCards, MarsGame game, Player player, Deck activatedBlueCards) {
        if (blueCards.containsKey(RedraftedContracts.class) && !activatedBlueCards.containsCard(blueCards.get(RedraftedContracts.class).getId())) {
            List<Integer> badCards = getBadCardsUpToCount(game, player, 3);
            if (!badCards.isEmpty()) {
                aiTurnService.performBlueAction(game, player, blueCards.get(RedraftedContracts.class).getId(), Map.of(InputFlag.CARD_CHOICE.getId(), badCards));
                return true;
            }
        }
        return false;
    }


}
