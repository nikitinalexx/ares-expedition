package com.terraforming.ares.services.ai;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.ai.dto.PhaseUpgradeWithChance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiDiscoveryDecisionService {
    private final Random random = new Random();
    private final DeepNetwork deepNetwork;

    public int choosePhaseUpgrade(MarsGame game, Player player, int phase) {
        int phaseOffset = (phase - 1) * 2;
        boolean chooseSecondUpgrade = false;
        switch (player.getDifficulty().PHASE_TAG_UPGRADE) {
            case RANDOM:
            case SMART:
                chooseSecondUpgrade = random.nextBoolean();
                break;
            case NETWORK:
                List<Integer> phaseUpgrades = player.getPhaseCards();

                int phaseUpgrade = phaseUpgrades.get(phase - 1);

                if (phaseUpgrade == 0) {
                    phaseUpgrades.set(phase - 1, 1);
                    float firstUpgradeState = deepNetwork.testState(game, player);

                    phaseUpgrades.set(phase - 1, 2);
                    float secondUpgradeState = deepNetwork.testState(game, player);

                    phaseUpgrades.set(phase - 1, 0);

                    if (secondUpgradeState > firstUpgradeState) {
                        chooseSecondUpgrade = true;
                    }
                } else {
                    float initialState = deepNetwork.testState(game, player);

                    int anotherUpdate = phaseUpgrade == 1 ? 2 : 1;

                    phaseUpgrades.set(phase - 1, anotherUpdate);
                    float anotherUpgradeState = deepNetwork.testState(game, player);

                    if (anotherUpgradeState > initialState) {
                        chooseSecondUpgrade = (anotherUpdate == 2);
                    }
                }
                phaseUpgrades.set(phase - 1, phaseUpgrade);
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
                List<PhaseUpgradeWithChance> bestUpgrades = findBestUpgrades(chooseBestUpdateForEachPhase(game, player), 2);

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

    public List<PhaseUpgradeWithChance> chooseBestUpdateForEachPhase(MarsGame game, Player player) {
        List<Integer> initialUpgrades = new ArrayList<>(player.getPhaseCards());

        List<PhaseUpgradeWithChance> result = new ArrayList<>();

        float initialState = deepNetwork.testState(game, player);
        for (int i = 0; i < Constants.UPGRADEABLE_PHASES_COUNT; i++) {
            int currentUpgrade = initialUpgrades.get(i);

            float firstUpdateChance = initialState;
            if (currentUpgrade != 1) {
                player.getPhaseCards().set(i, 1);
                firstUpdateChance = deepNetwork.testState(game, player);
                player.getPhaseCards().set(i, initialUpgrades.get(i));
            }

            result.add(PhaseUpgradeWithChance.of(firstUpdateChance, i, 1));

            float secondUpdateChance = initialState;
            if (currentUpgrade != 2) {
                player.getPhaseCards().set(i, 2);
                secondUpdateChance = deepNetwork.testState(game, player);
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
                List<Integer> phases = new ArrayList<>();
                List<Integer> phaseCards = player.getPhaseCards();
                for (int i = 1; i <= phaseCards.size(); i++) {
                    if (phaseCards.get(i - 1) == 0) {
                        phases.add(i);
                    }
                }
                if (phases.isEmpty()) {
                    phases = List.of(1, 2, 3, 4, 5);
                }
                return choosePhaseUpgrade(game, player, phases.get(random.nextInt(phases.size())));
            case NETWORK:
                List<PhaseUpgradeWithChance> bestUpgrades = findBestUpgrades(chooseBestUpdateForEachPhase(game, player), 1);

                return bestUpgrades.get(0).getUpgradeAsNumber();
            default:
                throw new IllegalStateException("Unable to choose phase upgrade for AI");
        }
    }

    public int chooseAustellarCorporationMilestone(MarsGame game) {
        return random.nextInt(game.getMilestones().size());
    }

    public int chooseDynamicTagValue(Player player, List<Tag> excludedTags) {
        switch (player.getDifficulty().PHASE_TAG_UPGRADE) {
            case SMART:
            case NETWORK://TODO when finish AI third phase
                if (!excludedTags.contains(Tag.SCIENCE)) {
                    return Tag.SCIENCE.ordinal();
                } else if (!excludedTags.contains(Tag.JUPITER)) {
                    return Tag.JUPITER.ordinal();
                }
                //TODO add logic, if has jupiter VP, add Jupiter. If has dynamic income, play that card.
                //else make random choice
            case RANDOM:
                Tag[] allTags = Tag.values();
                int randomTag = random.nextInt(allTags.length);

                for (int i = 0; i < allTags.length; i++) {
                    Tag tagToCheck = allTags[(randomTag + i) % allTags.length];
                    if (!excludedTags.contains(tagToCheck) && (tagToCheck != Tag.DYNAMIC)) {
                        return (randomTag + i) % allTags.length;
                    }
                }
                break;
        }

        throw new IllegalStateException("AI was not able to choose dynamic tag");
    }

    public int chooseModProTagValue() {
        if (random.nextBoolean()) {
            return Tag.SCIENCE.ordinal();
        } else {
            return Tag.JUPITER.ordinal();
        }
    }
    
    public Map<Integer, List<Integer>> getCorporationInput(MarsGame game, Player player, CardAction corporationCardAction) {
        if (corporationCardAction == CardAction.HYPERION_SYSTEMS_CORPORATION) {
            return Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(choosePhaseUpgrade(game, player, Constants.PERFORM_BLUE_ACTION_PHASE)));
        } else if (corporationCardAction == CardAction.APOLLO_CORPORATION) {
            return Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(choosePhaseUpgrade(game, player, Constants.BUILD_BLUE_RED_PROJECTS_PHASE)));
        } else if (corporationCardAction == CardAction.EXOCORP_CORPORATION) {
            return Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(choosePhaseUpgrade(game, player, Constants.DRAFT_CARDS_PHASE)));
        } else if (corporationCardAction == CardAction.AUSTELLAR_CORPORATION) {
            return Map.of(InputFlag.AUSTELLAR_CORPORATION_MILESTONE.getId(), List.of(chooseAustellarCorporationMilestone(game)), InputFlag.TAG_INPUT.getId(), List.of(chooseDynamicTagValue(player, List.of())));
        } else if (corporationCardAction == CardAction.MODPRO_CORPORATION) {
            return Map.of(InputFlag.TAG_INPUT.getId(), List.of(chooseModProTagValue()));
        } else if (corporationCardAction == CardAction.NEBU_LABS_CORPORATION) {
            return Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(choosePhaseUpgrade(game, player)));
        } else if (corporationCardAction == CardAction.SULTIRA_CORPORATION) {
            return Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(choosePhaseUpgrade(game, player, Constants.BUILD_GREEN_PROJECTS_PHASE)));
        }
        return Map.of();
    }

}
