package com.terraforming.ares.services.agent;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;

/**
 * Per-thread interception of the agent's decisions, used by measurement / search experiments.
 * <p>
 * Installed only on the thread that plays (or rolls out) a game: {@code AgentDecisionService.choose} asks the hook
 * before the policy and after the index is known. A hook therefore sees every decision point of that thread with the
 * game state exactly as it is <b>before</b> the action is applied, may force a candidate, and replaces
 * {@link DecisionLogs} (nothing is recorded while a hook is installed).
 * <p>
 * A hook must not leak between games: always pair {@link #set} with {@link #clear} in {@code finally}.
 */
public final class DecisionProbe {
    public interface Hook {
        /**
         * Called right before the policy chooses.
         *
         * @return the index of a candidate to force, or -1 to let the policy / the random agent decide
         */
        int beforeChoice(Decision decision, MarsGame game, Player player);

        /**
         * Called with the final index, while the game still has the state of {@link #beforeChoice}
         * (the action is applied after this call returns).
         */
        void afterChoice(Decision decision, MarsGame game, Player player, int index);

        /**
         * Called when the engine starts one player's action attempt (one call of
         * {@code AiService.processAiTurn}). A decision snapshot is only replayable if it starts from this state:
         * everything before it in the same attempt is re-decided otherwise.
         */
        default void turnStarted(MarsGame game, Player player) {
        }
    }

    private static final ThreadLocal<Hook> CURRENT = new ThreadLocal<>();

    private DecisionProbe() {
    }

    public static Hook current() {
        return CURRENT.get();
    }

    /** @return the hook that was installed before, so a nested instrumentation can restore it */
    public static Hook set(Hook hook) {
        Hook previous = CURRENT.get();
        if (hook == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(hook);
        }
        return previous;
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** Never instantiates a hook: the engine calls this on the hot path even when no probe is installed. */
    public static void notifyTurnStarted(MarsGame game, Player player) {
        Hook hook = CURRENT.get();
        if (hook != null) {
            hook.turnStarted(game, player);
        }
    }
}
