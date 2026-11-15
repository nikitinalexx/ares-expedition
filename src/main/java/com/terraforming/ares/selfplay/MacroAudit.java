package com.terraforming.ares.selfplay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.agent.BlueActionDecisions;
import com.terraforming.ares.services.agent.Decision;
import com.terraforming.ares.services.agent.DecisionProbe;
import com.terraforming.ares.services.agent.DecisionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Causal test of a PLAN on the real simulator: play the position twice with the same deck order and the same opponent,
 * once normally (A) and once with a macro that forces the plan for a few rounds (B), then compare the final margins.
 * <p>
 * Why it is needed: the value of an option depends on the policy that will realise it. A blue card is worth something only
 * if someone calls phase 3 and presses the actions afterwards - so a paired rollout with the plain policy answers "0" for
 * an engine, no matter how good an engine is. Forcing the plan inside the rollout answers the different, causal question:
 * "if this player did commit to the engine for a few rounds, would it end better?" That answer is evidence the critic and
 * the policy can be taught from, and it needs no teacher: it is the game itself.
 * <p>
 * {@code --mode macroaudit --out FILE.json --games N --turns 8}
 */
@Component
@RequiredArgsConstructor
public class MacroAudit {
    public record Sample(int turns, double blueCards, double normalMargin, double planMargin, double difference) {
    }

    public record Report(int positions, int turnsBudget, double normalMargin, double planMargin, double meanDifference,
                         double sd, int betterWithPlan, int betterWithout, List<Sample> samples) {
    }

    private final GameService gameService;
    private final SimulationProcessorService simulation;
    private final WinPointsService winPointsService;
    private final CardService cardService;
    private final BlueActionDecisions blueActionDecisions;
    private final com.terraforming.ares.services.agent.GuaranteedActions guaranteedActions;

    /**
     * Forces a plan inside the rollout for a bounded number of turns. The variants are the honest candidates for "what
     * does playing an engine mean": press everything, press only what is free, build for income.
     */
    private final class PlanHook implements DecisionProbe.Hook {
        private final int untilTurns;
        private final String mode;

        PlanHook(int untilTurns, String mode) {
            this.untilTurns = untilTurns;
            this.mode = mode;
        }

        @Override
        public int beforeChoice(Decision decision, MarsGame game, Player player) {
            if (game.getTurns() > untilTurns) {
                return -1;
            }
            if (decision.type() == DecisionType.PERFORM_BLUE_ACTION) {
                if (mode.equals("all")) {
                    for (int i = 0; i < decision.candidates().size(); i++) {
                        if (decision.candidates().get(i).id() != BlueActionDecisions.STOP) {
                            return i;
                        }
                    }
                }
                var free = guaranteedActions.freeBlueAction(game, player);
                if (free.isPresent()) {
                    for (int i = 0; i < decision.candidates().size(); i++) {
                        if (decision.candidates().get(i).id() == free.get()) {
                            return i;
                        }
                    }
                }
                return -1;
            }
            if (decision.type() == DecisionType.BUILD_CARD) {
                for (int i = 0; i < decision.candidates().size(); i++) {
                    int id = decision.candidates().get(i).id();
                    if (id == com.terraforming.ares.services.agent.BuildCardDecisions.STOP) {
                        continue;
                    }
                    Card card = cardService.getCard(id);
                    if (card == null) {
                        continue;
                    }
                    if (mode.equals("income")) {
                        if (card.getCardMetadata() != null && !card.getCardMetadata().getIncomes().isEmpty()) {
                            return i;
                        }
                    } else if (card.getColor() != CardColor.GREEN) {
                        return i;
                    }
                }
            }
            return -1;
        }

        @Override
        public void afterChoice(Decision decision, MarsGame game, Player player, int index) {
        }
    }

    public Report run(Path out, int positions, int turnsBudget, String mode) throws Exception {
        List<Sample> samples = new ArrayList<>();
        int guard = 0;
        while (samples.size() < positions && guard++ < positions * 10) {
            MarsGame game = snapshot();
            if (game == null) {
                break;
            }
            Player me = game.getPlayerUuidToPlayer().values().iterator().next();
            int blueCards = (int) me.getPlayed().getCards().stream()
                    .map(cardService::getCard)
                    .filter(card -> card != null && card.getColor() == CardColor.BLUE)
                    .count();
            MarsGame plain = new MarsGame(game);
            double normal = playOut(plain, me.getUuid(), null);
            MarsGame planned = new MarsGame(game);
            double plan = playOut(planned, me.getUuid(), new PlanHook(game.getTurns() + turnsBudget, mode));
            samples.add(new Sample(turnsBudget, blueCards, normal, plan, plan - normal));
        }
        double meanNormal = samples.stream().mapToDouble(Sample::normalMargin).average().orElse(0);
        double meanPlan = samples.stream().mapToDouble(Sample::planMargin).average().orElse(0);
        double[] differences = samples.stream().mapToDouble(Sample::difference).toArray();
        double mean = samples.stream().mapToDouble(Sample::difference).average().orElse(0);
        double sd = 0;
        for (double difference : differences) {
            sd += (difference - mean) * (difference - mean);
        }
        sd = differences.length > 1 ? Math.sqrt(sd / (differences.length - 1)) : 0;
        Report report = new Report(samples.size(), turnsBudget, meanNormal, meanPlan, mean, sd,
                (int) samples.stream().filter(s -> s.difference() > 0).count(),
                (int) samples.stream().filter(s -> s.difference() < 0).count(), samples);
        if (out != null && out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        if (out != null) {
            new ObjectMapper().writeValue(out.toFile(), report);
        }
        System.out.printf("[MACRO] %d positions, %d turns: normal %+.1f, with the engine plan %+.1f, "
                        + "difference %+.1f +/- %.1f (better with plan %d, better without %d)%n",
                report.positions(), report.turnsBudget(), report.normalMargin(), report.planMargin(),
                report.meanDifference(), 1.96 * report.sd() / Math.sqrt(Math.max(1, report.positions())),
                report.betterWithPlan(), report.betterWithout());
        return report;
    }

    private double playOut(MarsGame sim, String uuid, DecisionProbe.Hook hook) {
        DecisionProbe.Hook previous = DecisionProbe.set(hook);
        try {
            simulation.processSimulation(sim);
        } finally {
            DecisionProbe.set(previous);
        }
        Player me = sim.getPlayerByUuid(uuid);
        Player other = sim.getPlayerUuidToPlayer().values().stream()
                .filter(p -> !p.getUuid().equals(uuid)).findFirst().orElseThrow();
        return winPointsService.countWinPoints(me, sim) - winPointsService.countWinPoints(other, sim);
    }

    private MarsGame snapshot() {
        MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(PlayerDifficulty.AGENT, PlayerDifficulty.AGENT))
                .mulligan(true)
                .expansion(com.terraforming.ares.model.Expansion.BASE)
                .expansion(com.terraforming.ares.model.Expansion.DISCOVERY)
                .expansion(com.terraforming.ares.model.Expansion.BUFFED_CORPORATION)
                .dummyHand(true).build());
        MarsGame[] snapshot = new MarsGame[1];
        simulation.processSimulation(game, current -> {
            if (snapshot[0] == null && current.getTurns() >= 16
                    && current.getStateType() == StateType.PERFORM_BLUE_ACTION) {
                snapshot[0] = new MarsGame(current);
            }
        });
        return snapshot[0];
    }
}
