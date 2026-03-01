package com.terraforming.ares.services.ai.network2.projection;

import com.terraforming.ares.cards.blue.ConservedBiome;
import com.terraforming.ares.cards.blue.ExtremeColdFungus;
import com.terraforming.ares.cards.blue.SelfReplicatingBacteria;
import com.terraforming.ares.cards.blue.SymbioticFungus;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.network2.AbstractPhaseProcessor;
import com.terraforming.ares.services.ai.network2.Network2PaymentService;
import com.terraforming.ares.services.ai.network2.buildParams.AiMarsUniversityInputHandler;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import com.terraforming.ares.services.policyai.PolicyActionCollectService;
import com.terraforming.ares.services.policyai.PolicyCollectService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class SelfReplicatingBacteriaService extends AbstractPhaseProcessor {
    private final ScenarioEngine scenarioEngine;
    private final BatchProjectionService batchProjectionService;

    private final Map<Class<?>, Integer> CARD_TO_INPUT_FLAG = Map.of(
            SymbioticFungus.class, InputFlag.CARD_CHOICE.getId(),
            ExtremeColdFungus.class, InputFlag.EXTREME_COLD_FUNGUS_PUT_MICROBE.getId(),
            ConservedBiome.class, InputFlag.CARD_CHOICE.getId(),
            SelfReplicatingBacteria.class, InputFlag.ADD_DISCARD_MICROBE.getId()
    );

    public SelfReplicatingBacteriaService(Network2PaymentService network2PaymentService, AiTurnService aiTurnService, ScenarioEngine scenarioEngine, BatchProjectionService batchProjectionService, AiMarsUniversityInputHandler aiMarsUniversityInputHandler, PolicyCollectService policyCollectService, PolicyActionCollectService policyActionCollectService) {
        super(policyCollectService, policyActionCollectService, aiTurnService, aiMarsUniversityInputHandler, network2PaymentService);
        this.scenarioEngine = scenarioEngine;
        this.batchProjectionService = batchProjectionService;
    }

    public MarsGame simulateSelfReplicatingBacteriaFinalActions(Map<Class<?>, Card> blueCards, MarsGame game, Player player) {
        MarsGame simGame = new MarsGame(game);
        Player simPlayer = simGame.getPlayerByUuid(player.getUuid());

        return doSelfReplicatingBacteria(blueCards, simGame, simPlayer, true);
    }

    public void doSelfReplicatingBacteriaFinalActions(Map<Class<?>, Card> blueCards, MarsGame game, Player player) {
        doSelfReplicatingBacteria(blueCards, game, player, false);
    }

    private MarsGame doSelfReplicatingBacteria(Map<Class<?>, Card> blueCards, MarsGame simGame, Player simPlayer, boolean isSimulation) {
        if (!blueCards.containsKey(SelfReplicatingBacteria.class)) return null;

        Card selfCard = blueCards.get(SelfReplicatingBacteria.class);
        int extraLeft = simPlayer.getBlueActionExtraActivationsLeft();

        // Список ВНЕШНИХ доноров (без самой SelfReplicating)
        List<Class<?>> externalDonors = List.of(
                SymbioticFungus.class,
                ExtremeColdFungus.class,
                ConservedBiome.class
        );

        // --- ШАГ 1: Собираем микробы со всех внешних доноров (первая активация) ---
        for (Class<?> clazz : externalDonors) {
            if (blueCards.containsKey(clazz)) {
                Card donor = blueCards.get(clazz);
                if (canDoFirstActivation(simPlayer, donor) && getMicrobes(simPlayer) < 5) {
                    executeMicrobeAction(simGame, simPlayer, donor, selfCard, isSimulation);
                }
            }
        }

        // --- ШАГ 2: Собираем микробы через EXTRA активации внешних доноров ---
        for (Class<?> clazz : externalDonors) {
            if (blueCards.containsKey(clazz)) {
                Card donor = blueCards.get(clazz);
                if (extraLeft > 0 && getMicrobes(simPlayer) < 5 && canDoExtraActivation(simPlayer, donor)) {
                    executeMicrobeAction(simGame, simPlayer, donor, selfCard, isSimulation);
                    extraLeft--;
                }
            }
        }

        // --- ШАГ 3: Решаем судьбу активации самой SelfReplicatingBacteria ---
        int current = getMicrobes(simPlayer);

        // Если нам не хватает 1 микроба до 5, мы обязаны сделать базовую активацию, доп активация здесь уже никак не поможет
        if (current == 4 && canDoFirstActivation(simPlayer, selfCard)) {
            executeMicrobeAction(simGame, simPlayer, selfCard, selfCard, isSimulation);// Сама на себя (+1)
            current++;
        }

        // Если у нас уже есть 5 микробов и либо основное, либо доп действие, то делаем
        if (current >= 5) {
            if (canDoFirstActivation(simPlayer, selfCard) || extraLeft > 0 && canDoExtraActivation(simPlayer, selfCard)) {
                executeSelfReplicateFinalAction(simGame, simPlayer, selfCard, isSimulation);
                return buildProjectScenario(simGame, simPlayer, isSimulation);
            }
        }

        return null;
    }

    private MarsGame buildProjectScenario(MarsGame game, Player player, boolean isSimulation) {
        boolean builtSomething = false;

        while (true) {
            List<Scenario> scenarios = scenarioEngine.generateAllScenarios(game, player);
            if (scenarios.isEmpty()) {
                break;
            }

            // 2. Подготавливаем задачи для Batch-обработки
            List<ProjectionTask<?>> allTasks = new ArrayList<>();

            // Задача для всех сценариев (оценка финальных состояний)
            ProjectionTask<List<Scenario>> scenariosTask = new ScenariosBatchTask(scenarios);
            allTasks.add(scenariosTask);

            // Базовая вероятность (Пас) — точка отсчета, чтобы понять, стоит ли вообще что-то делать
//        BaseChanceProjectionTask baseTask = baseChanceProjectionService.createBaseChanceProjectionTask(game, player);
//        allTasks.add(baseTask);

            // 3. Отправляем в нейронку ОДНИМ пакетом
            batchProjectionService.executeAll(player, allTasks);

            // Ищем сценарий, который дает максимальный прирост вероятности
            Scenario bestScenario = scenarios.stream()
                    .max(Comparator.comparingDouble(Scenario::getFinalChance))
                    .orElse(null);

            if (bestScenario != null && bestScenario.getFirstStepType() == Scenario.ActionType.SKIP) {
                break;
            } else if (bestScenario != null && bestScenario.getFirstStepType() == Scenario.ActionType.BUILD) {
                builtSomething = true;
                finalizeBuildProject(game, player, bestScenario.getFirstStepCardData(), isSimulation);
            } else if (bestScenario != null && bestScenario.getFirstStepType() == Scenario.ActionType.UNMI) {
                finalizeUnmiTurn(game, player, isSimulation);
            } else if (bestScenario != null && bestScenario.getFirstStepType() == Scenario.ActionType.EXTRA_BONUS) {
                int handSizeBeforeAction = player.getHand().size();
                finalizeDoExtraAction(game, player, isSimulation);
                if (isSimulation && (player.getHand().size() > handSizeBeforeAction)) {
                    player.getHand().getCards().removeLast();
                    player.getHand().addCard(AiConstants.GENERIC_DUMMY_ID);
                }
            } else if (bestScenario == null) {
                break;
            } else {
                throw new IllegalStateException("Invalid action during Self Replicating Bacteria execution " + bestScenario);
            }
        }

        if (builtSomething) {
            return game;
        } else {
            return null;
        }
    }

    private void executeMicrobeAction(MarsGame game, Player player, Card donor, Card selfCard, boolean isSimulation) {
        finalizeBlueAction(game, player, donor, Map.of(CARD_TO_INPUT_FLAG.get(donor.getClass()), List.of((donor == selfCard ? 1 : selfCard.getId()))), isSimulation);
    }

    private void executeSelfReplicateFinalAction(MarsGame game, Player player, Card selfCard, boolean isSimulation) {
        finalizeBlueAction(game, player, selfCard, Map.of(CARD_TO_INPUT_FLAG.get(selfCard.getClass()), List.of(5)), isSimulation);
    }

    private int getMicrobes(Player p) {
        return p.getCardResourcesCount().getOrDefault(SelfReplicatingBacteria.class, 0);
    }

    private boolean canDoFirstActivation(Player p, Card c) {
        return !p.getActivatedBlueCards().containsCard(c.getId());
    }

    private boolean canDoExtraActivation(Player p, Card c) {
        // Карта уже была активирована один раз, но еще не дважды
        return p.getActivatedBlueCards().containsCard(c.getId()) && !p.getActivatedBlueCardsTwice().containsCard(c.getId());
    }


}
