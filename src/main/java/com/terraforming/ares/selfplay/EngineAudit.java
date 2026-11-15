package com.terraforming.ares.selfplay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.model.StateType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.agent.BlueInputDecisions;
import com.terraforming.ares.services.agent.DecisionType;
import com.terraforming.ares.services.agent.MlpValueModel;
import com.terraforming.ares.services.agent.ObservationBuilder;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Does the critic know what a blue engine is worth? The owner's NETWORK did: its value grew when a table full of blue
 * action cards could be realised in phase 3. This mode measures the same claim on our critic, on real mid-game positions:
 * <ol>
 *   <li>V of the position as it is;</li>
 *   <li>V after putting several activatable blue action cards on the table (and money to press them);</li>
 *   <li>V after actually pressing them on a copy.</li>
 * </ol>
 * A critic that is blind to engines shows no difference between (1) and (2), or a drop: then no search can ever prefer an
 * engine, and the training data has to contain engines before anything else is changed.
 * <p>
 * {@code --mode engineaudit --out FILE.json --games N --value-model DIR [--cards K]}
 */
@Component
@RequiredArgsConstructor
public class EngineAudit {
    public record Sample(double base, double engine, double activated, int cards) {
    }

    public record Report(String valueModel, int positions, double base, double engine, double activated,
                         double engineMinusBase, double activatedMinusEngine, List<Sample> samples) {
    }

    private final GameService gameService;
    private final SimulationProcessorService simulation;
    private final CardService cardService;
    private final ObservationBuilder observationBuilder;
    private final BlueInputDecisions blueInputDecisions;
    private final AiTurnService aiTurnService;

    public Report run(Path out, int positions, String valueModel, int cards) throws Exception {
        MlpValueModel model = MlpValueModel.load(Path.of(valueModel));
        List<Sample> samples = new ArrayList<>();
        List<Integer> activatable = activatableCards();
        int guard = 0;
        while (samples.size() < positions && guard++ < positions * 20) {
            MarsGame game = snapshot(StateType.PERFORM_BLUE_ACTION);
            if (game == null) {
                break;
            }
            Player player = game.getPlayerUuidToPlayer().values().iterator().next();
            player.setMc(Math.max(player.getMc(), 60));
            List<Integer> usable = new ArrayList<>();
            for (Integer id : activatable) {
                if (usable.size() == cards) {
                    break;
                }
                try {
                    blueInputDecisions.fill(game, player, cardService.getCard(id), decision -> 0);
                    usable.add(id);
                } catch (RuntimeException e) {
                    // not activatable in this position
                }
            }
            if (usable.size() < 2) {
                continue;
            }
            double base = model.logit(observationBuilder.build(game, player));
            MarsGame withEngine = new MarsGame(game);
            Player enginePlayer = withEngine.getPlayerByUuid(player.getUuid());
            for (Integer id : usable) {
                enginePlayer.getPlayed().addCard(id);
            }
            double engine = model.logit(observationBuilder.build(withEngine, enginePlayer));
            MarsGame pressed = new MarsGame(withEngine);
            Player pressedPlayer = pressed.getPlayerByUuid(player.getUuid());
            for (Integer id : usable) {
                try {
                    Map<Integer, List<Integer>> input = blueInputDecisions.fill(pressed, pressedPlayer,
                            cardService.getCard(id), decision -> 0);
                    aiTurnService.performBlueAction(pressed, pressedPlayer, id, input);
                } catch (RuntimeException e) {
                    // a card that cannot be pressed in this copy: skip it, the other ones still pay
                }
            }
            double afterActivation = model.logit(observationBuilder.build(pressed, pressedPlayer));
            samples.add(new Sample(base, engine, afterActivation, usable.size()));
        }
        Report report = new Report(valueModel, samples.size(),
                mean(samples, Sample::base), mean(samples, Sample::engine), mean(samples, Sample::activated),
                mean(samples, Sample::engine) - mean(samples, Sample::base),
                mean(samples, Sample::activated) - mean(samples, Sample::engine),
                samples);
        if (out != null && out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        if (out != null) {
            new ObjectMapper().writeValue(out.toFile(), report);
        }
        System.out.printf("[ENGINE] %s: %d positions, V base %.3f, +engine %.3f (%+.3f), +pressed %.3f (%+.3f)%n",
                valueModel, report.positions(), report.base(), report.engine(), report.engineMinusBase(),
                report.activated(), report.activatedMinusEngine());
        return report;
    }

    private static double mean(List<Sample> samples, java.util.function.ToDoubleFunction<Sample> field) {
        return samples.isEmpty() ? 0 : samples.stream().mapToDouble(field).average().orElse(0);
    }

    /** Blue cards with an action that exist in the played expansions. */
    private List<Integer> activatableCards() {
        List<Integer> result = new ArrayList<>();
        for (Integer id : cardService.createProjectsDeck(new ArrayList<>(ObservationBuilder.PLAYED_EXPANSIONS))
                .getCards()) {
            Card card = cardService.getCard(id);
            if (card != null && card.isActiveCard()) {
                result.add(id);
            }
        }
        return result;
    }

    private MarsGame snapshot(StateType state) {
        MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(PlayerDifficulty.SMART, PlayerDifficulty.SMART))
                .mulligan(true)
                .expansion(com.terraforming.ares.model.Expansion.BASE)
                .expansion(com.terraforming.ares.model.Expansion.DISCOVERY)
                .expansion(com.terraforming.ares.model.Expansion.BUFFED_CORPORATION)
                .dummyHand(true).build());
        MarsGame[] snapshot = new MarsGame[1];
        simulation.processSimulation(game, current -> {
            if (snapshot[0] == null && current.getTurns() >= 20 && current.getStateType() == state) {
                snapshot[0] = new MarsGame(current);
            }
        });
        return snapshot[0];
    }
}
