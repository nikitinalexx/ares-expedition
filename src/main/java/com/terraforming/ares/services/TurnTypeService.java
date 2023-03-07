package com.terraforming.ares.services;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.turn.TurnType;
import org.springframework.stereotype.Service;

/**
 * Created by oleksii.nikitin
 * Creation date 01.06.2022
 */
@Service
public class TurnTypeService {

    public boolean isTerminal(TurnType turnType, MarsGame game) {
        switch (turnType) {
            case PICK_CORPORATION:
            case PICK_PHASE:
            case BUILD_GREEN_PROJECT:
            case SELL_CARDS_LAST_ROUND:
            case SKIP_TURN:
            case BUILD_BLUE_RED_PROJECT:
            case COLLECT_INCOME:
            case DRAFT_CARDS:
            case PICK_EXTRA_BONUS_SECOND_PHASE:
            case GAME_END_CONFIRM:
            case GAME_END:
            case RESOLVE_PERSISTENT_ALL:
            case RESOLVE_IMMEDIATE_ALL:
            case DRAW_CRYSIS_CARD:
            case RESOLVE_IMMEDIATE_WITH_CHOICE:
            case RESOLVE_PERSISTENT_WITH_CHOICE:
            case CRISIS_CHOOSE_DUMMY_HAND:
            case RESOLVE_OCEAN_DETRIMENT:
                return true;
            case SELL_CARDS:
            case SELL_VP:
            case MULLIGAN:
            case PERFORM_BLUE_ACTION:
            case PLANT_FOREST:
            case INCREASE_TEMPERATURE:
            case STANDARD_PROJECT:
            case EXCHANGE_HEAT:
            case UNMI_RT:
            case PLANTS_TO_CRISIS_TOKEN:
                return false;
            case DISCARD_CARDS:
                return game.getCurrentPhase() == Constants.PICK_CORPORATIONS_PHASE ||
                        game.getCurrentPhase() == Constants.DRAFT_CARDS_PHASE;
            default:
                throw new IllegalStateException("Unknown turn type " + turnType);
        }
    }

    public boolean isIntermediate(TurnType turnType) {
        return TurnType.DISCARD_CARDS == turnType
                || TurnType.RESOLVE_PERSISTENT_WITH_CHOICE == turnType
                || TurnType.RESOLVE_IMMEDIATE_WITH_CHOICE == turnType
                || TurnType.RESOLVE_IMMEDIATE_ALL == turnType;
    }
}
