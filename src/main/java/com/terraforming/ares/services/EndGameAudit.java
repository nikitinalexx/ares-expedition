package com.terraforming.ares.services;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Logs the case the owner asked about (22/11): a player raises the LAST global parameter, the game really ends, and that
 * player LOSES. That is the expensive mistake - the game was ended while behind instead of playing on - and it is invisible
 * in the win rate of a bundle.
 * <p>
 * Why a static funnel: the parameter is raised in several places (standard projects, card effects, blue actions), and every
 * one of them goes through {@link TerraformingService}. The last raiser is remembered per game and checked when the state
 * machine reaches GAME_END. Enabled by {@code -Dares.endaudit=true} (off by default: one map write per parameter raise when
 * on, nothing when off).
 */
public final class EndGameAudit {

    private static final Map<MarsGame, String> LAST_RAISER = Collections.synchronizedMap(new IdentityHashMap<>());
    private static volatile boolean enabled;

    private EndGameAudit() {
    }

    public static boolean enabled() {
        if (!enabled) {
            enabled = Boolean.parseBoolean(System.getProperty("ares.endaudit", "false"));
        }
        return enabled;
    }

    /** Records the player who raised a parameter; the final raise before GAME_END is the one that ended the game. */
    public static void record(MarsGame game, Player player) {
        if (!enabled() || player == null) {
            return;
        }
        LAST_RAISER.put(game, player.getUuid());
    }

    /** Called when the game reaches GAME_END: if the last raiser lost, say so with both scores. */
    public static void check(MarsGame game, WinPointsService winPointsService) {
        if (!enabled()) {
            return;
        }
        String raiser = LAST_RAISER.remove(game);
        if (raiser == null) {
            return;
        }
        int mine = -1, theirs = -1;
        String other = null;
        for (Player player : game.getPlayerUuidToPlayer().values()) {
            int points = winPointsService.countWinPoints(player, game);
            if (player.getUuid().equals(raiser)) {
                mine = points;
            } else {
                theirs = points;
                other = player.getName();
            }
        }
        if (mine < 0 || theirs < 0) {
            return;
        }
        if (mine < theirs) {
            Player ender = game.getPlayerUuidToPlayer().values().stream()
                    .filter(p -> p.getUuid().equals(raiser)).findFirst().orElse(null);
            System.out.printf("[END-AUDIT] %s ended the game at round %d with %d against %d (%s) - ended while behind%n",
                    ender == null ? raiser : ender.getDifficulty() + "/" + ender.getName(),
                    game.getTurns(), mine, theirs, other);
        }
    }
}
