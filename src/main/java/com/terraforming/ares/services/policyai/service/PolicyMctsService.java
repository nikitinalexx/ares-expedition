package com.terraforming.ares.services.policyai.service;

import com.terraforming.ares.factories.StateFactory;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.model.turn.PhaseChoiceTurn;
import com.terraforming.ares.processors.turn.TurnProcessor;
import com.terraforming.ares.services.*;
import com.terraforming.ares.services.ai.AiService;
import com.terraforming.ares.services.ai.dto.MctsDecision;
import com.terraforming.ares.services.policyai.GameArenaContext;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.encoder.PolicyTableAndHandEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class PolicyMctsService extends BaseProcessorService {
    public static final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    private final AiService aiService;
    private final StateFactory stateFactory;
    private final PolicyActionSelector policyActionSelector;
    private final PolicyTableAndHandEncoder encoder;
    private final CardService cardService;
    private final WinPointsService winPointsService;
    public static final double TAU = 1.0f;


    protected PolicyMctsService(TurnTypeService turnTypeService, StateFactory stateFactory, StateContextProvider stateContextProvider, List<TurnProcessor<?>> turnProcessor, AiService aiService, StateFactory stateFactory1, PolicyActionSelector policyActionSelector, PolicyTableAndHandEncoder encoder, CardService cardService, WinPointsService winPointsService) {
        super(turnTypeService, stateFactory, stateContextProvider, turnProcessor);
        this.aiService = aiService;
        this.stateFactory = stateFactory1;
        this.policyActionSelector = policyActionSelector;
        this.encoder = encoder;
        this.cardService = cardService;
        this.winPointsService = winPointsService;
    }

//    public MctsDecision runMctsPhaseSelection(Map<Integer, Float> policyProbs, MarsGame game, String playerUuid, String anotherPlayerUuid) {
//        int totalIterations = 100;
//        int batchSize = 10;
//
//        // Мапы для сбора статистики (теперь должны быть потокобезопасными или собираться в конце)
//        // Используем Concurrent для безопасности, хотя при параллельном запуске итераций мы соберем их через Future
//        Map<Integer, Double> phaseToSumQ = new ConcurrentHashMap<>();
//        Map<Integer, Integer> phaseToVisits = new ConcurrentHashMap<>();
//
//        for (Integer phase : policyProbs.keySet()) {
//            phaseToSumQ.put(phase, 0.0);
//            phaseToVisits.put(phase, 0);
//        }
//
//        // Используем Joiner, который просто ждет завершения всех подзадач.
//        // Если кто-то упадет, он пробросит ошибку.
//        for (int b = 0; b < totalIterations / batchSize; b++) {
//            try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.<MctsResult>awaitAll())) {
//                List<StructuredTaskScope.Subtask<MctsResult>> subtasks = new ArrayList<>();
//
//                for (int i = 0; i < batchSize; i++) {
//                    // Теперь каждая пачка видит результаты ПРЕДЫДУЩЕЙ пачки
//                    int currentTotalVisits = b * batchSize + i;
//                    int bestPhase = selectBestPhase(policyProbs, phaseToSumQ, phaseToVisits, currentTotalVisits);
//
//                    MarsGame copy = new MarsGame(game);
//                    subtasks.add(scope.fork(() -> {
//                        double v = goInsidePhaseAndReturnValue(bestPhase, copy, playerUuid, anotherPlayerUuid);
//                        return new MctsResult(bestPhase, v);
//                    }));
//                }
//
//                scope.join(); // Ждем завершения пачки
//
//                // Обновляем статистику перед следующей пачкой
//                for (var subtask : subtasks) {
//                    MctsResult res = subtask.get();
//                    phaseToSumQ.merge(res.phase, res.v, Double::sum);
//                    phaseToVisits.merge(res.phase, 1, Integer::sum);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        return buildDecision(phaseToSumQ, phaseToVisits);
//    }

    private MctsDecision buildDecision(Map<Integer, Double> phaseToSumQ, Map<Integer, Integer> phaseToVisits) {
                // 1. Находим победителя по визитам
        int winnerPhase = phaseToVisits.entrySet().stream()
                .max(Map.Entry.comparingByValue()).get().getKey();

        float winnerAvgQ = (float) (phaseToSumQ.get(winnerPhase) / phaseToVisits.get(winnerPhase));

        // 2. Считаем распределение Policy Target (с учетом температуры)
        Map<Integer, Float> policyTarget = new HashMap<>();
        double sumVisitPow = 0;

        for (int n : phaseToVisits.values()) {
            sumVisitPow += Math.pow(n, 1.0 / TAU);
        }

        for (var entry : phaseToVisits.entrySet()) {
            float prob = (float) (Math.pow(entry.getValue(), 1.0 / TAU) / sumVisitPow);
            policyTarget.put(entry.getKey(), prob);
        }

//        printMctsStats(policyProbs, phaseToSumQ, phaseToVisits);
        return new MctsDecision(winnerPhase, winnerAvgQ, policyTarget);
    }

    // Вынесенная логика выбора фазы
    private int selectBestPhase(Map<Integer, Float> policyProbs, Map<Integer, Double> phaseToSumQ, Map<Integer, Integer> phaseToVisits, int totalVisits) {
        int bestPhase = -1;
        double maxUct = -Double.MAX_VALUE;
        double cPuct = 1.41;

        for (int phase : policyProbs.keySet()) {
            double p = policyProbs.get(phase);
            int n = phaseToVisits.getOrDefault(phase, 0);
            double q = (n == 0) ? 0.5 : (phaseToSumQ.get(phase) / n);
            double uct = q + cPuct * p * (Math.sqrt(totalVisits) / (1 + n));

            if (uct > maxUct) {
                maxUct = uct;
                bestPhase = phase;
            }
        }
        return bestPhase;
    }

    // Вспомогательный рекорд
    record MctsResult(int phase, double v) {}


    public MctsDecision runMctsPhaseSelection(Map<Integer, Float> policyProbs, MarsGame game, String playerUuid, String anotherPlayerUuid) {
        int iterations = 100;
        double cPuct = 1.41; // Константа исследования (корень из 2 — классика)

        Map<Integer, Double> phaseToSumQ = new HashMap<>(); // Сумма всех полученных Value
        Map<Integer, Integer> phaseToVisits = new HashMap<>(); // N(s,a)

        // Инициализируем мапы для доступных фаз
        for (Integer phase : policyProbs.keySet()) {
            phaseToSumQ.put(phase, 0.0);
            phaseToVisits.put(phase, 0);
        }

        for (int totalVisits = 0; totalVisits < iterations; totalVisits++) {
            int bestPhase = -1;
            double maxUct = -Double.MAX_VALUE;

            // 1. SELECTION (Выбираем фазу по формуле PUCT)
            for (int phase : policyProbs.keySet()) {
                double p = policyProbs.get(phase);
                int n = phaseToVisits.get(phase);
                double q = (n == 0) ? 0.5 : (phaseToSumQ.get(phase) / n); // 0.5 - начальный скептицизм

                // Формула: Q + C * P * (sqrt(TotalN) / (1 + n))
                double uct = q + cPuct * p * (Math.sqrt(totalVisits) / (1 + n));

                if (uct > maxUct) {
                    maxUct = uct;
                    bestPhase = phase;
                }
            }

            // 2. EXPANSION & EVALUATION (Проигрываем фазу до конца)
            // Метод должен вернуть нормализованный шанс на победу (0.0 ... 1.0)
            double v = goInsidePhaseAndReturnValue(bestPhase, new MarsGame(game), playerUuid, anotherPlayerUuid);

            // 3. BACKPROPAGATION (Обновляем статистику ветки)
            phaseToSumQ.put(bestPhase, phaseToSumQ.get(bestPhase) + v);
            phaseToVisits.put(bestPhase, phaseToVisits.get(bestPhase) + 1);
        }


        // --- ФОРМИРОВАНИЕ РЕЗУЛЬТАТА ---

        // 1. Находим победителя по визитам
        int winnerPhase = phaseToVisits.entrySet().stream()
                .max(Map.Entry.comparingByValue()).get().getKey();

        float winnerAvgQ = (float) (phaseToSumQ.get(winnerPhase) / phaseToVisits.get(winnerPhase));

        // 2. Считаем распределение Policy Target (с учетом температуры)
        Map<Integer, Float> policyTarget = new HashMap<>();
        double sumVisitPow = 0;

        for (int n : phaseToVisits.values()) {
            sumVisitPow += Math.pow(n, 1.0 / TAU);
        }

        for (var entry : phaseToVisits.entrySet()) {
            float prob = (float) (Math.pow(entry.getValue(), 1.0 / TAU) / sumVisitPow);
            policyTarget.put(entry.getKey(), prob);
        }

//        printMctsStats(policyProbs, phaseToSumQ, phaseToVisits);
        return new MctsDecision(winnerPhase, winnerAvgQ, policyTarget);
    }

//    public static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private double goInsidePhaseAndReturnValue(int phase, MarsGame game, String playerUuid, String anotherPlayerUuid) {
        Player player = game.getPlayerByUuid(playerUuid);
        player.addFirstTurn(new PhaseChoiceTurn(playerUuid, phase));

        Player anotherPlayer = game.getPlayerByUuid(anotherPlayerUuid);
        anotherPlayer.setNextTurns(List.of());

        Deck projectsDeck = cardService.createProjectsDeck(game.getExpansions());
        projectsDeck.removeCards(player.getHand().getCards());
        projectsDeck.removeCards(player.getPlayed().getCards());
        projectsDeck.removeCards(anotherPlayer.getPlayed().getCards());

        game.setProjectsDeck(projectsDeck);

        int opponentHandSize = anotherPlayer.getHand().size();
        anotherPlayer.getHand().getCards().clear();
        anotherPlayer.getHand().addCards(cardService.dealCards(game, opponentHandSize));

        ScopedValue.where(
                GameArenaContext.SIMULATION,
                true
        ).run(() -> runGameSimulationUntilPhaseEnd(game));
//        Future<?> future = executor.submit(() -> {
//            ScopedValue.where(
//                    GameArenaContext.SIMULATION,
//                    true
//            ).run(() -> runGameSimulationUntilPhaseEnd(game));
//        });
//
//        try {
//            future.get();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        if (game.getStateType() == StateType.GAME_END) {
            int firstWinPoints = winPointsService.countWinPoints(player, game);
            int secondWinPoints = winPointsService.countWinPoints(anotherPlayer, game);

            if (firstWinPoints > secondWinPoints) {
                return 1;
            } else if (secondWinPoints > firstWinPoints) {
                return 0;
            } else {
                return 0.5;
            }
        }
        PolicyRecord record = new PolicyRecord();
        encoder.encode(game, player, anotherPlayer, record);
        return (policyActionSelector.predict(record).outcome + 1.) / 2;
    }

    private int getMax(Map<Integer, Integer> visits) {
        return visits.values().stream().max(Integer::compare).orElse(0);
    }

    public void runGameSimulationUntilPhaseEnd(MarsGame game) {
        int count = 0;
        do {
            count++;
            while (aiService.waitingAiTurns(game)) {
                aiService.makeAiTurns(game);
            }

            while (processFinalTurns(game)) {
                stateFactory.getCurrentState(game).updateState();
            }
            if (count > 50) {
                System.out.println("COUNT ALERT");
            }
        } while (game.getStateType() != StateType.GAME_END && game.getStateType() != StateType.PICK_PHASE);
    }

    public void printMctsStats(Map<Integer, Float> policyProbs,
                               Map<Integer, Double> phaseToSumQ,
                               Map<Integer, Integer> phaseToVisits) {

        System.out.println("\n=== MCTS PHASE DECISION REPORT ===");
        System.out.printf("%-7s | %-8s | %-8s | %-8s | %-10s%n",
                "PHASE", "POLICY", "AVG_Q", "VISITS", "CONFIDENCE");
        System.out.println("------------------------------------------------------------");

        int totalVisits = phaseToVisits.values().stream().mapToInt(Integer::intValue).sum();

        for (int phase : policyProbs.keySet().stream().sorted().toList()) {
            double p = policyProbs.get(phase);
            int n = phaseToVisits.get(phase);
            double avgQ = (n == 0) ? 0.0 : (phaseToSumQ.get(phase) / n);
            double confidence = (double) n / totalVisits;

            // Выделяем победителя звездочкой
            String marker = (n == getMax(phaseToVisits)) ? " [WIN]" : "";

            System.out.printf("Phase %-2d | %-8.4f | %-8.4f | %-8d | %-8.1f%%%s%n",
                    phase, p, avgQ, n, confidence * 100, marker);
        }
        System.out.println("============================================\n");
    }

//    public int runMctsPhaseSelection(Map<Integer, Float> policyProbs, MarsGame game, String playerUuid, String anotherPlayerUuid) {
//        int iterations = 500;
//        int batchSize = 16; // Оптимально для виртуальных потоков и загрузки CPU/GPU
//        double cPuct = 1.41;
//
//        // Используем Thread-safe мапы или конкурентные обертки, так как запись будет из разных потоков
//        Map<Integer, Double> phaseToSumQ = new ConcurrentHashMap<>();
//        Map<Integer, Integer> phaseToVisits = new ConcurrentHashMap<>();
//
//        for (Integer phase : policyProbs.keySet()) {
//            phaseToSumQ.put(phase, 0.0);
//            phaseToVisits.put(phase, 0);
//        }
//
//        int completedIterations = 0;
//        while (completedIterations < iterations) {
//            List<CompletableFuture<Double>> futures = new ArrayList<>();
//            List<Integer> chosenPhasesInBatch = new ArrayList<>();
//
//            // 1. SELECTION (в главном потоке)
//            for (int i = 0; i < batchSize && (completedIterations + i) < iterations; i++) {
//                int currentTotal = completedIterations + i;
//                int bestPhase = -1;
//                double maxUct = -Double.MAX_VALUE;
//
//                for (int phase : policyProbs.keySet()) {
//                    double p = policyProbs.get(phase);
//                    int n = phaseToVisits.get(phase);
//                    // Если n=0, ставим высокую ценность для исследования
//                    double q = (n == 0) ? 0.8 : (phaseToSumQ.get(phase) / n);
//
//                    double uct = q + cPuct * p * (Math.sqrt(currentTotal) / (1 + n));
//
//                    if (uct > maxUct) {
//                        maxUct = uct;
//                        bestPhase = phase;
//                    }
//                }
//
//                // Virtual Loss: Сразу помечаем визит, чтобы следующий выбор в батче
//                // видел, что эту ветку уже "копают", и переключался на другие.
//                phaseToVisits.merge(bestPhase, 1, Integer::sum);
//                chosenPhasesInBatch.add(bestPhase);
//
//                // 2. EXPANSION (Запуск в виртуальном потоке)
//                final int phaseToSimulate = bestPhase;
//                // Копируем игру СНАРУЖИ потока, чтобы избежать Race Condition при чтении
//                MarsGame gameCopy = new MarsGame(game);
//
//                futures.add(CompletableFuture.supplyAsync(() ->
//                                simulateAndScore(phaseToSimulate, gameCopy, playerUuid, anotherPlayerUuid),
//                        executorService));
//            }
//
//            // 3. Ждем завершения батча
//            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//
//            // 4. BACKPROPAGATION (Обновляем Q-values)
//            for (int i = 0; i < futures.size(); i++) {
//                try {
//                    double v = futures.get(i).get();
//                    int phase = chosenPhasesInBatch.get(i);
//                    phaseToSumQ.merge(phase, v, Double::sum);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//
//            completedIterations += futures.size();
//        }
//
//        printMctsStats(policyProbs, phaseToSumQ, phaseToVisits);
//        return phaseToVisits.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
//    }
//
//    /**
//     * Чистая симуляция. Теперь без вложенных .get() и submit().
//     * Выполняется внутри виртуального потока CompletableFuture.
//     */
//    private double simulateAndScore(int phase, MarsGame game, String playerUuid, String anotherPlayerUuid) {
//        Player player = game.getPlayerByUuid(playerUuid);
//        player.addFirstTurn(new PhaseChoiceTurn(playerUuid, phase));
//
//        Player anotherPlayer = game.getPlayerByUuid(anotherPlayerUuid);
//        anotherPlayer.setNextTurns(List.of());
//
//        // Подготовка колоды
//        Deck projectsDeck = cardService.createProjectsDeck(game.getExpansions());
//        projectsDeck.removeCards(player.getHand().getCards());
//        projectsDeck.removeCards(player.getPlayed().getCards());
//        projectsDeck.removeCards(anotherPlayer.getPlayed().getCards());
//        game.setProjectsDeck(projectsDeck);
//
//        // Скрытие информации (имитация неизвестных карт оппонента)
//        int opponentHandSize = anotherPlayer.getHand().size();
//        anotherPlayer.getHand().getCards().clear();
//        anotherPlayer.getHand().addCards(cardService.dealCards(game, opponentHandSize));
//
//        try {
//            // Выполняем симуляцию прямо в текущем виртуальном потоке
//            ScopedValue.where(GameArenaContext.SIMULATION, true).run(() -> {
//                runGameSimulationUntilPhaseEnd(game);
//            });
//
//            PolicyRecord record = new PolicyRecord();
//            encoder.encode(game, player, anotherPlayer, record);
//
//            // Предсказание (Важно: если predict внутри синхронизирован, это станет бутылочным горлышком)
//            return (policyActionSelector.predict(record).outcome + 1.0) / 2.0;
//        } catch (Exception e) {
//            return 0.5;
//        }
//    }



}
