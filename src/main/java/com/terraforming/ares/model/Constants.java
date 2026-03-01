package com.terraforming.ares.model;

import com.terraforming.ares.model.parameters.Ocean;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
public class Constants {
    public static final int ESTIMATED_TURNS_PER_GAME = 24;

    public static Map<Integer, Integer> FIRST_PLAYER_PHASES = new ConcurrentHashMap<>();
    public static Map<Integer, Integer> SECOND_PLAYER_PHASES = new ConcurrentHashMap<>();

    public static final List<PlayerDifficulty> AVAILABLE_DIFFICULTIES = List.of(PlayerDifficulty.NONE, PlayerDifficulty.RANDOM, PlayerDifficulty.SMART, PlayerDifficulty.NETWORK);

    public static final int CORPORATION_ID_OFFSET = 10000;






    public static final int MAX_OCEANS = 9;
    public static final int STARTING_RT = 5;
    public static final int TEMPERATURE_HEAT_COST = 8;
    public static final int CRISIS_TOKEN_PLANTS_COST = 4;
    public static final int CRISIS_TOKEN_HEAT_COST = 6;
    public static final int CRISIS_EASY_MODE_TURNS = 3;
    public static final int FOREST_PLANT_COST = 8;
    public static final int INFRASTRUCTURE_HEAT_COST = 5;
    public static final int INFRASTRUCTURE_PLANT_COST = 3;

    public static final int CRISIS_NORMAL_DIFFICULTY = 0;
    public static final int CRISIS_BEGINNER_DIFFICULTY = -1;

    public static final int FOREST_MC_COST = 20;
    public static final int TEMPERATURE_MC_COST = 14;
    public static final int OCEAN_MC_COST = 15;
    public static final int INFRASTRUCTURE_MC_COST = 15;

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

    public static final int PHASE_1_UPGRADE_DISCOUNT = 0;
    public static final int PHASE_1_UPGRADE_BUILD_EXTRA = 1;

    public static final int PHASE_2_UPGRADE_PROJECT_AND_MC = 2;
    public static final int PHASE_2_UPGRADE_PROJECT_AND_CARD = 3;

    public static final int PHASE_3_UPGRADE_DOUBLE_REPEAT = 4;
    public static final int PHASE_3_UPGRADE_REVEAL_CARDS = 5;

    public static final int PHASE_4_UPGRADE_EXTRA_MC = 6;
    public static final int PHASE_4_UPGRADE_DOUBLE_PRODUCE = 7;

    public static final int PHASE_5_UPGRADE_KEEP_EXTRA = 8;
    public static final int PHASE_5_UPGRADE_SEE_EXTRA = 9;

    public static final int TOTAL_CARDS_COUNT = 268;

    public static final float RED_CARDS_RATIOO = 49f / TOTAL_CARDS_COUNT;
    public static final float GREEN_CARDS_RATIOO = 135f / TOTAL_CARDS_COUNT;
    public static final float BLUE_CARDS_RATIOO = 84f / TOTAL_CARDS_COUNT;

    public static final int POWER_INFRASTRUCTURE_CARD_ID = 47;

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


    public static final boolean PLAYER_CHOOSES_CARDS_FOR_FIRST_PLAYER = false;
    public static final boolean INFRASTRUCTURE_SIMULATIONS = false;

    public static final boolean WRITE_STATISTICS_TO_FILE = false;
    public static final boolean WRITE_CARD_STATISTICS_TO_CONSOLE = false;


    public static final List<PlayerDifficulty> SIMULATION_PLAYERS = List.of(PlayerDifficulty.RANDOM, PlayerDifficulty.RANDOM);

    public static final boolean COLLECT_DATASET = false;
    public static final boolean LOG_NET_COMPARISON = false;
    public static final boolean LOG_NET_COMPARISON_V2 = false;

    private Constants() {
    }

}
