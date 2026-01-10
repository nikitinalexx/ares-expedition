package com.terraforming.ares.services.ai.turnProcessors.network2.projection;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildDto;
import com.terraforming.ares.model.BuildType;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService;
import com.terraforming.ares.services.ai.turnProcessors.network2.dto.CardWithChanceAndInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScenarioEngineBackup {
    private final CardProjectionService cardProjectionService;
    private final AdvancedAiDataCollectionService dataService;

    public List<Scenario> generateAllScenarios(MarsGame game, Player player, List<TurnType> possibleTurns) {
        List<Scenario> results = new ArrayList<>();

        // Создаем "нулевой" сценарий (точку отсчета)
        Scenario rootScenario = new Scenario();

        // Клонируем игру для симуляции
        MarsGame simulationGame = new MarsGame(game);

        // Запускаем рекурсию
        explore(simulationGame, player.getUuid(), rootScenario, possibleTurns, results);

        return results;
    }

    private void explore(MarsGame game, String pUuid, Scenario currentScenario, List<TurnType> possibleTurns, List<Scenario> results) {

        Player player = game.getPlayerByUuid(pUuid);
        boolean canDoAnything = false;

        // --- ВЕТКА 1: ПОСТРОЙКА КАРТ ---
        if (!player.cantBuildAnything()) {
            // Используем вашу логику фильтрации, но на ТЕКУЩЕМ (симулированном) стейте
            List<CardWithChanceAndInput> validCards = cardProjectionService.getAvailableProjectsSync(game, player);

            for (CardWithChanceAndInput cardProj : validCards) {
                canDoAnything = true;

                // Симулируем шаг
                MarsGame nextGame = cardProjectionService.projectBuildCardWithRequirements(game, player, cardProj);
                Scenario nextScenario = cloneAndAddAction(currentScenario,
                        "Build " + cardProj.getCard().getCardMetadata().getName(),
                        Scenario.ActionType.BUILD,
                        cardProj); // логика как в вашем processTurn

                // Рекурсия (жетоны постройки уменьшатся внутри projectBuildCardWithRequirements автоматически)
                explore(nextGame, pUuid, nextScenario, possibleTurns, results);
            }
        }

        // --- ВЕТКА 2: UNMI (один раз за фазу/ход) ---
        if (possibleTurns.contains(TurnType.UNMI_RT) && player.getMc() >= 6
                && currentScenario.getStepsDescription().stream().noneMatch(d -> d.contains("UNMI"))) {

            canDoAnything = true;
            MarsGame nextGame = simulateUnmi(game, player.getUuid());
            Scenario nextScenario = cloneAndAddAction(currentScenario,
                    "UNMI",
                    Scenario.ActionType.UNMI,
                    null);

            explore(nextGame, pUuid, nextScenario, possibleTurns, results);
        }

        // --- ВЕТКА 3: БОНУС ВТОРОЙ ФАЗЫ ---
        if (possibleTurns.contains(TurnType.PICK_EXTRA_BONUS_SECOND_PHASE)
                && currentScenario.getStepsDescription().stream().noneMatch(d -> d.contains("Extra Bonus"))) {

            canDoAnything = true;
            MarsGame nextGame = simulateExtraBonus(game, player.getUuid());
            Scenario nextScenario = cloneAndAddAction(currentScenario,
                    "Extra Bonus",
                    Scenario.ActionType.EXTRA_BONUS,
                    null);

            explore(nextGame, pUuid, nextScenario, possibleTurns, results);
        }

        // Если больше нечего делать — это "лист" дерева, сохраняем для оценки
        if (!canDoAnything) {
            if (currentScenario.getFirstStepType() != null) {
                currentScenario.setFinalStateData(dataService.collectData(game, game.getPlayerByUuid(pUuid)));
                results.add(currentScenario);
            }
        }
    }

    private Scenario cloneAndAddAction(Scenario old, String description, Scenario.ActionType type, CardWithChanceAndInput cardData) {
        // Создаем новый объект сценария
        Scenario next = new Scenario();

        // 1. Копируем историю шагов (для дебага)
        next.getStepsDescription().addAll(old.getStepsDescription());
        next.getStepsDescription().add(description);

        // 2. Если это самый первый шаг в цепочке — запоминаем его детали
        if (old.getStepsDescription().isEmpty()) {
            next.setFirstStepType(type);
            next.setFirstStepCardData(cardData);
        } else {
            // Если это второй/третий шаг — просто наследуем инфу о первом шаге
            next.setFirstStepType(old.getFirstStepType());
            next.setFirstStepCardData(old.getFirstStepCardData());
        }

        return next;
    }

    // Вспомогательные методы симуляции (простой сдвиг стейта)
    private MarsGame simulateUnmi(MarsGame game, String playerUuid) { /* -6 MC, +1 TR */
        game = new MarsGame(game);
        Player player = game.getPlayerByUuid(playerUuid);

        player.setTerraformingRating(player.getTerraformingRating() + 1);
        player.setMc(player.getMc() - 6);

        player.setDidUnmiAction(true);
        player.setHasUnmiAction(false);

        return game;
    }

    private MarsGame simulateExtraBonus(MarsGame game, String playerUuid) { /* +1 card  or +6mc*/
        game = new MarsGame(game);
        Player player = game.getPlayerByUuid(playerUuid);

        final BuildDto getCardDto = player.getBuilds().stream().filter(build -> build.getType() == BuildType.BLUE_RED_OR_CARD).findAny().orElse(null);
        if (getCardDto != null) {
            player.getHand().getCards().add(AiConstants.GENERIC_DUMMY_ID);
        } else {
            final BuildDto getMcDto = player.getBuilds().stream().filter(build -> build.getType() == BuildType.BLUE_RED_OR_MC).findAny().orElse(null);

            if (getMcDto != null) {
                player.setMc(player.getMc() + 6);

                player.getBuilds().remove(getMcDto);
            }
        }
        return game;
    }

}
