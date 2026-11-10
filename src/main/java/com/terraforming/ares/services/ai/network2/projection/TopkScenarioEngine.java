package com.terraforming.ares.services.ai.network2.projection;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildType;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService;
import com.terraforming.ares.services.ai.dl4j.NNService;
import com.terraforming.ares.services.ai.dl4j.Prediction;
import com.terraforming.ares.services.ai.network2.buildParams.OptimizedInputDecisions;
import com.terraforming.ares.services.ai.network2.buildParams.SharedInputAnalysis;
import com.terraforming.ares.services.ai.network2.dto.CardWithChanceAndInput;
import com.terraforming.ares.services.ai.turnProcessors.AiUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TopkScenarioEngine extends AbstractScenarionEngine {
    private final CardProjectionService cardProjectionService;
    private final AdvancedAiDataCollectionService dataService;
    private final AiUtility aiUtility;
    private final NNService nnService;

    private static final int BEAM_WIDTH = 999999999;

    // --- Inner class ---
    private static class BeamNode {
        final MarsGame game;
        final Scenario scenario;
        final String pUuid;
        double score;
        final Map<SharedInputAnalysis, OptimizedInputDecisions> optimizedInputs;

        BeamNode(MarsGame game, Scenario scenario, String pUuid, double score,
                 Map<SharedInputAnalysis, OptimizedInputDecisions> optimizedInputs) {
            this.game = game;
            this.scenario = scenario;
            this.pUuid = pUuid;
            this.score = score;
            this.optimizedInputs = optimizedInputs;
        }
    }

    public List<Scenario> generateAllScenarios(MarsGame game, Player player) {

        List<Scenario> results = new ArrayList<>();
        List<Integer> phasesToProcess = resolvePhasesToProcess(game);

        MarsGame simulationGame = new MarsGame(game);

        List<ExplorationState> currentLayer = new ArrayList<>();

        currentLayer.add(new ExplorationState(
                simulationGame,
                player.getUuid(),
                new Scenario(),
                0
        ));

        Map<SharedInputAnalysis, OptimizedInputDecisions> optimizedInputs = new HashMap<>();

        while (!currentLayer.isEmpty()) {

            List<ExplorationState> nextLayer = new ArrayList<>();

            for (ExplorationState state : currentLayer) {

                MarsGame gameState = state.game;
                Scenario scenario = state.scenario;
                int phaseIdx = state.phaseIdx;

                Player p = gameState.getPlayerByUuid(state.playerUuid);

                // --- BUILD ---
                if (!p.cantBuildAnything()) {

                    List<CardWithChanceAndInput> validCards =
                            cardProjectionService.getAvailableProjectsSync(gameState, p, optimizedInputs);

                    for (CardWithChanceAndInput cardProj : validCards) {

                        MarsGame nextGame =
                                cardProjectionService.projectBuildCardWithRequirements(
                                        gameState, p, cardProj);

                        Scenario nextScenario = cloneAndAddAction(
                                scenario,
                                "Build " + cardProj.getCard().getCardMetadata().getName(),
                                Scenario.ActionType.BUILD,
                                cardProj
                        );

                        nextLayer.add(new ExplorationState(
                                nextGame,
                                state.playerUuid,
                                nextScenario,
                                phaseIdx
                        ));
                    }
                }

                // --- UNMI ---
                if (p.isHasUnmiAction() && !p.isDidUnmiAction() && p.getMc() >= 6) {

                    MarsGame nextGame = simulateUnmi(gameState, state.playerUuid);

                    Scenario nextScenario = cloneAndAddAction(
                            scenario,
                            "UNMI",
                            Scenario.ActionType.UNMI,
                            null
                    );

                    nextLayer.add(new ExplorationState(
                            nextGame,
                            state.playerUuid,
                            nextScenario,
                            phaseIdx
                    ));
                }

                // --- EXTRA BONUS ---
                if (p.canBuildAny(List.of(BuildType.BLUE_RED_OR_MC, BuildType.BLUE_RED_OR_CARD))) {

                    MarsGame nextGame = simulateExtraBonus(gameState, state.playerUuid);

                    Scenario nextScenario = cloneAndAddAction(
                            scenario,
                            "Extra Bonus",
                            Scenario.ActionType.EXTRA_BONUS,
                            null
                    );

                    nextLayer.add(new ExplorationState(
                            nextGame,
                            state.playerUuid,
                            nextScenario,
                            phaseIdx
                    ));
                }

                // --- NEXT PHASE ---
                if (phaseIdx < phasesToProcess.size() - 1) {

                    MarsGame nextPhaseGame = prepareForNextPhase(
                            gameState,
                            state.playerUuid,
                            phasesToProcess.get(phaseIdx + 1)
                    );

                    Scenario nextScenario = scenario;

                    if (scenario.getFirstStepType() == null) {
                        nextScenario = cloneAndAddAction(
                                scenario,
                                "P" + phasesToProcess.get(phaseIdx) + ": SKIP (Wait for next phase)",
                                Scenario.ActionType.SKIP,
                                null
                        );
                    }

                    nextLayer.add(new ExplorationState(
                            nextPhaseGame,
                            state.playerUuid,
                            nextScenario,
                            phaseIdx + 1
                    ));
                } else {

                    if (scenario.getFirstStepType() != null) {

                        scenario.setFinalStateData(
                                dataService.collectData(gameState, state.playerUuid)
                        );

                        results.add(scenario);
                    }
                }
            }

            if (nextLayer.isEmpty()) {
                break;
            }

            // --- SCORE ---
            scoreCandidates(nextLayer);

            // --- SORT ---
            nextLayer.sort((a, b) -> Double.compare(b.score, a.score));

            // --- CUT TO K ---
            if (nextLayer.size() > BEAM_WIDTH) {
                nextLayer = new ArrayList<>(nextLayer.subList(0, BEAM_WIDTH));
            }

            currentLayer = nextLayer;
        }

        return results;
    }

//    public List<Scenario> generateAllScenarios(MarsGame game, Player player) {
//        List<Scenario> results = new ArrayList<>();
//
//
//        List<Integer> phasesToProcess = resolvePhasesToProcess(game);
//
//        List<BeamNode> activeBeam = new ArrayList<>();
//        activeBeam.add(new BeamNode(new MarsGame(game), new Scenario(), player.getUuid(), 0.0, new HashMap<>()));
//
//        for (int phaseIdx = 0; phaseIdx < phasesToProcess.size(); phaseIdx++) {
//            boolean isLastPhase = (phaseIdx == phasesToProcess.size() - 1);
//
//            // --- Внутренний цикл внутри фазы ---
//            while (true) {
//
//                List<BeamNode> nextBeam = new ArrayList<>();
//                List<BeamNode> candidates = new ArrayList<>();
//
//                boolean producedChildren = false;
//
//                for (BeamNode node : activeBeam) {
//
//                    boolean hasRealAction =
//                            node.scenario.getFirstStepType() != null &&
//                                    node.scenario.getFirstStepType() != Scenario.ActionType.SKIP;
//
//                    // если уже есть реальное действие — больше не расширяем
//                    if (hasRealAction) {
//                        nextBeam.add(node);
//                        continue;
//                    }
//
//                    List<BeamNode> children = expandNode(node);
//
//                    if (!children.isEmpty()) {
//                        candidates.addAll(children);
//                        producedChildren = true;
//                    }
//
//                    // root сохраняем для возможности SKIP
//                    nextBeam.add(node);
//                }
//
//                if (!producedChildren) {
//                    activeBeam = nextBeam;
//                    break;
//                }
//
//                scoreCandidates(candidates);
//
//                List<BeamNode> all = new ArrayList<>(nextBeam);
//                all.addAll(candidates);
//
//                activeBeam = all.stream()
//                        .sorted(Comparator.comparingDouble(n -> -n.score))
//                        .limit(BEAM_WIDTH)
//                        .toList();
//            }
//
//            // --- Конец фазы ---
//            if (isLastPhase) {
//                for (BeamNode node : activeBeam) {
//                    Scenario scenario = node.scenario;
//                    // Только если первый шаг уже определён — добавляем в результаты
//                    // SKIP без последующих действий не добавляем, как в оригинале
//                    if (scenario.getFirstStepType() != null) {
//                        scenario.setFinalStateData(dataService.collectData(node.game, node.pUuid));
//                        results.add(scenario);
//                    }
//                }
//            } else {
//                List<BeamNode> nextPhaseBeam = new ArrayList<>();
//                for (BeamNode node : activeBeam) {
//                    Scenario scenario = node.scenario;
//                    if (scenario.getFirstStepType() == null) {
//                        scenario = cloneAndAddAction(scenario,
//                                "P" + phasesToProcess.get(phaseIdx) + ": SKIP (Wait for next phase)",
//                                Scenario.ActionType.SKIP, null);
//                    }
//                    MarsGame nextPhaseGame = prepareForNextPhase(node.game, node.pUuid, phasesToProcess.get(phaseIdx + 1));
//                    nextPhaseBeam.add(new BeamNode(nextPhaseGame, scenario, node.pUuid, node.score, node.optimizedInputs));
//                }
//                activeBeam = nextPhaseBeam;
//            }
//        }
//
//        return results;
//    }

    private List<BeamNode> expandNode(BeamNode node) {
        List<BeamNode> children = new ArrayList<>();
        MarsGame game = node.game;
        Player player = game.getPlayerByUuid(node.pUuid);
        Scenario scenario = node.scenario;

        // Ветка BUILD
        if (!player.cantBuildAnything()) {
            List<CardWithChanceAndInput> validCards =
                    cardProjectionService.getAvailableProjectsSync(game, player, node.optimizedInputs);
            for (CardWithChanceAndInput cardProj : validCards) {
                MarsGame nextGame = cardProjectionService.projectBuildCardWithRequirements(game, player, cardProj);
                Scenario nextScenario = cloneAndAddAction(scenario,
                        "Build " + cardProj.getCard().getCardMetadata().getName(),
                        Scenario.ActionType.BUILD, cardProj);
                children.add(new BeamNode(nextGame, nextScenario, node.pUuid, 0.0, node.optimizedInputs));
            }
        }

        // Ветка UNMI
        if (player.isHasUnmiAction() && !player.isDidUnmiAction() && player.getMc() >= 6) {
            MarsGame nextGame = simulateUnmi(game, node.pUuid);
            Scenario nextScenario = cloneAndAddAction(scenario, "UNMI", Scenario.ActionType.UNMI, null);
            children.add(new BeamNode(nextGame, nextScenario, node.pUuid, 0.0, node.optimizedInputs));
        }

        // Ветка EXTRA_BONUS
        if (player.canBuildAny(List.of(BuildType.BLUE_RED_OR_MC, BuildType.BLUE_RED_OR_CARD))) {
            MarsGame nextGame = simulateExtraBonus(game, node.pUuid);
            Scenario nextScenario = cloneAndAddAction(scenario, "Extra Bonus", Scenario.ActionType.EXTRA_BONUS, null);
            children.add(new BeamNode(nextGame, nextScenario, node.pUuid, 0.0, node.optimizedInputs));
        }

        return children;
    }

    private void scoreCandidates(List<ExplorationState> candidates) {
        if (candidates.isEmpty()) return;

        // Берём player из первого узла — все узлы принадлежат одному игроку
        Player player = candidates.get(0).game.getPlayerByUuid(candidates.get(0).playerUuid);

        List<float[]> inputs = candidates.stream()
                .map(n -> dataService.collectData(n.game, n.playerUuid))
                .toList();

        List<Prediction> predictions = nnService.predictBatch(inputs, player);

        for (int i = 0; i < candidates.size(); i++) {
            candidates.get(i).score = predictions.get(i).baseProb;
        }
    }

    private static class ExplorationState {

        MarsGame game;
        String playerUuid;
        Scenario scenario;
        int phaseIdx;
        double score;

        public ExplorationState(MarsGame game, String playerUuid, Scenario scenario, int phaseIdx) {
            this.game = game;
            this.playerUuid = playerUuid;
            this.scenario = scenario;
            this.phaseIdx = phaseIdx;
        }
    }
}