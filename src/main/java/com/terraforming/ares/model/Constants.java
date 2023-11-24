package com.terraforming.ares.model;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
public class Constants {
    public static Map<Integer, Integer> FIRST_PLAYER_PHASES = new ConcurrentHashMap<>();
    public static Map<Integer, Integer> SECOND_PLAYER_PHASES = new ConcurrentHashMap<>();

    //TODO update
    public static final List<Integer> CORPORATION_PRIORITY = List.of(
            10006, /* Tharsis */
            10015, /* MayNi */
            10104, 10011, /* Saturn */
            10105, 10012, /* Zetacell */
            10102, 10009, /* Phobolog */
            10017, /* Interplanetary */
            10003, /* LaunchStar */
            10002, /* DevTechs */
            10007, /* Credicor */
            10005, /* Teractor */
            10107, 10014, /* Inventrix */
            10004, /* Thorgate */
            10103, 10010, /* Mining */
            10106, 10013, /* Ecoline */
            10001, /* Celestior */
            10101, 10008, /* Arclight */
            10108, 10016, /* UNMI */
            10100, 10000 /* Helion */
    );

    public static final int CORPORATION_ID_OFFSET = 10000;


    public static final int MAX_OCEANS = 9;
    public static final int STARTING_RT = 5;
    public static final int TEMPERATURE_HEAT_COST = 8;
    public static final int CRISIS_TOKEN_PLANTS_COST = 4;
    public static final int CRISIS_TOKEN_HEAT_COST = 6;
    public static final int CRISIS_EASY_MODE_TURNS = 3;
    public static final int FOREST_PLANT_COST = 8;

    public static final int CRISIS_NORMAL_DIFFICULTY = 0;
    public static final int CRISIS_BEGINNER_DIFFICULTY = -1;

    public static final int FOREST_MC_COST = 20;
    public static final int TEMPERATURE_MC_COST = 14;
    public static final int OCEAN_MC_COST = 15;

    public static final int DEFAULT_START_HAND_SIZE = 8;
    public static final int MAX_HAND_SIZE_LAST_ROUND = 10;

    public static final int BUILD_GREEN_PROJECTS_PHASE = 1;
    public static final int BUILD_BLUE_RED_PROJECTS_PHASE = 2;
    public static final int PERFORM_BLUE_ACTION_PHASE = 3;
    public static final int COLLECT_INCOME_PHASE = 4;
    public static final int DRAFT_CARDS_PHASE = 5;
    public static final int SELL_EXTRA_CARDS_PHASE = 6;
    public static final int PICK_CORPORATIONS_PHASE = 7;

    public static final int UPGRADEABLE_PHASES_COUNT = 5;

    public static final int MAX_PLAYERS = 4;

    public static final int REMOVE_GAMES_FROM_LOBBY_AFTER_SECONDS = 300;
    public static final int REMOVE_PLAYERS_FROM_LOBBY_AFTER_SECONDS = 10;

    public static final int ACHIEVEMENTS_SIZE = 3;

    public static final int PHASE_1_NO_UPGRADE = 0;
    public static final int PHASE_1_UPGRADE_DISCOUNT = 1;
    public static final int PHASE_1_UPGRADE_BUILD_EXTRA = 2;

    public static final int PHASE_2_NO_UPGRADE = 3;
    public static final int PHASE_2_UPGRADE_PROJECT_AND_MC = 4;
    public static final int PHASE_2_UPGRADE_PROJECT_AND_CARD = 5;

    public static final int PHASE_3_NO_UPGRADE = 6;
    public static final int PHASE_3_UPGRADE_DOUBLE_REPEAT = 7;
    public static final int PHASE_3_UPGRADE_REVEAL_CARDS = 8;

    public static final int PHASE_4_NO_UPGRADE = 9;
    public static final int PHASE_4_UPGRADE_EXTRA_MC = 10;
    public static final int PHASE_4_UPGRADE_DOUBLE_PRODUCE = 11;

    public static final int PHASE_5_NO_UPGRADE = 12;
    public static final int PHASE_5_UPGRADE_KEEP_EXTRA = 13;
    public static final int PHASE_5_UPGRADE_SEE_EXTRA = 14;

    public static final int TOTAL_CARDS_COUNT = 268;

    public static final float RED_CARDS_RATIO = 49f / TOTAL_CARDS_COUNT;
    public static final float GREEN_CARDS_RATIO = 135f / TOTAL_CARDS_COUNT;
    public static final float BLUE_CARDS_RATIO = 84f / TOTAL_CARDS_COUNT;

    public static final Map<Tag, Integer> TAG_TO_CARDS_COUNT = Map.of(
            Tag.PLANT, 35,
            Tag.SCIENCE, 41,
            Tag.ENERGY, 33,
            Tag.SPACE, 51,
            Tag.EVENT, 49,
            Tag.BUILDING, 85,
            Tag.ANIMAL, 10,
            Tag.MICROBE, 23,
            Tag.EARTH, 28,
            Tag.JUPITER, 14
    );

    public static final int CARDS_WITH_DYNAMIC_TAG = 4;

    public static final boolean DISCOVERY_SIMULATIONS = true;

    public static final boolean PLAYER_CHOOSES_CARDS_FOR_FIRST_PLAYER = false;

    public static final boolean WRITE_STATISTICS_TO_FILE = false;
    public static final boolean WRITE_STATISTICS_TO_CONSOLE = false;
    public static final boolean SAVE_SIMULATION_GAMES_TO_DB = false;


    public static final List<PlayerDifficulty> SIMULATION_PLAYERS = List.of(PlayerDifficulty.NETWORK, PlayerDifficulty.NETWORK);

    public static final boolean AI_CORP_RANDOM = true;
    public static final boolean COLLECT_DATASET = true;
    public static final boolean LOG_NET_COMPARISON = false;

    private Constants() {
    }

}
