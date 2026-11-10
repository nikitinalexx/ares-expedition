package com.terraforming.ares.services.agent;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.StandardProjectType;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.PaymentValidationService;
import com.terraforming.ares.services.StandardProjectService;
import com.terraforming.ares.services.TerraformingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Phase-3 spend menu: standard projects, plant/heat/infra conversions, Helion heat→MC one unit at a time, or stop.
 * Stop leaves blues, builds and skip to the fallback. The engine lists these turns together; converting now or buying a
 * project now are alternatives to each other, so they share one decision.
 * <p>
 * Candidate features: one-hot kind, MC/heat/plant costs (scaled), which global parameter rises, Cargo Ships heat flag,
 * stop. Kind ids are stable integers; a new kind must be appended and increases {@link #KINDS}.
 */
@Service
@RequiredArgsConstructor
public class StandardActionDecisions {
    public static final int STOP = 0;
    public static final int EXCHANGE_ONE_HEAT = 1;
    public static final int PLANT_FOREST = 2;
    public static final int INCREASE_TEMPERATURE = 3;
    public static final int INCREASE_INFRASTRUCTURE_PLANTS = 4;
    public static final int INCREASE_INFRASTRUCTURE_HEAT = 5;
    public static final int SP_OCEAN = 6;
    public static final int SP_FOREST = 7;
    public static final int SP_TEMPERATURE = 8;
    public static final int SP_INFRASTRUCTURE_PLANTS = 9;
    public static final int SP_INFRASTRUCTURE_HEAT = 10;
    public static final int KINDS = 11;

    public static final int MAX_CANDIDATES = KINDS;
    public static final int CANDIDATE_FEATURES = KINDS + 3 + 4 + 1 + 1;
    public static final String OBSERVATION_LAYOUT = ObservationBuilder.LAYOUT
            + "; candidate=[kind(" + KINDS + "), mcCost/20, heatCost/10, plantCost/10, raisesTemp, raisesOcean,"
            + " raisesForestOrOxygen, raisesInfra, cargoHeat, stop]; stopId=" + STOP;

    private static final Map<Integer, List<Integer>> CARGO_HEAT =
            Map.of(InputFlag.CARGO_SHIPS.getId(), List.of(InputFlag.CARGO_SHIPS_HEAT.getId()));

    private final ObservationBuilder observationBuilder;
    private final StandardProjectService standardProjectService;
    private final PaymentValidationService paymentValidationService;
    private final TerraformingService terraformingService;
    private final CardService cardService;

    public static DecisionFile.Schema schema() {
        return new DecisionFile.Schema(DecisionType.STANDARD_ACTION, ObservationBuilder.size(),
                DecisionType.STANDARD_ACTION.maxCandidates(), CANDIDATE_FEATURES, OBSERVATION_LAYOUT);
    }

    public List<StandardActionChoice> options(MarsGame game, Player player, List<TurnType> possibleTurns) {
        List<StandardActionChoice> choices = new ArrayList<>();
        if (possibleTurns.contains(TurnType.EXCHANGE_HEAT) && isHelion(player) && player.getHeat() > 0) {
            choices.add(new StandardActionChoice(EXCHANGE_ONE_HEAT, null, Map.of()));
        }
        if (possibleTurns.contains(TurnType.PLANT_FOREST)
                && player.getPlants() >= paymentValidationService.forestPriceInPlants(player)) {
            choices.add(new StandardActionChoice(PLANT_FOREST, null, Map.of()));
        }
        if (possibleTurns.contains(TurnType.INCREASE_TEMPERATURE)
                && player.getHeat() >= Constants.TEMPERATURE_HEAT_COST
                && terraformingService.canIncreaseTemperature(game)) {
            choices.add(new StandardActionChoice(INCREASE_TEMPERATURE, null, Map.of()));
        }
        if (possibleTurns.contains(TurnType.INCREASE_INFRASTRUCTURE)
                && player.getHeat() >= Constants.INFRASTRUCTURE_HEAT_COST
                && player.getPlants() >= Constants.INFRASTRUCTURE_PLANT_COST
                && terraformingService.canIncreaseInfrastructure(game)) {
            choices.add(new StandardActionChoice(INCREASE_INFRASTRUCTURE_PLANTS, null, Map.of()));
            if (hasCargoShips(player)) {
                choices.add(new StandardActionChoice(INCREASE_INFRASTRUCTURE_HEAT, null, CARGO_HEAT));
            }
        }
        if (possibleTurns.contains(TurnType.STANDARD_PROJECT)) {
            for (StandardProjectType type : StandardProjectType.values()) {
                if (standardProjectService.validateStandardProject(game, player, type) != null) {
                    continue;
                }
                if (type == StandardProjectType.INFRASTRUCTURE) {
                    choices.add(new StandardActionChoice(SP_INFRASTRUCTURE_PLANTS, type, Map.of()));
                    if (hasCargoShips(player)) {
                        choices.add(new StandardActionChoice(SP_INFRASTRUCTURE_HEAT, type, CARGO_HEAT));
                    }
                } else {
                    choices.add(new StandardActionChoice(spKind(type), type, Map.of()));
                }
            }
        }
        choices.add(new StandardActionChoice(STOP, null, Map.of()));
        if (choices.size() > MAX_CANDIDATES) {
            throw new IllegalStateException("Standard action menu has " + choices.size() + " options");
        }
        return choices;
    }

    public Decision decision(MarsGame game, Player player, List<StandardActionChoice> choices) {
        List<Candidate> candidates = new ArrayList<>(choices.size());
        for (StandardActionChoice choice : choices) {
            candidates.add(new Candidate(choice.kind(), features(player, choice)));
        }
        return new Decision(DecisionType.STANDARD_ACTION, player.getUuid(),
                observationBuilder.build(game, player), candidates);
    }

    public float[] features(Player player, StandardActionChoice choice) {
        float[] features = new float[CANDIDATE_FEATURES];
        features[choice.kind()] = 1f;
        int pos = KINDS;
        features[pos] = mcCost(player, choice) / 20f;
        features[pos + 1] = heatCost(choice) / 10f;
        features[pos + 2] = plantCost(player, choice) / 10f;
        pos += 3;
        features[pos] = raisesTemperature(choice) ? 1f : 0f;
        features[pos + 1] = choice.kind() == SP_OCEAN ? 1f : 0f;
        features[pos + 2] = raisesForest(choice) ? 1f : 0f;
        features[pos + 3] = raisesInfrastructure(choice) ? 1f : 0f;
        pos += 4;
        features[pos] = choice.kind() == INCREASE_INFRASTRUCTURE_HEAT || choice.kind() == SP_INFRASTRUCTURE_HEAT ? 1f : 0f;
        features[pos + 1] = choice.kind() == STOP ? 1f : 0f;
        return features;
    }

    private int mcCost(Player player, StandardActionChoice choice) {
        if (choice.projectType() == null) {
            return 0;
        }
        return standardProjectService.getProjectPrice(player, choice.projectType());
    }

    private static int heatCost(StandardActionChoice choice) {
        return switch (choice.kind()) {
            case EXCHANGE_ONE_HEAT -> 1;
            case INCREASE_TEMPERATURE -> Constants.TEMPERATURE_HEAT_COST;
            case INCREASE_INFRASTRUCTURE_PLANTS, INCREASE_INFRASTRUCTURE_HEAT -> Constants.INFRASTRUCTURE_HEAT_COST;
            default -> 0;
        };
    }

    private int plantCost(Player player, StandardActionChoice choice) {
        return switch (choice.kind()) {
            case PLANT_FOREST -> paymentValidationService.forestPriceInPlants(player);
            case INCREASE_INFRASTRUCTURE_PLANTS, INCREASE_INFRASTRUCTURE_HEAT -> Constants.INFRASTRUCTURE_PLANT_COST;
            default -> 0;
        };
    }

    private static boolean raisesTemperature(StandardActionChoice choice) {
        return choice.kind() == INCREASE_TEMPERATURE || choice.kind() == SP_TEMPERATURE;
    }

    private static boolean raisesForest(StandardActionChoice choice) {
        return choice.kind() == PLANT_FOREST || choice.kind() == SP_FOREST;
    }

    private static boolean raisesInfrastructure(StandardActionChoice choice) {
        return choice.kind() == INCREASE_INFRASTRUCTURE_PLANTS || choice.kind() == INCREASE_INFRASTRUCTURE_HEAT
                || choice.kind() == SP_INFRASTRUCTURE_PLANTS || choice.kind() == SP_INFRASTRUCTURE_HEAT;
    }

    private static int spKind(StandardProjectType type) {
        return switch (type) {
            case OCEAN -> SP_OCEAN;
            case FOREST -> SP_FOREST;
            case TEMPERATURE -> SP_TEMPERATURE;
            case INFRASTRUCTURE -> SP_INFRASTRUCTURE_PLANTS;
        };
    }

    private boolean hasCargoShips(Player player) {
        for (int id : player.getPlayed().getCards()) {
            Card card = cardService.getCard(id);
            if (card.getCardMetadata() != null && card.getCardMetadata().getCardAction() == CardAction.CARGO_SHIPS) {
                return true;
            }
        }
        return false;
    }

    private boolean isHelion(Player player) {
        if (player.getSelectedCorporationCard() == null) {
            return false;
        }
        Card corporation = cardService.getCard(player.getSelectedCorporationCard());
        return corporation.getCardMetadata() != null
                && corporation.getCardMetadata().getCardAction() == CardAction.HELION_CORPORATION;
    }
}
