package com.terraforming.ares.services.ai;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class AiDiscoveryDecisionService {
    private Random random = new Random();

    public int choosePhaseUpgrade(MarsGame game, Player player, int phase) {
        int phaseOffset = (phase - 1) * 2;
        boolean chooseSecondUpgrade = false;
        switch (player.isFirstBot() ? Constants.PHASE_UPGRADE_PLAYER_1 : Constants.PHASE_UPGRADE_PLAYER_2) {
            case RANDOM:
                chooseSecondUpgrade = random.nextBoolean();
                break;
            case SMART:
                switch (phase) {
                    case 1:
                        chooseSecondUpgrade = true;
                        break;
                    case 2:
                        chooseSecondUpgrade = true;
                        break;
                    case 3:
                        chooseSecondUpgrade = true;
                        break;
                    case 4:
                    case 5:
                        //TODO expansion
                }
                break;
            case NETWORK:
                //TODO expansion
                break;
            default:
                throw new IllegalStateException("Unable to choose phase upgrade for AI");
        }
        return phaseOffset + (chooseSecondUpgrade ?  1 : 0);
    }

    public List<Integer> chooseTwoPhaseUpgrades(MarsGame game, Player player) {
        switch (player.isFirstBot() ? Constants.PHASE_UPGRADE_PLAYER_1 : Constants.PHASE_UPGRADE_PLAYER_2) {
            case RANDOM:
                int firstUpgrade = random.nextInt(5);
                int secondUpgrade = random.nextInt(5);
                if (firstUpgrade == secondUpgrade) {
                    secondUpgrade = (secondUpgrade + 1) % 5;
                }
                return List.of(choosePhaseUpgrade(game, player, firstUpgrade), choosePhaseUpgrade(game, player, secondUpgrade));
            case SMART:
            case NETWORK:
            default:
                throw new IllegalStateException("Unable to choose phase upgrade for AI");
        }
    }

    public int choosePhaseUpgrade(MarsGame game, Player player) {
        switch (player.isFirstBot() ? Constants.PHASE_UPGRADE_PLAYER_1 : Constants.PHASE_UPGRADE_PLAYER_2) {
            case RANDOM:
                return choosePhaseUpgrade(game, player, random.nextInt(5) + 1);
            case SMART://TODO expansion
            case NETWORK://TODO expansion
            default:
                throw new IllegalStateException("Unable to choose phase upgrade for AI");
        }
    }

    public int chooseAustellarCorporationMilestone(MarsGame game) {
        return random.nextInt(game.getMilestones().size());//TODO expansion
    }

    public int chooseDynamicTagValue(List<Tag> excludedTags) {
        Tag[] allTags = Tag.values();
        int randomTag = random.nextInt(allTags.length);

        for (int i = 0; i < allTags.length; i++) {
            Tag tagToCheck = allTags[(randomTag + i) % allTags.length];
            if (!excludedTags.contains(tagToCheck) && (tagToCheck != Tag.DYNAMIC)) {
                return (randomTag + i) % allTags.length;
            }
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
