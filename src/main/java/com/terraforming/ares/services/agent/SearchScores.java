package com.terraforming.ares.services.agent;

/**
 * Handoff of the search's per-candidate scores from {@link OnePlySearch} to {@link DecisionLogs}.
 * <p>
 * A decision is built by the decision service, the search scores its candidates, and the logs record the row - three
 * different objects in one call chain. Threading a fifth field through {@link Decision} would touch every decision type;
 * a thread-local pair (decision identity, scores) keeps the search's opinion next to the decision it belongs to and is
 * consumed exactly once, by the recorder of that same thread.
 */
public final class SearchScores {
    private static final ThreadLocal<Decision> OWNER = new ThreadLocal<>();
    private static final ThreadLocal<float[]> SCORES = new ThreadLocal<>();

    private SearchScores() {
    }

    public static void put(Decision decision, float[] scores) {
        OWNER.set(decision);
        SCORES.set(scores);
    }

    /**
     * The scores for exactly this decision without consuming them, or null. Used by the generator's guided exploration: it
     * samples among the candidates the search considers nearly as good, instead of a uniformly random action.
     */
    public static float[] peek(Decision decision) {
        return OWNER.get() == decision ? SCORES.get() : null;
    }

    /** The scores for exactly this decision, or null. Clears the slot: one decision, one opinion. */
    public static float[] take(Decision decision) {
        if (OWNER.get() != decision) {
            return null;
        }
        float[] scores = SCORES.get();
        OWNER.remove();
        SCORES.remove();
        return scores;
    }
}
