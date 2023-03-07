package com.terraforming.ares.model;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
public class Constants {
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


    public static final int ADAPTATION_TECHNOLOGY_CARD_ID = 1;
    public static final int ADVANCED_ALLOYS_CARD_ID = 2;
    public static final int ADVANCED_SCREENING_TECHNOLOGY_CARD_ID = 3;
    public static final int AI_CENTRAL_CARD_ID = 4;
    public static final int ANAEROBIC_MICROORGANISMS_CARD_ID = 5;

    public static final int ARTIFICIAL_JUNGLE_CARD_ID = 9;
    public static final int ASSEMBLY_LINES_CARD_ID = 10;
    public static final int ASSET_LIQUIDATION_CARD_ID = 11;


    public static final int BUILD_GREEN_PROJECTS_PHASE = 1;
    public static final int BUILD_BLUE_RED_PROJECTS_PHASE = 2;
    public static final int PERFORM_BLUE_ACTION_PHASE = 3;
    public static final int COLLECT_INCOME_PHASE = 4;
    public static final int DRAFT_CARDS_PHASE = 5;
    public static final int SELL_EXTRA_CARDS_PHASE = 6;
    public static final int PICK_CORPORATIONS_PHASE = 7;

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

    public static final boolean COLLECT_DATASET = false;

    public static final boolean LOG_NET_COMPARISON = false;

    public static final boolean WRITE_STATISTICS_TO_FILE = false;
    public static final boolean WRITE_STATISTICS_TO_CONSOLE = false;
    public static final boolean SAVE_SIMULATION_GAMES_TO_DB = false;


    private Constants() {}

}
