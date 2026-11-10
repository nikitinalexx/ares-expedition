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
    ),
    /**
     * Learnable agent. Decisions it already handles (see DecisionType) go to the agent, everything else is played like SMART.
     */
    AGENT(
            AiCardsChoice.FILE_VALUE,
            AiTurnChoice.SMART,
            AiTurnChoice.SMART,
            AiTurnChoice.SMART,
            AiTurnChoice.SMART,
            AiExperimentalTurn.REGULAR
    ),
    /**
     * Same learnable agent, but everything it does not decide yet is played RANDOMLY instead of by SMART. Mixing it with AGENT in the
     * data keeps the learned decisions from adapting to the hand written SMART heuristics only.
     */
    AGENT_R(
            AiCardsChoice.RANDOM,
            AiTurnChoice.RANDOM,
            AiTurnChoice.RANDOM,
            AiTurnChoice.RANDOM,
            AiTurnChoice.RANDOM,
            AiExperimentalTurn.REGULAR
    ),
    NETWORK_V2(
            AiCardsChoice.RANDOM,
            AiTurnChoice.RANDOM,
            AiTurnChoice.RANDOM,
            AiTurnChoice.RANDOM,
            AiTurnChoice.RANDOM,
            AiExperimentalTurn.EXPERIMENT
            )
    ;

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

    public boolean isAgent() {
        return this == AGENT || this == AGENT_R;
    }

    /**
     * Who plays the decisions the agent does not take yet: 0 = SMART heuristics, 1 = RANDOM.
     */
    public int agentContext() {
        return this == AGENT_R ? 1 : 0;
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
