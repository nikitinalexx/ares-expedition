package com.terraforming.ares.services.ai;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AiDiscoveryDecisionService {
    private final Random random = new Random();

    public int choosePhaseUpgrade(MarsGame game, Player player, int phase) {
        int phaseOffset = (phase - 1) * 2;
        boolean chooseSecondUpgrade = false;
        switch (player.getDifficulty().PHASE_TAG_UPGRADE) {
            case RANDOM:
            case SMART:
                chooseSecondUpgrade = random.nextBoolean();
                break;
            case NETWORK:
                //TODO expansion
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
            default:
                throw new IllegalStateException("Unable to choose phase upgrade for AI");
        }
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
            case NETWORK://TODO expansion
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

}
