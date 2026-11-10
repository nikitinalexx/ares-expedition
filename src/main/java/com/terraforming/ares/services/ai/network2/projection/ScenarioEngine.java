package com.terraforming.ares.services.ai.network2.projection;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildType;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService;
import com.terraforming.ares.services.ai.network2.buildParams.OptimizedInputDecisions;
import com.terraforming.ares.services.ai.network2.buildParams.SharedInputAnalysis;
import com.terraforming.ares.services.ai.network2.dto.CardWithChanceAndInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ScenarioEngine extends AbstractScenarionEngine {
    private final CardProjectionService cardProjectionService;
    private final AdvancedAiDataCollectionService dataService;

    public List<Scenario> generateAllScenarios(MarsGame game, Player player) {
        List<Scenario> results = new ArrayList<>();

        List<Integer> phasesToProcess = resolvePhasesToProcess(game);

        MarsGame simulationGame = new MarsGame(game);
        // Запускаем рекурсию с указанием индекса текущей фазы в списке задач
        explore(simulationGame, player.getUuid(), new Scenario(), phasesToProcess, 0, results, new HashMap<>());

        return results;
    }

    private void explore(MarsGame game, String pUuid, Scenario currentScenario,
                         List<Integer> phasesToProcess,
                         int phaseIdx,
                         List<Scenario> results,
                         Map<SharedInputAnalysis, OptimizedInputDecisions> optimizedInputs) {
        Player player = game.getPlayerByUuid(pUuid);

        // --- ВЕТКА 1: ПОСТРОЙКА КАРТ ---
        if (!player.cantBuildAnything()) {
            // Используем вашу логику фильтрации, но на ТЕКУЩЕМ (симулированном) стейте
            List<CardWithChanceAndInput> validCards = cardProjectionService.getAvailableProjectsSync(game, player, optimizedInputs);

            for (CardWithChanceAndInput cardProj : validCards) {

                // Симулируем шаг
                MarsGame nextGame = cardProjectionService.projectBuildCardWithRequirements(game, player, cardProj);
                Scenario nextScenario = cloneAndAddAction(currentScenario,
                        "Build " + cardProj.getCard().getCardMetadata().getName(),
                        Scenario.ActionType.BUILD,
                        cardProj); // логика как в вашем processTurn

                // Рекурсия (жетоны постройки уменьшатся внутри projectBuildCardWithRequirements автоматически)
                explore(nextGame, pUuid, nextScenario, phasesToProcess, phaseIdx, results, optimizedInputs);
            }
        }

        // --- ВЕТКА 2: UNMI (один раз за фазу/ход) ---
        if (player.isHasUnmiAction() && !player.isDidUnmiAction() && player.getMc() >= 6) {

            MarsGame nextGame = simulateUnmi(game, player.getUuid());
            Scenario nextScenario = cloneAndAddAction(currentScenario,
                    "UNMI",
                    Scenario.ActionType.UNMI,
                    null);

            explore(nextGame, pUuid, nextScenario, phasesToProcess, phaseIdx, results, optimizedInputs);
        }

        // --- ВЕТКА 3: БОНУС ВТОРОЙ ФАЗЫ ---
        if (player.canBuildAny(List.of(BuildType.BLUE_RED_OR_MC, BuildType.BLUE_RED_OR_CARD))) {

            MarsGame nextGame = simulateExtraBonus(game, player.getUuid());
            Scenario nextScenario = cloneAndAddAction(currentScenario,
                    "Extra Bonus",
                    Scenario.ActionType.EXTRA_BONUS,
                    null);

            explore(nextGame, pUuid, nextScenario, phasesToProcess, phaseIdx, results, optimizedInputs);
        }

        // --- ПЕРЕХОД К СЛЕДУЮЩЕЙ ФАЗЕ ---
        if (phaseIdx < phasesToProcess.size() - 1) {
            MarsGame nextPhaseGame = prepareForNextPhase(game, pUuid, phasesToProcess.get(phaseIdx + 1));

            Scenario nextScenario = currentScenario;

            // КЛЮЧЕВОЙ МОМЕНТ:
            // Если мы переходим в следующую фазу, а первое действие еще не было выбрано,
            // значит ПЕРВЫМ действием в этом сценарии для текущей фазы является ПАС.
            if (currentScenario.getFirstStepType() == null) {
                nextScenario = cloneAndAddAction(currentScenario, "P" + phasesToProcess.get(phaseIdx) + ": SKIP (Wait for next phase)", Scenario.ActionType.SKIP, null);
            }

            explore(nextPhaseGame, pUuid, nextScenario, phasesToProcess, phaseIdx + 1, results, optimizedInputs);
        } else {
            // Конец цепочки фаз
            // Теперь здесь всегда будет либо действие из 1 фазы, либо SKIP из 1 фазы
            if (currentScenario.getFirstStepType() != null) {
                currentScenario.setFinalStateData(dataService.collectData(game, player.getUuid()));
                results.add(currentScenario);
            }
        }
    }

}
