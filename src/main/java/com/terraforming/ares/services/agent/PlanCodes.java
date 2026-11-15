package com.terraforming.ares.services.agent;

import com.terraforming.ares.model.Player;

/**
 * The latent plan of a self-play game: four bits on the player that the policy can condition on and that, during data
 * generation, select a coherent exploration profile. This is how styles are *discovered* instead of being named:
 * <ul>
 *   <li>at generation time a random code is assigned per seat ({@code -Dares.agent.planRandom=true}), and its bits
 *       switch the generator's nudges ({@code -Dares.agent.planNudges=true}): prefer income cards, prefer non-green
 *       builds, press every blue action, prefer the build phases;</li>
 *   <li>the code is part of the observation (private), so the policy can learn a different behaviour per code and the game
 *       outcome says which codes were good in which position;</li>
 *   <li>at play time the code is chosen by the value (or fixed - it is a knob), and the player then plays the style the
 *       code stands for.</li>
 * </ul>
 * The bit meanings are the generator's instruments, not rules of the player: nothing here decides a move, it only decides
 * which exploration profile the game is generated with.
 * <p>
 * Assignment and nudges are deliberately separate switches. A trained model is measured with codes assigned but nudges
 * OFF - otherwise the measurement would show the generator's behaviour instead of the policy's, and there would be no way
 * to tell whether the model learned the styles at all.
 */
public final class PlanCodes {
    public static final int BITS = 4;
    public static final int CODES = 1 << BITS;
    /** Bit of "prefer the build phases" (1 green, 2 blue/red) instead of income/draft. */
    public static final int BUILD_PHASES = 8;
    /** Marker for "no fixed code requested" ({@code -Dares.agent.planCode} absent). */
    public static final int NO_FIXED_CODE = -1;

    private PlanCodes() {
    }

    /** Assign a random code to every computer seat when a game is created. */
    public static boolean randomGeneration() {
        return Boolean.parseBoolean(System.getProperty("ares.agent.planRandom", "false"));
    }

    /** Every computer seat plays this code, or {@link #NO_FIXED_CODE} when none was requested. */
    public static int fixed() {
        int code = Integer.getInteger("ares.agent.planCode", NO_FIXED_CODE);
        return code >= 0 && code < CODES ? code : NO_FIXED_CODE;
    }

    /** A uniform random code: the generator's latent variable for one seat of one game. */
    public static int random() {
        return java.util.concurrent.ThreadLocalRandom.current().nextInt(CODES);
    }

    /** Let the code switch the generator's nudges. Only meaningful while generating training data. */
    public static boolean nudges() {
        return Boolean.parseBoolean(System.getProperty("ares.agent.planNudges", "false"));
    }

    /**
     * How often a nudge is allowed to override the policy (1.0 = always). A nudge that fires every time turns the code
     * into a compulsion - the generated games then measure the compulsion (an unconditional "call phase 1" starves the
     * economy), and imitating them damages the base style. A partial rate makes each code a *preference*, which is what a
     * style is, and keeps the games plausible enough that their outcomes mean something.
     */
    public static double nudgeRate() {
        double rate = Double.parseDouble(System.getProperty("ares.agent.planNudgeRate", "1"));
        return rate < 0 ? 0 : Math.min(rate, 1);
    }

    /** True when this nudge is allowed to fire now. */
    public static boolean rollNudge() {
        double rate = nudgeRate();
        return rate >= 1 || (rate > 0 && java.util.concurrent.ThreadLocalRandom.current().nextDouble() < rate);
    }

    /** Bits: 1 = prefer income cards, 2 = prefer non-green builds, 4 = press blue actions, 8 = prefer build phases. */
    public static boolean preferIncome(Player player) {
        return bit(player, 1);
    }

    public static boolean preferNonGreen(Player player) {
        return bit(player, 2);
    }

    public static boolean pressBlueActions(Player player) {
        return bit(player, 4);
    }

    public static boolean preferBuildPhases(Player player) {
        return bit(player, BUILD_PHASES);
    }

    public static int code(Player player) {
        int code = player.getPlanCode();
        return code < 0 || code >= CODES ? 0 : code;
    }

    private static boolean bit(Player player, int mask) {
        return nudges() && (code(player) & mask) != 0;
    }
}
