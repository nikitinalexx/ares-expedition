package com.terraforming.ares.model;

import com.terraforming.ares.model.ai.AiCardsChoice;
import com.terraforming.ares.model.ai.AiExperimentalTurn;
import com.terraforming.ares.model.ai.AiTurnChoice;

public enum PlayerDifficulty {
    NONE(),
    RANDOM(
            AiCardsChoice.RANDOM,
            AiTurnChoice.RANDOM,
            AiTurnChoice.RANDOM,
            AiTurnChoice.RANDOM,
            AiTurnChoice.RANDOM,
            AiExperimentalTurn.REGULAR
    ),
    SMART(
            AiCardsChoice.FILE_VALUE,
            AiTurnChoice.SMART,
            AiTurnChoice.SMART,
            AiTurnChoice.SMART,
            AiTurnChoice.SMART,
            AiExperimentalTurn.REGULAR
    ),
    NETWORK(
            AiCardsChoice.NETWORK_PROJECTION,
            AiTurnChoice.NETWORK,
            AiTurnChoice.NETWORK,
            AiTurnChoice.NETWORK,
            AiTurnChoice.NETWORK,
            AiExperimentalTurn.REGULAR
    ),
    NETWORK_EXPERIMENT(
            AiCardsChoice.NETWORK_PROJECTION,
            AiTurnChoice.NETWORK,
            AiTurnChoice.NETWORK,
            AiTurnChoice.NETWORK,
            AiTurnChoice.NETWORK,
            AiExperimentalTurn.EXPERIMENT
    );

    public final AiCardsChoice CARDS_PICK;
    public final AiTurnChoice PICK_PHASE;
    public final AiTurnChoice BUILD;
    public final AiTurnChoice THIRD_PHASE_ACTION;
    public final AiTurnChoice PHASE_TAG_UPGRADE;
    public final AiExperimentalTurn EXPERIMENTAL_TURN;

    PlayerDifficulty(AiCardsChoice CARDS_PICK, AiTurnChoice PICK_PHASE, AiTurnChoice BUILD, AiTurnChoice THIRD_PHASE_ACTION, AiTurnChoice PHASE_TAG_UPGRADE, AiExperimentalTurn EXPERIMENTAL_TURN) {
        this.CARDS_PICK = CARDS_PICK;
        this.PICK_PHASE = PICK_PHASE;
        this.BUILD = BUILD;
        this.THIRD_PHASE_ACTION = THIRD_PHASE_ACTION;
        this.PHASE_TAG_UPGRADE = PHASE_TAG_UPGRADE;
        this.EXPERIMENTAL_TURN = EXPERIMENTAL_TURN;
    }

    PlayerDifficulty() {
        this.CARDS_PICK = AiCardsChoice.RANDOM;
        this.PICK_PHASE = AiTurnChoice.RANDOM;
        this.BUILD = AiTurnChoice.RANDOM;
        this.THIRD_PHASE_ACTION = AiTurnChoice.RANDOM;
        this.PHASE_TAG_UPGRADE = AiTurnChoice.RANDOM;
        this.EXPERIMENTAL_TURN = AiExperimentalTurn.REGULAR;
    }

}
