package com.terraforming.ares.services.ai.network2.projection;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildDto;
import com.terraforming.ares.model.BuildType;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.network2.dto.CardWithChanceAndInput;
import com.terraforming.ares.services.ai.turnProcessors.AiUtility;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractScenarionEngine {

    @Autowired
    private AiUtility aiUtility;

    protected List<Integer> resolvePhasesToProcess(MarsGame game) {
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
        return phasesToProcess;
    }

    protected Scenario cloneAndAddAction(Scenario old, String description, Scenario.ActionType type, CardWithChanceAndInput cardData) {
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
    protected MarsGame simulateUnmi(MarsGame game, String playerUuid) { /* -6 MC, +1 TR */
        game = new MarsGame(game);
        Player player = game.getPlayerByUuid(playerUuid);

        player.setTerraformingRating(player.getTerraformingRating() + 1);
        player.setMc(player.getMc() - 6);

        player.setDidUnmiAction(true);
        player.setHasUnmiAction(false);

        return game;
    }

    protected MarsGame simulateExtraBonus(MarsGame game, String playerUuid) { /* +1 card  or +6mc*/
        game = new MarsGame(game);
        Player player = game.getPlayerByUuid(playerUuid);

        final BuildDto getCardDto = player.getBuilds().stream().filter(build -> build.getType() == BuildType.BLUE_RED_OR_CARD).findAny().orElse(null);
        if (getCardDto != null) {
            player.getHand().getCards().add(AiConstants.GENERIC_DUMMY_ID);
            player.getBuilds().remove(getCardDto);
        } else {
            final BuildDto getMcDto = player.getBuilds().stream().filter(build -> build.getType() == BuildType.BLUE_RED_OR_MC).findAny().orElse(null);

            if (getMcDto != null) {
                player.setMc(player.getMc() + 6);

                player.getBuilds().remove(getMcDto);
            }
        }
        return game;
    }

    protected MarsGame prepareForNextPhase(MarsGame game, String pUuid, int nextPhaseId) {
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

}
