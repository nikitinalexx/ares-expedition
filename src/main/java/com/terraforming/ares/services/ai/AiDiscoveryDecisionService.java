package com.terraforming.ares.services.ai;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.ai.AiCardsChoice;
import com.terraforming.ares.model.ai.AiTurnChoice;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.MarsContextProvider;
import com.terraforming.ares.services.TerraformingService;
import com.terraforming.ares.services.UpgradePhaseHelper;
import com.terraforming.ares.services.ai.dto.PhaseUpgradeWithChance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.terraforming.ares.model.Constants.*;
import static com.terraforming.ares.model.ai.AiTurnChoice.NETWORK;
import static com.terraforming.ares.model.ai.AiTurnChoice.SMART;

@Service
@RequiredArgsConstructor
public class AiDiscoveryDecisionService {
    private final Random random = new Random();
    private final DeepNetwork deepNetwork;
    private final CardService cardService;
    private final TerraformingService terraformingService;
    private final MarsContextProvider marsContextProvider;

    public int choosePhaseUpgrade(MarsGame game, Player player, int phaseToUpgrade) {
        int phaseOffset = (phaseToUpgrade - 1) * 2;
        boolean chooseSecondUpgrade = false;
        switch (player.getDifficulty().PHASE_TAG_UPGRADE) {
            case RANDOM:
            case SMART:
                chooseSecondUpgrade = random.nextBoolean();
                break;
            case NETWORK:
                List<Integer> phaseUpgrades = player.getPhaseCards();

                int phaseUpgrade = phaseUpgrades.get(phaseToUpgrade - 1);

                if (phaseUpgrade == 0) {
                    phaseUpgrades.set(phaseToUpgrade - 1, 1);
                    float firstUpgradeState = deepNetwork.testState(game, player);

                    phaseUpgrades.set(phaseToUpgrade - 1, 2);
                    float secondUpgradeState = deepNetwork.testState(game, player);

                    phaseUpgrades.set(phaseToUpgrade - 1, 0);

                    if (secondUpgradeState > firstUpgradeState) {
                        chooseSecondUpgrade = true;
                    }
                } else {
                    float initialState = deepNetwork.testState(game, player);

                    int anotherUpdate = phaseUpgrade == 1 ? 2 : 1;

                    phaseUpgrades.set(phaseToUpgrade - 1, anotherUpdate);
                    float anotherUpgradeState = deepNetwork.testState(game, player);

                    if (anotherUpgradeState > initialState) {
                        chooseSecondUpgrade = (anotherUpdate == 2);
                    }
                }
                phaseUpgrades.set(phaseToUpgrade - 1, phaseUpgrade);
                //TODO run in debug to test at least once
                break;
            default:
                throw new IllegalStateException("Unable to choose phase upgrade for AI");
        }
        return phaseOffset + (chooseSecondUpgrade ? 1 : 0);
    }

    public List<Integer> chooseTwoPhaseUpgrades(MarsGame game, Player player) {
        switch (player.getDifficulty().PHASE_TAG_UPGRADE) {
            case RANDOM:
                int firstUpgrade = random.nextInt(5);
                int secondUpgrade = random.nextInt(5);
                if (firstUpgrade == secondUpgrade) {
                    secondUpgrade = (secondUpgrade + 1) % 5;
                }
                return List.of(choosePhaseUpgrade(game, player, firstUpgrade + 1), choosePhaseUpgrade(game, player, secondUpgrade + 1));
            case SMART:
                List<Integer> nonUpdatedPhaseCards = new ArrayList<>();
                List<Integer> updatedPhaseCards = new ArrayList<>();
                for (int i = 1; i <= 5; i++) {
                    if (player.getPhaseCards().get(i - 1) == 0) {
                        nonUpdatedPhaseCards.add(i);
                    } else {
                        updatedPhaseCards.add(i);
                    }
                }
                int firstUpdate;
                int secondUpdate;
                if (nonUpdatedPhaseCards.size() >= 2) {
                    firstUpdate = nonUpdatedPhaseCards.get(random.nextInt(nonUpdatedPhaseCards.size()));
                    nonUpdatedPhaseCards.remove((Integer) firstUpdate);
                    secondUpdate = nonUpdatedPhaseCards.get(random.nextInt(nonUpdatedPhaseCards.size()));
                } else if (nonUpdatedPhaseCards.size() == 1) {
                    firstUpdate = nonUpdatedPhaseCards.get(0);
                    secondUpdate = updatedPhaseCards.get(random.nextInt(updatedPhaseCards.size()));
                } else {
                    firstUpdate = updatedPhaseCards.get(random.nextInt(updatedPhaseCards.size()));
                    updatedPhaseCards.remove((Integer) firstUpdate);
                    secondUpdate = updatedPhaseCards.get(random.nextInt(updatedPhaseCards.size()));
                }
                return List.of(choosePhaseUpgrade(game, player, firstUpdate), choosePhaseUpgrade(game, player, secondUpdate));

            case NETWORK:
                List<PhaseUpgradeWithChance> bestUpgrades = findBestUpgrades(chooseBestUpdateForEachPhase(game, player, player.isFirstBot() ? 1 : 2), 2);

                return List.of(bestUpgrades.get(0).getUpgradeAsNumber(), bestUpgrades.get(1).getUpgradeAsNumber());
            default:
                throw new IllegalStateException("Unable to choose phase upgrade for AI");
        }
    }


    private List<PhaseUpgradeWithChance> findBestUpgrades(List<PhaseUpgradeWithChance> upgrades, int count) {
        List<PhaseUpgradeWithChance> sortedList = upgrades.stream()
                .sorted(Comparator.comparing(PhaseUpgradeWithChance::getChance).reversed())
                .collect(Collectors.toList());

        List<PhaseUpgradeWithChance> result = new ArrayList<>();
        for (PhaseUpgradeWithChance item : sortedList) {
            if (result.stream().noneMatch(p -> p.getPhase() == item.getPhase())) {
                result.add(item);
            }

            if (result.size() == count) {
                break;
            }
        }

        return result;
    }

    public List<PhaseUpgradeWithChance> chooseBestUpdateForEachPhase(MarsGame game, Player player, int network) {
        List<Integer> initialUpgrades = new ArrayList<>(player.getPhaseCards());

        List<PhaseUpgradeWithChance> result = new ArrayList<>();

        float initialState = deepNetwork.testState(game, player, network);
        for (int i = 0; i < UPGRADEABLE_PHASES_COUNT; i++) {
            int currentUpgrade = initialUpgrades.get(i);

            float firstUpdateChance = initialState;
            if (currentUpgrade != 1) {
                player.getPhaseCards().set(i, 1);
                firstUpdateChance = deepNetwork.testState(game, player, network);
                player.getPhaseCards().set(i, initialUpgrades.get(i));
            }

            result.add(PhaseUpgradeWithChance.of(firstUpdateChance, i, 1));

            float secondUpdateChance = initialState;
            if (currentUpgrade != 2) {
                player.getPhaseCards().set(i, 2);
                secondUpdateChance = deepNetwork.testState(game, player, network);
                player.getPhaseCards().set(i, initialUpgrades.get(i));
            }

            result.add(PhaseUpgradeWithChance.of(secondUpdateChance, i, 2));

        }

        return result;
    }

    public int choosePhaseUpgrade(MarsGame game, Player player) {
        switch (player.getDifficulty().PHASE_TAG_UPGRADE) {
            case RANDOM:
            case SMART:
                return pickRandomSingleUpgrade(player, Set.of());
            case NETWORK:
                List<PhaseUpgradeWithChance> bestUpgrades = findBestUpgrades(chooseBestUpdateForEachPhase(game, player, player.isFirstBot() ? 1 : 2), 1);

                return bestUpgrades.get(0).getUpgradeAsNumber();
            default:
                throw new IllegalStateException("Unable to choose phase upgrade for AI");
        }
    }

    public int pickRandomSingleUpgrade(Player player, Set<Integer> forbiddenPhases) {
        List<Integer> phaseCards = player.getPhaseCards();
        List<Integer> candidates = new ArrayList<>();

        // 1. Ищем фазы без апгрейда
        for (int i = 0; i < UPGRADEABLE_PHASES_COUNT; i++) {
            if (phaseCards.get(i) == 0 && !forbiddenPhases.contains(i)) {
                candidates.add(i);
            }
        }

        // 2. Если таких нет — берём любую разрешённую фазу
        if (candidates.isEmpty()) {
            for (int i = 0; i < UPGRADEABLE_PHASES_COUNT; i++) {
                if (!forbiddenPhases.contains(i)) {
                    candidates.add(i);
                }
            }
        }

        int phaseIndex = candidates.get(random.nextInt(candidates.size()));
        int currentState = phaseCards.get(phaseIndex);

        int upgradeIndex;
        if (currentState == 0) {
            upgradeIndex = 0;
        } else if (currentState == 1) {
            upgradeIndex = 1;
        } else {
            upgradeIndex = 0;
        }

        return phaseIndex * 2 + upgradeIndex;
    }

    /**
     *
     * @param phase 1 to 5
     */
    public int pickRandomUpgradeForPhase(Player player, int phase) {
        int phaseIndex = phase - 1;
        int currentState = player.getPhaseCards().get(phaseIndex);

        int upgradeIndex;
        if (currentState == 0) {
            upgradeIndex = random.nextBoolean() ? 0 : 1;
        } else if (currentState == 1) {
            upgradeIndex = 1;
        } else {
            upgradeIndex = 0;
        }

        return phaseIndex * 2 + upgradeIndex;
    }

    public List<Integer> pickTwoRandomNonCompetingUpgrades(Player player) {
        int firstUpgrade = pickRandomSingleUpgrade(player, Set.of());
        int upgradedPhase = firstUpgrade / 2;

        int secondUpgrade = pickRandomSingleUpgrade(player, Set.of(upgradedPhase));

        return List.of(firstUpgrade, secondUpgrade);
    }


    public Map<Integer, List<Integer>> getBiomedicalImportsBestInput(MarsGame game, Player player) {
        float stateAfterOxygenIncrease = 0;

        if (!game.getPlanetAtTheStartOfThePhase().isOxygenMax()) {
            MarsGame copy = new MarsGame(game);
            terraformingService.raiseOxygen(
                    marsContextProvider.provide(copy, copy.getPlayerByUuid(player.getUuid()))
            );
            stateAfterOxygenIncrease = deepNetwork.testState(copy, copy.getPlayerByUuid(player.getUuid()));
        }

        MarsGame copy = new MarsGame(game);
        int bestPhaseUpgrade = choosePhaseUpgrade(copy, copy.getPlayerByUuid(player.getUuid()));
        UpgradePhaseHelper.upgradePhase(copy.getPlayerByUuid(player.getUuid()), bestPhaseUpgrade);
        float stateAfterPhaseUpgrade = deepNetwork.testState(copy, copy.getPlayerByUuid(player.getUuid()));

        if (stateAfterOxygenIncrease > stateAfterPhaseUpgrade) {
            return Map.of(InputFlag.BIOMEDICAL_IMPORTS_RAISE_OXYGEN.getId(), List.of(0));
        } else {
            return Map.of(
                    InputFlag.BIOMEDICAL_IMPORTS_UPGRADE_PHASE.getId(), List.of(-1),
                    InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(bestPhaseUpgrade)
            );
        }
    }

    public int chooseAustellarCorporationMilestone(MarsGame game, Player player) {
        List<Milestone> milestones = game.getMilestones();

        if (player.getDifficulty().CARDS_PICK == AiCardsChoice.RANDOM) {
            return random.nextInt(milestones.size());
        }

        Map<Tag, Long> tagToCount = cardService.countTagsOnCards(player.getHand().getCards());

        double milestoneRating = 0;
        int bestMilestoneIndex = 0;

        for (int i = 0; i < milestones.size(); i++) {
            Milestone milestone = milestones.get(i);

            double currentRating = 0;
            switch (milestone.getType()) {
                case BUILDER:
                    currentRating = (double) tagToCount.getOrDefault(Tag.BUILDING, 0L) / 8;
                    break;
                case SPACE_BARON:
                    currentRating = (double) tagToCount.getOrDefault(Tag.SPACE, 0L) / 7;
                    break;
                case DIVERSIFIER:
                    currentRating = (double) tagToCount.keySet().size() / 9;
                    break;
                case ENERGIZER:
                    int heatIncomeSum = player.getHand().getCards().stream().map(cardService::getCard).map(Card::getCardMetadata).filter(Objects::nonNull)
                            .filter(cardMetadata -> !CollectionUtils.isEmpty(cardMetadata.getIncomes()))
                            .flatMap(cardMetadata -> cardMetadata.getIncomes().stream())
                            .filter(income -> income.getType() == GainType.HEAT)
                            .map(Gain::getValue)
                            .mapToInt(num -> num).sum();
                    currentRating = (double) heatIncomeSum / 15;
                    break;
                case FARMER:
                    int plantsIncomeSum = player.getHand().getCards().stream().map(cardService::getCard).map(Card::getCardMetadata).filter(Objects::nonNull)
                            .filter(cardMetadata -> !CollectionUtils.isEmpty(cardMetadata.getIncomes()))
                            .flatMap(cardMetadata -> cardMetadata.getIncomes().stream())
                            .filter(income -> income.getType() == GainType.PLANT)
                            .map(Gain::getValue)
                            .mapToInt(num -> num).sum();
                    currentRating = (double) plantsIncomeSum / 5;
                    break;
                case LEGEND:
                    long redCardsCount = player.getHand().getCards().stream().map(cardService::getCard).filter(card -> card.getColor() == CardColor.RED).count();
                    currentRating = (double) redCardsCount / 6;
                    break;
                case MAGNATE:
                    long greenCardsCount = player.getHand().getCards().stream().map(cardService::getCard).filter(card -> card.getColor() == CardColor.GREEN).count();
                    currentRating = (double) greenCardsCount / 8;
                    break;
                case TYCOON:
                    long blueCardsCount = player.getHand().getCards().stream().map(cardService::getCard).filter(card -> card.getColor() == CardColor.BLUE).count();
                    currentRating = (double) blueCardsCount / 6;
                    break;
                case PLANNER:
                    long cheapCardsCount = player.getHand().getCards().stream().map(cardService::getCard).filter(card -> card.getPrice() < 10).count();
                    currentRating = (double) cheapCardsCount / 12;
                    break;
            }
            if (currentRating > milestoneRating) {
                milestoneRating = currentRating;
                bestMilestoneIndex = i;
            }
        }

        return bestMilestoneIndex;
    }

    public int chooseDynamicTagValue(Player player, List<Tag> excludedTags) {
        // 1. Собираем все допустимые теги в список
        List<Tag> candidates = new ArrayList<>();
        for (Tag t : Tag.values()) {
            if (t != Tag.DYNAMIC && !excludedTags.contains(t)) {
                candidates.add(t);
            }
        }

        if (candidates.isEmpty()) {
            throw new IllegalStateException("AI was not able to choose dynamic tag");
        }

        // 2. Обработка логики SMART/NETWORK
        AiTurnChoice phase = player.getDifficulty().PHASE_TAG_UPGRADE;
        if (phase == SMART || phase == NETWORK) {
            if (candidates.contains(Tag.SCIENCE)) return Tag.SCIENCE.ordinal();
            if (candidates.contains(Tag.JUPITER)) return Tag.JUPITER.ordinal();
        }

        // 3. Честный рандом для RANDOM (и как fallback для SMART)
        return candidates.get(random.nextInt(candidates.size())).ordinal();
    }


    public Map<Integer, List<Integer>> getCorporationInput(MarsGame game, Player player, CardAction corporationCardAction) {
        if (corporationCardAction == CardAction.HYPERION_SYSTEMS_CORPORATION) {
            return Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(choosePhaseUpgrade(game, player, PERFORM_BLUE_ACTION_PHASE)));
        } else if (corporationCardAction == CardAction.APOLLO_CORPORATION) {
            return Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(choosePhaseUpgrade(game, player, BUILD_BLUE_RED_PROJECTS_PHASE)));
        } else if (corporationCardAction == CardAction.EXOCORP_CORPORATION) {
            return Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(choosePhaseUpgrade(game, player, DRAFT_CARDS_PHASE)));
        } else if (corporationCardAction == CardAction.AUSTELLAR_CORPORATION) {
            return Map.of(InputFlag.AUSTELLAR_CORPORATION_MILESTONE.getId(), List.of(chooseAustellarCorporationMilestone(game, player)), InputFlag.TAG_INPUT.getId(), List.of(Tag.SCIENCE.ordinal()));
        } else if (corporationCardAction == CardAction.MODPRO_CORPORATION) {
            return Map.of(InputFlag.TAG_INPUT.getId(), List.of(chooseDynamicTagValue(player, List.of())));
        } else if (corporationCardAction == CardAction.NEBU_LABS_CORPORATION) {
            return Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(choosePhaseUpgrade(game, player)));
        } else if (corporationCardAction == CardAction.SULTIRA_CORPORATION) {
            return Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(choosePhaseUpgrade(game, player, BUILD_GREEN_PROJECTS_PHASE)));
        }
        return Map.of();
    }

}
