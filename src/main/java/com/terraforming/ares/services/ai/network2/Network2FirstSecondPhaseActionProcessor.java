package com.terraforming.ares.services.ai.network2;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.ai.network2.buildParams.AiMarsUniversityInputHandler;
import com.terraforming.ares.services.ai.network2.dto.CardWithChanceAndInput;
import com.terraforming.ares.services.ai.network2.projection.*;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class Network2FirstSecondPhaseActionProcessor {
    private final AiTurnService aiTurnService;
    private final BatchProjectionService batchProjectionService;
    private final ScenarioEngine scenarioEngine;
    private final AiMarsUniversityInputHandler aiMarsUniversityInputHandler;
    private final Network2DraftCardsProjectionService network2DraftCardsProjectionService;
    private final BaseChanceProjectionService baseChanceProjectionService;

    public void processTurn(List<TurnType> possibleTurns, MarsGame game, Player player) {
        network2DraftCardsProjectionService.performProactiveSale(game, player);

        // 1. Генерируем все возможные цепочки действий (сценарии)
        // Внутри этого метода происходит вся рекурсия и симуляция стейтов
        List<Scenario> scenarios = scenarioEngine.generateAllScenarios(game, player);
        if (scenarios.isEmpty()) {
            aiTurnService.skipTurn(player);
            return;
        }

        if (scenarios.size() > 4999) {
            System.out.println("Generated a lot of scenarios in 1-2 phase projection: " + scenarios.size());
        }

        // 2. Подготавливаем задачи для Batch-обработки
        List<ProjectionTask<?>> allTasks = new ArrayList<>();

        // Задача для всех сценариев (оценка финальных состояний)
        ProjectionTask<List<Scenario>> scenariosTask = new ScenariosBatchTask(scenarios);
        allTasks.add(scenariosTask);

        // Базовая вероятность (Пас) — точка отсчета, чтобы понять, стоит ли вообще что-то делать
        BaseChanceProjectionTask baseTask = baseChanceProjectionService.createBaseChanceProjectionTask(game, player);
        allTasks.add(baseTask);

        // 3. Отправляем в нейронку ОДНИМ пакетом
        batchProjectionService.executeAll(player, allTasks);

        // 4. Выбираем лучшее решение
        double currentProb = baseTask.getResults();

        // Ищем сценарий, который дает максимальный прирост вероятности относительно "паса"
        Scenario bestScenario = scenarios.stream()
                .filter(s -> s.getFinalChance() > currentProb) // Только если это лучше, чем ничего не делать
                .max(Comparator.comparingDouble(Scenario::getFinalChance))
                .orElse(null);

        // 5. Исполняем решение
        if (bestScenario != null) {
//            logScenarioSelection(bestScenario, currentProb); // Опционально: лог того, что выбрал бот
            executeFirstStep(game, player, bestScenario);
        } else {
            // Если ни один сценарий не улучшил позицию — пасуем
            aiTurnService.skipTurn(player);
        }
    }

    private void executeFirstStep(MarsGame game, Player player, Scenario bestScenario) {
        switch (bestScenario.getFirstStepType()) {
            case BUILD -> finalizeBuildProject(game, player, bestScenario.getFirstStepCardData());
            case UNMI -> aiTurnService.unmiRtCorporationTurn(game, player);
            case EXTRA_BONUS -> aiTurnService.pickExtraCardTurnAsync(player);
            case SKIP -> aiTurnService.skipTurn(player);
        }
    }

    /**
     * Инкапсулированная логика донастройки параметров (Mars University и т.д.) и самого хода
     */
    private void finalizeBuildProject(MarsGame game, Player player, CardWithChanceAndInput bestCard) {
        Map<Integer, List<Integer>> params = bestCard.getInputParameters();

        // Логика Mars University
        if (params.containsKey(InputFlag.MARS_UNIVERSITY_DUMMY_INPUT.getId())) {
            aiMarsUniversityInputHandler.updateMarsUniversityInput(game, player, bestCard.getCard().getId(), params);
        }

        aiTurnService.buildProject(game, player, bestCard.getCard().getId(), bestCard.getPayments(), params);
    }

}
