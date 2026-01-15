package com.terraforming.ares.services.ai.network2.projection;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildDto;
import com.terraforming.ares.model.BuildType;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.AdvancedAiDataCollectionService;
import com.terraforming.ares.services.ai.network2.buildParams.OptimizedInputDecisions;
import com.terraforming.ares.services.ai.network2.buildParams.SharedInputAnalysis;
import com.terraforming.ares.services.ai.turnProcessors.AiUtility;
import com.terraforming.ares.services.ai.network2.dto.CardWithChanceAndInput;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScenarioEngine {
    private final CardProjectionService cardProjectionService;
    private final AdvancedAiDataCollectionService dataService;
    private final AiUtility aiUtility;

    public List<Scenario> generateAllScenarios(MarsGame game, Player player) {
        List<Scenario> results = new ArrayList<>();

        // 1. Используем Set для автоматической фильтрации дубликатов
        Set<Integer> uniquePhases = game.getPlayerUuidToPlayer()
                .values()
                .stream()
                .map(Player::getChosenPhase)
                .collect(Collectors.toSet());

        // 2. Добавляем фазу Dummy Hand, если режим активен
        if (game.isDummyHandMode() && !CollectionUtils.isEmpty(game.getUsedDummyHand())) {
            uniquePhases.add(game.getCurrentDummyHand());
        }

        List<Integer> phasesToProcess;

        if (game.getCurrentPhase() >= 3) {
            // Мы уже в 3 фазе — фаз больше нет, просто проигрываем действия
            phasesToProcess = List.of(game.getCurrentPhase());
        } else {
            phasesToProcess = uniquePhases.stream()
                    .filter(p -> (p >= game.getCurrentPhase() && p <= 2) || p == -1)
                    .sorted()
                    .toList();

            if (phasesToProcess.isEmpty() && game.getCurrentPhase() <= 2) {
                phasesToProcess = List.of(game.getCurrentPhase());
            }
        }

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
        if (player.canBuildAny(List.of(BuildType.BLUE_RED_OR_MC, BuildType.BLUE_RED_OR_CARD))
                && currentScenario.getStepsDescription().stream().noneMatch(d -> d.contains("Extra Bonus"))) {

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
                currentScenario.setFinalStateData(dataService.collectData(game, player));
                results.add(currentScenario);
            }
        }
    }

    private MarsGame prepareForNextPhase(MarsGame game, String pUuid, int nextPhaseId) {
        MarsGame nextGame = new MarsGame(game);
        Player player = nextGame.getPlayerByUuid(pUuid);

        // Сброс UNMI для новой фазы
        player.setHasUnmiAction(false);
        player.setDidUnmiAction(false);

        // Логика жетонов для Фазы 2
        if (nextPhaseId == 2) {
            List<BuildDto> builds = new ArrayList<>();
            builds.add(new BuildDto(BuildType.BLUE_RED)); // Базовый жетон

            if (player.getChosenPhase() == 2) {
                // Бонусы того, кто выбрал фазу
                if (player.hasPhaseUpgrade(Constants.PHASE_2_UPGRADE_PROJECT_AND_MC)) {
                    builds.add(new BuildDto(BuildType.BLUE_RED_OR_MC));
                } else if (player.hasPhaseUpgrade(Constants.PHASE_2_UPGRADE_PROJECT_AND_CARD)) {
                    aiUtility.addDummyCards(player, 1); // Симулируем добор
                    builds.add(new BuildDto(BuildType.BLUE_RED));
                } else {
                    builds.add(new BuildDto(BuildType.BLUE_RED_OR_CARD));
                }
            }
            player.setBuilds(builds);
        }

        return nextGame;
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
