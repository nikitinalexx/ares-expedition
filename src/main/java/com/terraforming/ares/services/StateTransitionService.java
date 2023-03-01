package com.terraforming.ares.services;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.turn.DraftCardsTurn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 27.02.2023
 */
@Service
@RequiredArgsConstructor
public class StateTransitionService {
    private final CardService cardService;
    private final ShuffleService shuffleService;

    public void performStateTransferFromPhase(MarsGame marsGame, int phaseNumber) {
        if (marsGame.gameEndCondition()) {
            marsGame.setStateType(StateType.GAME_END, cardService);
            return;
        }

        if (phaseNumber == 0 && marsGame.isDummyHandMode()) {
            marsGame.updateDummyHand(shuffleService);
        }

        Set<Integer> pickedPhases = marsGame.getPlayerUuidToPlayer()
                .values()
                .stream()
                .map(Player::getChosenPhase)
                .collect(Collectors.toSet());
        if (marsGame.isDummyHandMode() && phaseNumber >= 0) {
            pickedPhases.add(marsGame.getCurrentDummyHand());
        }

        if (phaseNumber <= 1 && pickedPhases.contains(1)) {
            transferToPhaseOne(marsGame);
        } else if (phaseNumber <= 2 && pickedPhases.contains(2)) {
            transferToPhaseTwo(marsGame);
        } else if (phaseNumber <= 3 && pickedPhases.contains(3)) {
            marsGame.setStateType(StateType.PERFORM_BLUE_ACTION, cardService);
            marsGame.getPlayerUuidToPlayer().values().forEach(
                    player -> {
                        if (player.getChosenPhase() == 3 && player.hasPhaseUpgrade(Constants.PHASE_3_UPGRADE_REVEAL_CARDS)) {
                            final List<Integer> cards = cardService.dealCards(marsGame, 3);
                            for (Integer cardId : cards) {
                                final Card card = cardService.getCard(cardId);
                                if (card.getColor() == CardColor.BLUE || card.getColor() == CardColor.RED) {
                                    player.getHand().addCard(cardId);
                                }
                            }
                        }
                    });
        } else if (phaseNumber <= 4 && pickedPhases.contains(4)) {
            marsGame.setStateType(StateType.COLLECT_INCOME, cardService);
        } else if (phaseNumber <= 5 && pickedPhases.contains(5)) {
            marsGame.setStateType(StateType.DRAFT_CARDS, cardService);
            marsGame.getPlayerUuidToPlayer().values().forEach(player -> player.addNextTurn(new DraftCardsTurn(player.getUuid())));
        } else if (marsGame.getPlayerUuidToPlayer().values().stream().anyMatch(player -> player.getHand().size() > Constants.MAX_HAND_SIZE_LAST_ROUND)) {
            marsGame.setStateType(StateType.SELL_EXTRA_CARDS, cardService);
        } else {
            marsGame.setStateType(StateType.PICK_PHASE, cardService);
        }
    }

    private void transferToPhaseOne(MarsGame game) {
        game.setStateType(StateType.BUILD_GREEN_PROJECTS, cardService);

        for (Player player : game.getPlayerUuidToPlayer().values()) {
            List<BuildDto> builds = new ArrayList<>();
            if (player.getChosenPhase() != 1) {
                builds.add(new BuildDto(BuildType.GREEN, 0));
            } else {
                if (player.hasPhaseUpgrade(Constants.PHASE_1_UPGRADE_DISCOUNT)) {
                    builds.add(new BuildDto(BuildType.GREEN, 6));
                } else if (player.hasPhaseUpgrade(Constants.PHASE_1_UPGRADE_BUILD_EXTRA)) {
                    builds.add(new BuildDto(BuildType.GREEN, 3));
                    builds.add(new BuildDto(BuildType.GREEN, 0, 12));
                } else {
                    builds.add(new BuildDto(BuildType.GREEN, 3));
                }
            }
            player.setBuilds(builds);
        }
    }

    private void transferToPhaseTwo(MarsGame marsGame) {
        marsGame.setStateType(StateType.BUILD_BLUE_RED_PROJECTS, cardService);

        for (Player player : marsGame.getPlayerUuidToPlayer().values()) {
            List<BuildDto> builds = new ArrayList<>();
            builds.add(new BuildDto(BuildType.BLUE_RED));

            if (player.getChosenPhase() == 2) {
                if (player.hasPhaseUpgrade(Constants.PHASE_2_UPGRADE_PROJECT_AND_MC)) {
                    builds.add(new BuildDto(BuildType.BLUE_RED_OR_MC));
                } else if (player.hasPhaseUpgrade(Constants.PHASE_2_UPGRADE_PROJECT_AND_CARD)) {
                    player.getHand().addCards(cardService.dealCards(marsGame, 1));
                    builds.add(new BuildDto(BuildType.BLUE_RED));
                } else {
                    builds.add(new BuildDto(BuildType.BLUE_RED_OR_CARD));
                }
            }
            player.setBuilds(builds);
        }
    }

}
