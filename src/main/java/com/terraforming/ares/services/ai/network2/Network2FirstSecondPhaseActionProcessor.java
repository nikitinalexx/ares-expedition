package com.terraforming.ares.services.ai.network2;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.network2.buildParams.AiMarsUniversityInputHandler;
import com.terraforming.ares.services.ai.network2.projection.*;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import com.terraforming.ares.services.policyai.PolicyActionCollectService;
import com.terraforming.ares.services.policyai.PolicyCollectService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class Network2FirstSecondPhaseActionProcessor extends AbstractPhaseProcessor {
    private final AiTurnService aiTurnService;
    private final BatchProjectionService batchProjectionService;
    private final ScenarioEngine scenarioEngine;
    private final Network2DraftCardsProjectionService network2DraftCardsProjectionService;
    private final BaseChanceProjectionService baseChanceProjectionService;
    private final PolicyCollectService policyCollectService;
    private final CardService cardService;

    public Network2FirstSecondPhaseActionProcessor(Network2PaymentService network2PaymentService, AiTurnService aiTurnService, BatchProjectionService batchProjectionService, ScenarioEngine scenarioEngine, AiMarsUniversityInputHandler aiMarsUniversityInputHandler, Network2DraftCardsProjectionService network2DraftCardsProjectionService, BaseChanceProjectionService baseChanceProjectionService, PolicyCollectService policyCollectService, CardService cardService, PolicyActionCollectService policyActionCollectService) {
        super(policyCollectService, policyActionCollectService, aiTurnService, aiMarsUniversityInputHandler, network2PaymentService);
        this.aiTurnService = aiTurnService;
        this.batchProjectionService = batchProjectionService;
        this.scenarioEngine = scenarioEngine;
        this.network2DraftCardsProjectionService = network2DraftCardsProjectionService;
        this.baseChanceProjectionService = baseChanceProjectionService;
        this.policyCollectService = policyCollectService;
        this.cardService = cardService;
    }

    public void processTurn(List<TurnType> possibleTurns, MarsGame game, Player player) {
        // 1. Генерируем все возможные цепочки действий (сценарии)
        // Внутри этого метода происходит вся рекурсия и симуляция стейтов
        List<Scenario> scenarios = scenarioEngine.generateAllScenarios(game, player);
        if (scenarios.isEmpty()) {
            skipTurn(game, player);
            return;
        }

        if (scenarios.size() > 49999) {
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
            skipTurn(game, player);
        }
    }

    private void skipTurn(MarsGame game, Player player) {
        policyCollectService.skipTurn(game, player);
        // Если ни один сценарий не улучшил позицию — пасуем
        aiTurnService.skipTurn(player);
    }

    private void executeFirstStep(MarsGame game, Player player, Scenario bestScenario) {
        switch (bestScenario.getFirstStepType()) {
            case BUILD -> finalizeBuildProject(game, player, bestScenario.getFirstStepCardData(), false);
            case UNMI -> finalizeUnmiTurn(game, player);
            case EXTRA_BONUS -> finalizeDoExtraAction(game, player);
            case SKIP -> skipTurn(game, player);
        }
    }



}
